/*
 * Joey testing SBT
 */

lazy val commonSettings = Seq(
  organization := "com.philips.prna",
  version := "1.0",
  scalaVersion := "2.11.7"
)

name := """LiveQA"""

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    libraryDependencies ++= Seq(
        "org.java-websocket" % "Java-WebSocket" % "1.3.0",
        "commons-httpclient" % "commons-httpclient" % "3.1",
        "log4j" % "log4j" % "1.2.16",
        "org.jsoup" % "jsoup" % "1.8.3"
    )
  )

fork in run := true
connectInput in run := true
