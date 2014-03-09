package eg
import java.util.concurrent.LinkedBlockingQueue
import scalaz._
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
                        count: Int = 0) {
  def +(other: TwitterStats) =
    TwitterStats(startDate = math.Ordering[DateTime].min(startDate, other.startDate),
                 count = count + other.count)
}

import scalaz.{Equal, Monoid}
object TwitterStats {
  def makeFromStatus(status: Status) = TwitterStats(startDate = DateTime.now,
                                                    count = 1)

  val empty = TwitterStats(startDate = new DateTime(Long.MaxValue), count = 0)
  implicit val statsMonoid: Monoid[TwitterStats] = new Monoid[TwitterStats] {
    val zero = empty
    def append(f1: TwitterStats, f2: => TwitterStats): TwitterStats = f1 + f2
  }
  implicit val statsEqual: Equal[TwitterStats] = new Equal[TwitterStats] {
    def equal(s1: TwitterStats, s2: TwitterStats): Boolean = s1 == s2
  }
  implicit val statsShow: Show[TwitterStats] = new Show[TwitterStats] {
    import org.joda.time.format.DateTimeFormat
    val dateTimeFormat = DateTimeFormat.forStyle("SM")
    override def show(stats: TwitterStats) =
      "%d tweets since %s".format(stats.count, dateTimeFormat.print(stats.startDate))
  }
}
