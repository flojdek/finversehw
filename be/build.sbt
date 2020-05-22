Global / cancelable := false

lazy val commonSettings = Seq(
  name := "be",
  organization := "io.yisland",
  version := "1.0",
  scalaVersion := "2.12.8",
  fork in run := true,
  libraryDependencies ++= Seq(
    guice,
    jdbc,
    evolutions,
    "org.playframework.anorm" %% "anorm" % "2.6.2",
    "org.postgresql" % "postgresql" % "42.2.9",
    "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test,
    "com.jason-goodwin" %% "authentikat-jwt" % "0.4.5"
  )
)

lazy val root = (project in file("."))
  .settings(
    commonSettings
  )
  .enablePlugins(PlayScala)
  .disablePlugins(PlayLayoutPlugin)
