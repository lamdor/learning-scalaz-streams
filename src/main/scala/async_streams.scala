package eg
import scalaz.concurrent._
import scalaz.stream.{Process => P, _}

object AsyncStreams extends App {
  def add1(i: Int): Task[Int] = Task { 1 + i }

  val prange = P.range(1,100)
  prange.gatherMap(5)(add1)
    .map(_.toString)
    .to(io.stdOutLines).run.run
}
