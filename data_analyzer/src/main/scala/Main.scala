import org.apache.spark.sql.functions._
import org.apache.spark.sql.SparkSession

object Main extends App {

  val latencyTopicName: String =
    sys.env.getOrElse("LATENCY_TOPIC_NAME", "timestamps")
  val tradeEventsTopicName: String =
    sys.env.getOrElse("TRADE_EVENTS_TOPIC_NAME", "trade-events")
  val kafkaServer: String = "kafka:9092"

  val spark = SparkSession.builder
    .appName("StreaminSample")
    .getOrCreate()

  import spark.implicits._

  val df = spark.readStream
    .format("kafka")
    .option("kafka.bootstrap.servers", kafkaServer)
    .option("subscribe", tradeEventsTopicName)
    .option("startingOffsets", "earliest")
    .load()
  val cleandf = df
    .selectExpr(
      "cast(key as string) key",
      "cast(value as string) value",
      "timestamp"
    )
    .select(
      split(col("value"), ",").getItem(2).as("trading_value"),
      split(col("value"), ",").getItem(3).as("tradingtime"),
      col("key"),
      col("timestamp")
    )

  val output = cleandf.select(
    col("key"),
    concat($"trading_value", lit(","), $"tradingtime", lit(","), $"timestamp")
      .as("value")
  )

  output.writeStream
    .format("kafka")
    .option("checkpointLocation", "./spark")
    .option("kafka.bootstrap.servers", kafkaServer)
    .option("topic", latencyTopicName)
    .start()
    .awaitTermination()

}
