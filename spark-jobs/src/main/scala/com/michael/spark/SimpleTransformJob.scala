package com.michael.spark

import org.apache.spark.sql.{SparkSession, SaveMode}
import org.apache.spark.sql.functions._

object SimpleTransformJob {
  def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      System.err.println("Usage: SimpleTransformJob <inputPath> <outputPath>")
      System.exit(1)
    }

    val inputPath = args(0)
    val outputPath = args(1)

    val spark = SparkSession.builder()
      .appName("SimpleTransformJob")
      .getOrCreate()

    println(s"Reading data from: $inputPath")
    
    // Read CSV with header
    val df = spark.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv(inputPath)

    println(s"Input schema:")
    df.printSchema()
    
    println(s"Row count: ${df.count()}")

    // Simple transformation: add a processing timestamp
    val transformedDf = df
      .withColumn("processed_at", current_timestamp())
      .withColumn("processed_year", year(current_timestamp()))
      .withColumn("processed_month", month(current_timestamp()))

    println(s"Writing data to: $outputPath")

    // Write as Parquet partitioned by year/month
    transformedDf.write
      .mode(SaveMode.Overwrite)
      .partitionBy("processed_year", "processed_month")
      .parquet(outputPath)

    println(s"Job completed successfully")
    
    spark.stop()
  }
}