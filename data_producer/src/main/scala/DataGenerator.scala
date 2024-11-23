import com.github.tototoshi.csv._
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord

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

  validateCsvPath(csvPath)
  createKafkaTopic(topicName, partitions, replicationFactor, kafkaServer)
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

  def createKafkaTopic(
      topicName: String,
      partitions: Int,
      replicationFactor: Short,
      kafkaServer: String
  ): Unit = {
    val adminClientProps = new Properties()
    adminClientProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer)
    val adminClient = AdminClient.create(adminClientProps)

    try {
      val topic = new NewTopic(topicName, partitions, replicationFactor)
      adminClient.createTopics(Collections.singleton(topic))
      println(s"Successfully created topic: $topicName")
    } catch {
      case e: Exception =>
        println(
          s"Failed to create topic: $topicName; Exception: ${e.getMessage}"
        )
    } finally {
      adminClient.close()
    }
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
    // ID, SecType, Last, Trading time, Trading date
    val timestamp = Instant.now.toEpochMilli
    Seq(values(0), values(1), values(21), values(23), values(26), timestamp)
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
