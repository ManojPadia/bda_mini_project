name := "Wikipedia Topic Project"

version := "1.0"

scalaVersion := "2.11.12"

val sparkVersion = "2.4.3"
val hadoopAwsVersion = "2.7.7"
val awsJavaSdkVersion = "1.7.4"
val sparkNlpVersion="2.0.4"

libraryDependencies ++= Seq(
    "org.apache.spark" %% "spark-core" % sparkVersion,
    "org.apache.spark" %% "spark-sql" % sparkVersion,
    "org.apache.spark" %% "spark-mllib" % sparkVersion,

    //    "com.johnsnowlabs.nlp" %% "spark-nlp" % sparkNlpVersion,
    
    "org.apache.hadoop" % "hadoop-aws" % hadoopAwsVersion,
    "com.amazonaws" % "aws-java-sdk" % awsJavaSdkVersion
)
