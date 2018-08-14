import sbt._

object Dependencies {
  val extraResolvers = Seq(
    Resolver.mavenLocal,
    "google" at "https://maven-central-asia.storage-download.googleapis.com/repos/central/data/",
    "aliyun" at "http://maven.aliyun.com/nexus/content/groups/public/"
  )

  object Versions {
    val ammonite = "1.1.2"
    val config = "1.3.3"
    val fastparse = "1.0.0"
    val log4j = "1.2.17"
    val mockito = "2.21.0"
    val scala = "2.12.6"
    val scalaCheck = "1.14.0"
    val scalaTest = "3.0.5"
    val scalaXml = "1.1.0-newCollectionsBootstrap"
    val scopt = "3.7.0"
    val slf4j = "1.7.25"

    val sourcecode = "0.1.4"
  }

  val ammonite = Seq(
    "com.lihaoyi" % s"ammonite_${Versions.scala}" % Versions.ammonite
  )

  val fastparse = Seq(
    "com.lihaoyi" %% "fastparse" % Versions.fastparse
  )

  val log4j = Seq(
    "log4j" % "log4j" % Versions.log4j
  )

  val mockito = Seq(
    "org.mockito" % "mockito-core" % Versions.mockito % "test"
  )

  val scala = Seq(
    "org.scala-lang" % "scala-library" % Versions.scala,
    "org.scala-lang" % "scala-reflect" % Versions.scala,
    "org.scala-lang.modules" %% "scala-xml" % Versions.scalaXml
  )

  val scalaCheck = Seq(
    "org.scalacheck" %% "scalacheck" % Versions.scalaCheck % "test"
  )

  val scalaTest = Seq(
    "org.scalatest" %% "scalatest" % Versions.scalaTest % "test"
  )

  val scopt = Seq(
    "com.github.scopt" %% "scopt" % Versions.scopt
  )

  val slf4j = Seq(
    "org.slf4j" % "slf4j-api" % Versions.slf4j,
    "org.slf4j" % "slf4j-log4j12" % Versions.slf4j,
    "org.slf4j" % "jul-to-slf4j" % Versions.slf4j
  )

  val sourcecode = Seq(
    "com.lihaoyi" %% "sourcecode" % Versions.sourcecode
  )

  val typesafeConfig = Seq(
    "com.typesafe" % "config" % Versions.config
  )

  val testing: Seq[ModuleID] = mockito ++ scalaCheck ++ scalaTest

  val logging: Seq[ModuleID] = log4j ++ slf4j

  val overrides: Set[ModuleID] = (log4j ++ scala ++ slf4j ++ sourcecode).toSet
}
