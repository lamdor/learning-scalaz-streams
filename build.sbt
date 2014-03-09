scalaVersion := "2.10.2"

libraryDependencies += "org.scalaz.stream" %% "scalaz-stream" % "0.3"

libraryDependencies ++= Seq(
  "org.twitter4j" % "twitter4j-stream" % "3.0.5",
  "com.github.nscala-time" %% "nscala-time" % "0.8.0"
)

mainClass in Compile := Some("eg.tweets")

javaOptions ++= Seq("-XX:+UseParNewGC", "-XX:+UseConcMarkSweepGC")

Revolver.settings

libraryDependencies ++= Seq( 
  "org.specs2" %% "specs2" % "2.3.8" % "test" exclude("org.scalacheck", "scalacheck_2.10"), // cannot intermix specs2 and scalacheck because of the scalaz-scalacheck-binding
  "org.scalacheck" %% "scalacheck" % "1.10.1" % "test",
  "org.scalaz" %% "scalaz-scalacheck-binding" % "7.0.5" % "test"
)
