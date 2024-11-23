import org.apache.spark.sql.functions._
import org.apache.spark.sql.SparkSession


object Main extends App {
    val spark = SparkSession
  .builder
  .appName("StreaminSample")
  .getOrCreate()

import spark.implicits._

val df = spark.readStream.format("kafka").option("kafka.bootstrap.servers", "kafka:9092").option("subscribe", "trade-events").option("startingOffsets","earliest").load()
val cleandf = df.selectExpr("cast(key as string) key", "cast(value as string) value", "timestamp").select(split(col("value"),",").getItem(1).as("symbol"),
    split(col("value"),",").getItem(2).as("trading_value"),
    split(col("value"),",").getItem(3).as("tradingtime"), col("key"), col("timestamp"))

val output = cleandf.select(col("symbol").as("key"), concat($"trading_value", lit(","), $"tradingtime", lit(","), $"timestamp").as("value"))

output.writeStream.format("kafka").option("checkpointLocation" , "./spark").option("kafka.bootstrap.servers", "kafka:9092").option("topic", "topic1").start()


}