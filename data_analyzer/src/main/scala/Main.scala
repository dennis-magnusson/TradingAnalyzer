import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import spark.implicits._

object Main extends App {

  val writeTopicName: String =
    sys.env.getOrElse("LATENCY_TOPIC_NAME", "timestamps")
  val readTopicName: String =
    sys.env.getOrElse("TRADE_EVENTS_TOPIC_NAME", "trade-events")
  val kafkaServer: String = "kafka:9092"

  val spark = SparkSession.builder
    .appName("StreaminSample")
    .getOrCreate()

  val df = spark.readStream
    .format("kafka")
    .option("kafka.bootstrap.servers", kafkaServer)
    .option("subscribe", readTopicName)
    .option("startingOffsets", "earliest")
    .load()

  val cleandf = df
    .selectExpr(
      "cast(key as string) key",
      "cast(value as string) value",
      "timestamp"
    )
    .select(
      split(col("value"), ",").getItem(2).cast("double").as("trading_value"),
      split(col("value"), ",").getItem(3).as("tradingtime"),
      col("key"),
      col("timestamp")
    )

  val windowedCounts = cleandf
    .withWatermark("timestamp", "2 minutes")
    .groupBy(
      window($"timestamp", "5 minutes"),
      $"key"
    )
    .agg(
      avg("trading_value").alias("avg_value"),
      max("tradingtime").alias("maximum_trading_time")
    )

  // val withCurrentTimestamp = cleandf.withColumn("current_timestamp", current_timestamp())

  val output = windowedCounts.select(
    col("key"),
    concat(
      col("avg_value"),
      lit(","),
      col("maximum_trading_time"),
      lit(","),
      col("window.start"),
      lit(","),
      col("window.end")
    ).alias("value")
  )

  // val query = output.writeStream
  //         .option("checkpointLocation" , "./sparkcheckpoint22")
  //       .outputMode("append")
  //       .format("console")
  //       .start()

  // val output = cleandf.select(col("key"), concat($"count", lit(","), $"window", lit(","), $"timestamp").as("value"))

  output.writeStream
    .format("kafka")
    .option("checkpointLocation", "./sparkcheckpoint2")
    .option("kafka.bootstrap.servers", kafkaServer)
    .option("topic", writeTopicName)
    .start()
    .awaitTermination()

}
