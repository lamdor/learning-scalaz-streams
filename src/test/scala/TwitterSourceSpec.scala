package eg
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import scalaz.concurrent.Task
import scalaz.stream._
import twitter4j._

object TwitterSourceSpec extends Specification {
  "gets tweets" in new context {
    takeJust5.run must haveSize(5)
  }

  trait context extends Scope with Configuration with TwitterSource {
    lazy val source = tweetsR(twitterConfig)

    val takeJust5: Task[collection.immutable.IndexedSeq[Status]] =
      source.take(5).runLog
  }
}
