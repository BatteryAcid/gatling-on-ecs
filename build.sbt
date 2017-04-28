name := "Backend_Services_Performance_Test"

version := "1.0"

enablePlugins(GatlingPlugin)

scalaVersion := "2.11.8"

javaOptions in Gatling := Seq("-Xms1G", "-Xmx3G", "-Djava.net.preferIPv4Stack=true", "-Djava.net.preferIPv6Addresses=false")

scalacOptions := Seq(
  "-encoding", "UTF-8", "-target:jvm-1.8", "-deprecation",
  "-feature", "-unchecked", "-language:implicitConversions", "-language:postfixOps")

libraryDependencies ++= {
  val version = "2.2.5"
  Seq(
    "io.gatling.highcharts" % "gatling-charts-highcharts" % version % "test,it",
    "io.gatling" % "gatling-test-framework" % version % "test,it"
  )
}
