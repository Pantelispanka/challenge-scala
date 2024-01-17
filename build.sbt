name := "test-scala"

version := "0.1"

scalaVersion := "2.13.12"

resolvers += "Akka library repository".at("https://repo.akka.io/maven")

lazy val akkaVersion = "2.9.0"
lazy val akkaHttp = "10.6.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor"      % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" %akkaVersion,
  "com.typesafe.akka" %% "akka-http-core"  % akkaHttp,
  "com.typesafe.akka" %% "akka-http"       % akkaHttp,
  "com.typesafe.akka" %% "akka-slf4j"      % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-spray-json"     % akkaHttp,
  "com.typesafe" % "config" % "1.4.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.1.0" % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % "10.5.0" % Test
)