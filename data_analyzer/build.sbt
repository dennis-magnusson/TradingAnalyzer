scalaVersion := "2.12.20"

name := "dataanalyzer"
version := "1.0"

libraryDependencies += "org.apache.kafka" % "kafka-streams" % "3.8.1"
libraryDependencies += "org.apache.kafka" % "kafka-clients" % "3.8.1"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", _ @_*) => MergeStrategy.discard
  case "module-info.class"         => MergeStrategy.discard
  case x                           => MergeStrategy.first
}
