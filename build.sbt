name := "lass"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.11"

resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
libraryDependencies += filters
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % Test
libraryDependencies += "org.apache.spark" %% "spark-core" % "2.1.0"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.1.0"
libraryDependencies += "org.apache.spark" %% "spark-mllib" % "2.1.0"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.1",
  "com.typesafe.akka" %% "akka-agent" % "2.4.1",
  "com.typesafe.akka" %% "akka-camel" % "2.4.1",
  "com.typesafe.akka" %% "akka-multi-node-testkit" % "2.4.1",
  "com.typesafe.akka" %% "akka-persistence" % "2.4.1",
  "com.typesafe.akka" %% "akka-remote" % "2.4.1",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.1",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.1",
  "com.typesafe.akka" %% "akka-contrib" % "2.4.1",
  "com.typesafe.akka" %% "akka-http-core" % "10.0.6",
  "com.typesafe.akka" %% "akka-http" % "10.0.6",
  ws
)
dependencyOverrides ++= Set("com.fasterxml.jackson.core" % "jackson-databind" % "2.6.1")

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.example.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.example.binders._"
