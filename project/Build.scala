import org.stormenroute.mecha._
import sbt._
import sbt.Keys._
import sbtassembly._
import sbtassembly.AssemblyKeys._

object Build extends MechaRepoBuild {
  val customMergeStrategy: String => MergeStrategy = {
    case x if Assembly.isConfigFile(x) =>
      MergeStrategy.concat
    case PathList(ps @ _*) if Assembly.isReadme(ps.last) || Assembly.isLicenseFile(ps.last) =>
      MergeStrategy.rename
    case PathList("META-INF", xs @ _*) =>
      xs map {_.toLowerCase} match {
        case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
          MergeStrategy.discard
        case ps @ (x :: xs) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
          MergeStrategy.discard
        case "plexus" :: xs =>
          MergeStrategy.discard
        case "spring.tooling" :: xs =>
          MergeStrategy.last
        case "services" :: xs =>
          MergeStrategy.filterDistinctLines
        case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) =>
          MergeStrategy.filterDistinctLines
        case _ => MergeStrategy.deduplicate
      }
    case "asm-license.txt" | "overview.html" =>
      MergeStrategy.discard
    case _ => MergeStrategy.deduplicate
  }

  lazy val buildSettings = Defaults.coreDefaultSettings ++
    MechaRepoPlugin.defaultSettings ++ Seq(
    name := "akka-ignite",
    scalaVersion := "2.11.7",
    scalacOptions ++= Seq("-unchecked", "-feature", "-deprecation", "-language:postfixOps"),
    version := "0.1",
    organization := "com.cleawing",
    resolvers += "GridGain External Repository" at "http://www.gridgainsystems.com/nexus/content/repositories/external",
    libraryDependencies ++= superRepoDependencies("akka-ignite") ++ Dependencies.ignite ++ Dependencies.akka
      ++ Dependencies.akkaStream ++ Dependencies.scalaz
      ++ Seq(Dependencies.typesafeConfig, Dependencies.scalaTest),
    assemblyMergeStrategy in assembly := customMergeStrategy,
    mainClass in assembly := Some("com.cleawing.ignite.MainApp"),
    initialCommands in console :=
      """
        |import akka.actor._
        |import com.cleawing.ignite.akka.IgniteExtension
        |import scala.collection.JavaConversions._
        |import com.cleawing.ignite.Implicits._
        |
        |val system = ActorSystem()
        |implicit val ignite = IgniteExtension(system)
      """.stripMargin
  )

  def repoName = "akka-ignite"

  lazy val akkaIgnite: Project = Project(
    "akka-ignite",
    file("."),
    settings = buildSettings
  ) dependsOnSuperRepo
}
