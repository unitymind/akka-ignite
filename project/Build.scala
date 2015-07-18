import org.stormenroute.mecha._
import sbt._
import sbt.Keys._

object Build extends MechaRepoBuild {
  lazy val buildSettings = Defaults.coreDefaultSettings ++
    MechaRepoPlugin.defaultSettings ++ Seq(
    name := "akka-ignite",
    scalaVersion := "2.11.7",
    scalacOptions ++= Seq("-unchecked", "-feature", "-deprecation", "-language:postfixOps"),
    version := "0.1",
    organization := "com.cleawing",
    libraryDependencies ++= superRepoDependencies("akka-ignite") ++ Dependencies.ignite ++ Dependencies.akka
      ++ Seq(Dependencies.typesafeConfig, Dependencies.scalaz, Dependencies.scalazScalaTest, Dependencies.scalaTest),
    initialCommands in console :=
      """
        |import akka.actor._
        |import com.cleawing.ignite.akka.IgniteExtension
        |import scala.collection.JavaConversions._
        |
        |val system = ActorSystem()
        |val ignite = IgniteExtension(system)
      """.stripMargin
  )

  def repoName = "akka-ignite"

  lazy val finagleServices: Project = Project(
    "akka-ignite",
    file("."),
    settings = buildSettings
  ) dependsOnSuperRepo
}
