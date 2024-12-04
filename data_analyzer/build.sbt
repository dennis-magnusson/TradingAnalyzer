scalaVersion := "2.12.20"

name := "dataanalyzer"
version := "1.0"

libraryDependencies += "org.apache.kafka" % "kafka-streams" % "3.8.1"
libraryDependencies += "org.apache.kafka" % "kafka-clients" % "3.8.1"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.10" % Test
libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.15.2"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.30"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", _ @_*) => MergeStrategy.discard
  case "module-info.class"         => MergeStrategy.discard
  case x                           => MergeStrategy.first
}
