import com.github.tototoshi.csv._
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.LocalDateTime
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Collections
import java.util.Properties

object DataGenerator extends App {

  val speedFactor: Int = sys.env.getOrElse("SPEED_FACTOR", "1.0").toInt
  val csvPath: String =
    sys.env.getOrElse("CSV_PATH", "/data/debs2022-gc-trading-day-08-11-21.csv")
  val topicName: String = sys.env.getOrElse("TOPIC_NAME", "trade-events")
  val partitions: Int = sys.env.getOrElse("TOPIC_PARTITIONS", "1").toInt
  val replicationFactor: Short =
    sys.env.getOrElse("TOPIC_REPLICATION_FACTOR", "1").toShort
  val printSentRecords: Boolean =
    sys.env.getOrElse("PRINT_SENT_RECORDS", "false").toBoolean
  val kafkaServer: String = "kafka:9092"
  val marketOpenTime = "07:00:00.000"
  val TestState: Boolean = sys.env.getOrElse("TEST_STATE", "false").toBoolean

  validateCsvPath(csvPath)

  val producer = createKafkaProducer(kafkaServer)
  processCsvAndSendData(
    csvPath,
    topicName,
    producer,
    printSentRecords,
    speedFactor
  )

  producer.close()
  println(s"Reached end of file: $csvPath")

  def validateCsvPath(path: String): Unit = {
    if (!new File(path).exists()) {
      println(s"Source data file not found: $path")
      System.exit(1)
    }
    println(s"Using CSV file: $path")
  }

  def createKafkaProducer(
      kafkaServer: String
  ): KafkaProducer[String, String] = {
    val producerProps = new Properties()
    producerProps.put("bootstrap.servers", kafkaServer)
    producerProps.put(
      ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
      "org.apache.kafka.common.serialization.StringSerializer"
    )
    producerProps.put(
      ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
      "org.apache.kafka.common.serialization.StringSerializer"
    )
    new KafkaProducer[String, String](producerProps)
  }

  def processCsvAndSendData(
      csvPath: String,
      topicName: String,
      producer: KafkaProducer[String, String],
      printSentRecords: Boolean,
      speedFactor: Int
  ): Unit = {
    if (TestState) {
      var value: Double = 1.0
      var shortema: Double = 0.0
      var longema: Double = 0.0
      var increasing_factor: Double = 1.0
    while (true) {
      val key = "testKey"
      val timestamp = Instant.now.toEpochMilli
      val record_value = Seq(key, key, value, timestamp).mkString(",")
      sendRecordToKafka(producer, topicName, key, record_value)
      
      shortema = (0.051282051)*value + (1-0.051282051)*shortema // previous value more important
      longema = (0.01980198)*value + (1-0.01980198)*longema // new value more important
      println(s"Sent record with key: <$key>, value: <$record_value>, shortema: <$shortema> longema: <$longema>")
      // println(s"EMA : $ema")
      value += increasing_factor
      if (value > 100) {
        increasing_factor = -1.0
      }else if (value < 20) {
        increasing_factor = 1.0
      }
      val sleep_time = (60*1000)/speedFactor
      Thread.sleep(sleep_time)
    }

    }
    else {
    val file = new File(csvPath)
    val reader = CSVReader.open(file)

    var previousTradingTime: Long = parseTradingTime(marketOpenTime)

    reader.foreach { values =>
      if (isValidTickRow(values)) {
        val recordData = formatRecordData(values)
        val currentTradingTime: Long = parseTradingTime(values(23))
        val intervalMs =
          calculateInterval(currentTradingTime, previousTradingTime)

        previousTradingTime = currentTradingTime

        val key = values(0)
        val value = recordData
        sendRecordToKafka(producer, topicName, key, value)
        if (printSentRecords) {
          println(s"Sent record with key: <$key>, value: <$value>")
        }
        Thread.sleep(intervalMs / speedFactor)
      }
    }

    reader.close()
    }
  }

  def isValidTickRow(values: Seq[String]): Boolean = {
    values.length == 40 &&
    values(21).trim.nonEmpty &&
    isParsableFloat(values(21).trim) &&
    values(21).trim.toFloat != 0.0
  }

  def isParsableFloat(value: String): Boolean = {
    try {
      value.toFloat
      true
    } catch {
      case _: NumberFormatException => false
    }
  }

  def formatRecordData(values: Seq[String]): String = {
    // ID, SecType, Last, Trading time (now), Trading date
    val timestamp = Instant.now.toEpochMilli
    Seq(values(0), values(1), values(21), timestamp, values(26))
      .mkString(",")
  }

  def parseTradingTime(time: String): Long = {
    val format = new SimpleDateFormat("HH:mm:ss.SSS")
    val date = format.parse(time)
    date.getTime
  }

  def calculateInterval(
      previousTradingTime: Long,
      currentTradingTime: Long
  ): Long = {
    Math.abs(currentTradingTime - previousTradingTime)
  }

  def sendRecordToKafka(
      producer: KafkaProducer[String, String],
      topicName: String,
      key: String,
      value: String
  ): Unit = {
    val record = new ProducerRecord[String, String](topicName, key, value)
    try {
      producer.send(record).get()
    } catch {
      case e: Exception =>
        println(
          s"Failed to send record; key: <$key> value: <$value>; Exception: ${e.getMessage}"
        )
        println(
          s"Failed to send record; key: <$key> value: <$value>; Exception: ${e.getMessage}"
        )
    }
  }
}
