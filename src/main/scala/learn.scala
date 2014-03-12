package eg
import scalaz.stream._
import Process._

object learn extends App {
  val convertAllToCelsius =
    io.linesR("testdata/fahrenheit.txt")
      .filter(line => line.trim.nonEmpty && !line.startsWith("//"))
      .map(line => fahrenheitToCelsius(line.toDouble).toString)
      .intersperse("\n")
      .pipe(process1.utf8Encode)
      .to(io.fileChunkW("testdata/celsius.txt"))
      .run

  convertAllToCelsius.run

  def fahrenheitToCelsius(f: Double): Double =
    (f - 32.0) * (5.0/9.0)
}
