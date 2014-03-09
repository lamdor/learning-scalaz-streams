package eg
import org.scalacheck._; import Arbitrary._
import scalaz.scalacheck.ScalazProperties._

object TwitterStatsProperties extends Properties("TwitterStats") with Arbs {
  include(monoid.laws[TwitterStats])
}
