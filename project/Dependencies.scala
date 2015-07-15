import sbt._

object Dependencies {
  object Versions {
    val typesafeConfig        = "1.3.0"
    val scalaz                = "7.1.3"
    val ignite                = "1.2.0-incubating"
    val akka                  = "2.4-M2"
    val scalazScalaTest       = "0.2.3"
    val scalaTest             = "2.2.5"
  }

  lazy val typesafeConfig       = "com.typesafe"      %   "config"            % Versions.typesafeConfig
  lazy val scalaz               = "org.scalaz"        %%  "scalaz-core"       % Versions.scalaz
  lazy val ignite = Seq(
                                  "org.apache.ignite" %   "ignite-core"       % Versions.ignite,
                                  "org.apache.ignite" %   "ignite-spring"     % Versions.ignite
  )
  lazy val akka = Seq(
                                  "com.typesafe.akka" %%  "akka-actor"        % Versions.akka,
                                  "com.typesafe.akka" %%  "akka-testkit"      % Versions.akka % "test"
  )

  lazy val scalazScalaTest      = "org.typelevel"     %%  "scalaz-scalatest"  % Versions.scalazScalaTest  % "test"
  lazy val scalaTest            = "org.scalatest"     %%  "scalatest"         % Versions.scalaTest        % "test"
}
