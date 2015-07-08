name := "SpatialScala"
 
version := "1.0"
 
scalaVersion := "2.11.6"
 
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor"  % "2.3.4",
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
)
