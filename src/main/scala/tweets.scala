package eg
import java.util.concurrent.LinkedBlockingQueue
import scalaz._
import scalaz.concurrent._
import scalaz.stream._; import Process._
import twitter4j._
import twitter4j.conf.{Configuration => TwitterConfig, ConfigurationBuilder}

object tweets extends App with TwitterSource {
  val outputTweets =
    tweets(twitterConfig)
      .map(_.getText)
      .to(io.stdOutLines)

  outputTweets.run.run

  lazy val twitterConfig =
    new ConfigurationBuilder()
      .setOAuthConsumerKey("WyZZczRFm9ge3bn9JMMzQ")
      .setOAuthConsumerSecret("VRY1PtmGVq0jKOakyhn7xNhbVwXF2lp5rsAh6dIg")
      .setOAuthAccessToken("14086115-CCwDPShEc0lCUAUIDsMohjkgxQtg4efZ7agPF6P8S")
      .setOAuthAccessTokenSecret("gEQfu90RIX8sA9oDPyw3mJ2hfwvI6vURdRuNfvZzUcjlB")
      .build
}

trait TwitterSource {
  // TODO: this needs to flush all from the queue buffer, not just one by one
  def tweets(config: TwitterConfig): Process[Task, Status] =
    io.resource(startListenerIntoState(config))(st => Task.delay(st.stream.shutdown)) {
      st => Task.delay(st.queue.take)
    }

  private[this] case class TwitterSourceState(
    queue: LinkedBlockingQueue[Status],
    stream: TwitterStream
  )

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
