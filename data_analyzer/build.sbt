scalaVersion := "2.12.20"

name := "dataanalyzer"
version := "1.0"

libraryDependencies += "org.apache.spark" %% "spark-core" % "3.3.0" % "provided"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.3.0" % "provided"
libraryDependencies += "org.apache.spark" %% "spark-streaming" % "3.3.0" % "provided"
