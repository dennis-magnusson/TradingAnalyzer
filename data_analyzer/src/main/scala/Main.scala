import org.apache.spark.sql.functions._
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.{DataFrame, SparkSession, Row}
import java.time.{Duration, Instant}
import com.influxdb.annotations.{Column, Measurement}
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import com.influxdb.client.{InfluxDBClient, InfluxDBClientFactory}
import com.influxdb.query.{FluxTable, FluxRecord}
import scala.collection.JavaConverters._

object Main extends App {

  val token = "1dd585b8fd63b23323387dae193220381521cdee5f1ea50086b091deae1c2c44"
  val org = "docs"
  val bucket = "home"
  val url = "http://influxdb:8086"

   val spark = SparkSession
  .builder
  .appName("StreaminSample")
  .getOrCreate()

import spark.implicits._ 

val df = spark.readStream.format("kafka").option("kafka.bootstrap.servers", "kafka:9092").option("subscribe", "trade-events").option("startingOffsets","earliest").load()
val cleandf = df.selectExpr("cast(key as string) key", "cast(value as string) value", "timestamp").select(
    split(col("value"),",").getItem(2).as("trading_value"),
    split(col("value"),",").getItem(3).as("tradingtime"), col("key"), col("timestamp"))

// val output = cleandf.select(col("key"), concat($"trading_value", lit(","), $"tradingtime", lit(","), $"timestamp").as("value"))

// output.writeStream.format("kafka").option("checkpointLocation" , "./spark").option("kafka.bootstrap.servers", "kafka:9092").option("topic", "topic1").start().awaitTermination()

// key is symbol, value 

// EMA,symbol={key} EMA={trading_value}\n

val output = cleandf.select(
  col("key"),
  col("trading_value").as("value")
)

// Define foreachBatch function
    
def writeToInfluxDB(spark: SparkSession, output: DataFrame, batchId: Long): Unit = {
  val client = InfluxDBClientFactory.create(url, token.toCharArray, org, bucket)
  val writeApi = client.getWriteApiBlocking
  
  try {
    output.collect().foreach { row =>
      val point = Point.measurement("measure")
        .addTag("symbol", "key")
        .addField("value", 55.0)
        .time(Instant.now(), WritePrecision.MS)
      
      writeApi.writePoint(point)
    }
  } catch {
    case e: Exception => 
      println(s"Error writing batch $batchId to InfluxDB: ${e.getMessage}")
      e.printStackTrace()
  } finally {
    client.close()
  }
}

// Write the streaming data to InfluxDB
output.writeStream
  .foreachBatch((df: DataFrame, batchId: Long) => writeToInfluxDB(spark, df, batchId))
  .outputMode("append")
  .start()
  .awaitTermination()

}