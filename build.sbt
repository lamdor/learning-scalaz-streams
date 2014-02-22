scalaVersion := "2.10.2"

libraryDependencies += "org.scalaz.stream" %% "scalaz-stream" % "0.3"

libraryDependencies += "org.twitter4j" % "twitter4j-stream" % "3.0.5"

mainClass in Compile := Some("eg.tweets")

Revolver.settings

libraryDependencies += "org.specs2" %% "specs2" % "2.3.8" % "test"
