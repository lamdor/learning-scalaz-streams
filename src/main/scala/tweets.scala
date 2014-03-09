package eg
import java.util.concurrent.LinkedBlockingQueue
import scalaz._
import scalaz.{Equal, Monoid}
import scalaz.concurrent._
import scalaz.stream._; import Process._
import twitter4j._
import twitter4j.conf.{Configuration => TwitterConfig, ConfigurationBuilder}

object tweets extends App
    with TwitterSource
    with Configuration {

  val collectTweetStats =
    tweetsR(twitterConfig)
      .scan1Map(TwitterStats.makeFromStatus)
      .map(Show[TwitterStats].shows)
      .to(io.stdOutLines)

  collectTweetStats.run.run
}

trait TwitterSource {
  def tweetsR(config: TwitterConfig): Process[Task, Status] =
    io.resource(startListenerIntoState(config))(st => Task.delay(st.stream.shutdown)) {
      st => Task(st.takeAllFromQueue)
    } flatMap (emitAll)

  def multipleTweetsR(size: Int)(config: TwitterConfig): Process[Task,Status] =
    List.fill(size)(tweetsR(config)).reduce(_ merge _)

  private[this] case class TwitterSourceState(
    queue: LinkedBlockingQueue[Status],
    stream: TwitterStream
  ) {
    def takeAllFromQueue = {
      import scala.collection.JavaConverters._
      val list = new java.util.ArrayList[Status]
      queue.drainTo(list)
      list.asScala
    }
  }

  private[this] def startListenerIntoState(config: TwitterConfig): Task[TwitterSourceState] = Task.delay {
    lazy val stream = new TwitterStreamFactory(config).getInstance
    val queue = new LinkedBlockingQueue[Status]()
    stream.addListener(
      new StatusListener {
        def onStatus(status: Status) = queue.put(status)
        def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) {}
        def onTrackLimitationNotice(numberOfLimitedStatuses: Int) {}
        def onException(ex: Exception) { ex.printStackTrace }
        def onScrubGeo(arg0: Long, arg1: Long) {}
        def onStallWarning(warning: StallWarning) {}
      }
    )
    stream.sample
    TwitterSourceState(queue, stream)
  }

}

trait Configuration {
  lazy val twitterConfig =
    new ConfigurationBuilder()
      .setOAuthConsumerKey("WyZZczRFm9ge3bn9JMMzQ")
      .setOAuthConsumerSecret("VRY1PtmGVq0jKOakyhn7xNhbVwXF2lp5rsAh6dIg")
      .setOAuthAccessToken("14086115-CCwDPShEc0lCUAUIDsMohjkgxQtg4efZ7agPF6P8S")
      .setOAuthAccessTokenSecret("gEQfu90RIX8sA9oDPyw3mJ2hfwvI6vURdRuNfvZzUcjlB")
      .build
}

import com.github.nscala_time.time.Imports._

case class TwitterStats(startDate: DateTime = new DateTime(0),
                        count: Long = 0L,
                        timeCounts: TimeCountMap = TimeCountMap.empty) {
  def +(other: TwitterStats) =
    TwitterStats(startDate = math.Ordering[DateTime].min(startDate, other.startDate),
                 count = count + other.count,
                 timeCounts = TimeCountMap.monoid.append(timeCounts, other.timeCounts))
}

object TwitterStats {
  def makeFromStatus(status: Status) = TwitterStats(startDate = DateTime.now,
                                                    count = 1,
                                                    timeCounts = TimeCountMap.unit(new DateTime(status.getCreatedAt)))

  val empty = TwitterStats(startDate = new DateTime(Long.MaxValue), count = 0)
  implicit val statsMonoid: Monoid[TwitterStats] = new Monoid[TwitterStats] {
    val zero = empty
    def append(f1: TwitterStats, f2: => TwitterStats): TwitterStats = f1 + f2
  }
  implicit val statsEqual: Equal[TwitterStats] = Equal.equalA[TwitterStats]
  implicit val statsShow: Show[TwitterStats] = new Show[TwitterStats] {
    import org.joda.time.format.DateTimeFormat
    val dateTimeFormat = DateTimeFormat.forStyle("SM")
    override def show(stats: TwitterStats) =
      "%d tweets since %s, %s per second".format(stats.count,
                                                 dateTimeFormat.print(stats.startDate),
                                                 stats.timeCounts.rate)
  }
}

// could just be a newtype
case class TimeCountMap(buckets: Map[DateTime, Long] = Map.empty) {
  def getCountFor(dt: DateTime) = buckets.getOrElse(TimeCountMap.truncate(dt), 0)
  lazy val rate: Double = buckets.values.sum / buckets.size
}

object TimeCountMap {
  val empty: TimeCountMap = TimeCountMap()

  def unit(dt: DateTime, count: Long = 1L) = TimeCountMap(Map(truncate(dt) -> count))

  private def truncate(dt: DateTime): DateTime = dt.secondOfMinute.roundFloorCopy


  implicit val equals: Equal[TimeCountMap] = Equal.equalA[TimeCountMap]
  implicit val monoid: Monoid[TimeCountMap] = new Monoid[TimeCountMap] {
    val zero = empty
    import scalaz.std._
    def append(tcm1: TimeCountMap, tcm2: => TimeCountMap): TimeCountMap = {
      val mergedBuckets = map.mapMonoid(anyVal.longInstance).append(tcm1.buckets, tcm2.buckets)
      TimeCountMap(keepNewest(mergedBuckets))
    }
  }

  private[this] def keepNewest(map: Map[DateTime, Long], toKeep: Int = 120): Map[DateTime, Long] = {
    if (map.nonEmpty) {
      val toRemove = map.keys.toList.sorted(math.Ordering[DateTime].reverse).drop(toKeep)
      map -- toRemove
    } else map
  }


}

