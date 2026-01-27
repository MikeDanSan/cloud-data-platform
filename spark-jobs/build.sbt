name := "cloud-data-platform-spark-jobs"

version := "0.1.0"

scalaVersion := "2.12.18"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-sql" % "3.5.0" % "provided",
  "org.apache.hadoop" % "hadoop-aws" % "3.3.4" % "provided"
)

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

assembly / assemblyJarName := s"${name.value}-${version.value}.jar"