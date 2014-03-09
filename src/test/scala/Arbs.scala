package eg
import org.joda.time.DateTime
import org.scalacheck._; import Arbitrary._

trait Arbs {
  implicit val arbDateTime = Arbitrary {
    Gen.choose(0L, DateTime.now.getMillis).map(new DateTime(_))
  }

  implicit val arbTimeCountMap = Arbitrary {
    for {
      d <- arbitrary[DateTime]
      c <- arbitrary[Long]
    } yield TimeCountMap.unit(d,c)
  }

  implicit val arbStats = Arbitrary {
    for {
      d <- arbitrary[DateTime]
      c <- arbitrary[Long]
      tcm <- arbitrary[TimeCountMap]
    } yield TwitterStats(startDate = new DateTime(d),
                         count = c,
                         timeCounts = tcm)
  }

}
