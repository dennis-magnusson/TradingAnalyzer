scalaVersion := "2.12.20"

name := "dataproducer"
version := "1.0"

libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "2.0.0"
libraryDependencies += "org.apache.kafka" % "kafka-clients" % "3.8.1"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.30"
