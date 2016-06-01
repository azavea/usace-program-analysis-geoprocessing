lazy val commonSettings = Seq(
  version := Version.version,
  scalaVersion := Version.scala,
  description := "USACE Program Analysis Geoprocessing",
  organization := "com.azavea",
  scalacOptions ++= Seq(
    "-deprecation",
    "-unchecked",
    "-Yinline-warnings",
    "-language:implicitConversions",
    "-language:reflectiveCalls",
    "-language:higherKinds",
    "-language:postfixOps",
    "-language:existentials",
    "-feature"),

  resolvers += Resolver.bintrayRepo("azavea", "geotrellis"),

  shellPrompt := { s => Project.extract(s).currentProject.id + " > " }
) ++ net.virtualvoid.sbt.graph.Plugin.graphSettings

lazy val root =
  Project("usace-programanalysis", file("."))
    .aggregate(geop)

lazy val geop =
  Project("geop", file("geop"))
    .settings(commonSettings: _*)
