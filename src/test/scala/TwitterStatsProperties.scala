package eg
import org.joda.time.DateTime
import org.scalacheck._; import Arbitrary._
import scalaz.scalacheck.ScalazProperties._

object TwitterStatsProperties extends Properties("TwitterStats") {
  implicit val arbStats = Arbitrary {
    for {
      d <- Gen.choose(0L, DateTime.now.getMillis).map(new DateTime(_))
      c <- arbitrary[Int]
    } yield TwitterStats(startDate = new DateTime(d),
                         count = c)
  }

  include(monoid.laws[TwitterStats])
}
