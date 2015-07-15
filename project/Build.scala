import org.stormenroute.mecha._
import sbt._
import sbt.Keys._

object Build extends MechaRepoBuild {
  lazy val buildSettings = Defaults.coreDefaultSettings ++
    MechaRepoPlugin.defaultSettings ++ Seq(
    name := "akka-ignite",
    scalaVersion := "2.11.7",
    version := "0.1",
    organization := "com.cleawing",
    libraryDependencies ++= superRepoDependencies("akka-ignite") ++ Dependencies.ignite ++ Dependencies.akka
      ++ Seq(Dependencies.typesafeConfig, Dependencies.scalaz, Dependencies.scalazScalaTest, Dependencies.scalaTest)
  )

  def repoName = "akka-ignite"

  lazy val finagleServices: Project = Project(
    "akka-ignite",
    file("."),
    settings = buildSettings
  ) dependsOnSuperRepo
}
