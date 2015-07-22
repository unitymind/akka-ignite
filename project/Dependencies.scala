import sbt._

object Dependencies {
  object Versions {
    val typesafeConfig  = "1.3.0"
    val scalaz          = "7.1.3"
    val ignite          = "1.3.2"
    val akka            = "2.3.12"
    val akkaStreams     = "1.0"
    val opRabbit        = "1.0.0-M9"
    val scalazScalaTest = "0.2.3"
    val scalaTest       = "2.2.5"
  }

  lazy val typesafeConfig = "com.typesafe" % "config" % Versions.typesafeConfig

  lazy val ignite = Seq(
    "org.apache.ignite" % "ignite-core"   % Versions.ignite,
    "org.apache.ignite" % "ignite-spring" % Versions.ignite
  )

  lazy val akka = Seq(
    "com.typesafe.akka" %% "akka-actor"   % Versions.akka,
    "com.typesafe.akka" %% "akka-testkit" % Versions.akka % "test"
  )

  lazy val akkaStream = Seq(
    "com.typesafe.akka" %% "akka-stream-experimental"         % Versions.akkaStreams,
    "com.typesafe.akka" %% "akka-http-core-experimental"      % Versions.akkaStreams,
    "com.typesafe.akka" %% "akka-http-experimental"           % Versions.akkaStreams,
    "com.typesafe.akka" %% "akka-stream-testkit-experimental" % Versions.akkaStreams % "test",
    "com.typesafe.akka" %% "akka-http-testkit-experimental"   % Versions.akkaStreams % "test"
  )

  lazy val opRabbit = Seq(
    "com.spingo"            %% "op-rabbit-core" % Versions.opRabbit
  )

  lazy val scalaz = Seq(
    "org.scalaz"    %%  "scalaz-core"       % Versions.scalaz,
    "org.typelevel" %%  "scalaz-scalatest"  % Versions.scalazScalaTest % "test"
  )

  lazy val scalaTest = "org.scalatest" %% "scalatest" % Versions.scalaTest % "test"
}
