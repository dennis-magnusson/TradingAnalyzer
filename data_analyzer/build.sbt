scalaVersion := "2.12.20"

name := "dataanalyzer"
organization := "com.example"
version := "1.0"

libraryDependencies += "org.apache.spark" %% "spark-core" % "3.3.0" % "provided"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.3.0" % "provided"
libraryDependencies += "org.apache.spark" %% "spark-streaming" % "3.3.0" % "provided"
libraryDependencies += "com.influxdb" % "influxdb-client-java" % "4.0.0" % "provided"

assemblyMergeStrategy in assembly := {
  case PathList("javax", "inject", _*) => MergeStrategy.last
  case PathList("org", "apache", _*) => MergeStrategy.last
  case PathList("org", "aopalliance", _*) => MergeStrategy.last
  case PathList("mime.types") => MergeStrategy.last
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}