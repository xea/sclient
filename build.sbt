name := "sclient"

version := "1.0"

scalaVersion := "2.11.4"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "junit" % "junit" % "4.8.1" % "test"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"

libraryDependencies += "org.rogach" %% "scallop" % "0.9.5"

libraryDependencies += "org.scalafx" %% "scalafx" % "8.0.0-R4"