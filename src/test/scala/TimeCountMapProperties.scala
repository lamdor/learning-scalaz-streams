package eg
import org.scalacheck._; import Arbitrary._
import scalaz.scalacheck.ScalazProperties._

object TimeCountMapProperties extends Properties("TimeCountMap") with Arbs {
  include(monoid.laws[TimeCountMap])
}
