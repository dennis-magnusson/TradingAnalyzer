import java.io.File
import java.text.SimpleDateFormat
import com.github.tototoshi.csv._
import org.apache.kafka.clients.admin.{AdminClient, NewTopic}
import org.apache.kafka.clients.producer.{
  KafkaProducer,
  ProducerConfig,
  ProducerRecord
}
import java.util.{Properties, Collections}

object DataGenerator extends App {

  val csvPath: String =
    sys.env.getOrElse("CSV_PATH", "/data/debs2022-gc-trading-day-08-11-21.csv")
  val topicName: String = sys.env.getOrElse("TOPIC_NAME", "trade-events")
  val partitions: Int = sys.env.getOrElse("TOPIC_PARTITIONS", "1").toInt
  val replicationFactor: Short =
    sys.env.getOrElse("TOPIC_REPLICATION_FACTOR", "1").toShort
  val kafkaServer: String = "kafka:9092"
  val marketOpenTime = "07:00:00.000"

  validateCsvPath(csvPath)
  createKafkaTopic(topicName, partitions, replicationFactor, kafkaServer)
  val producer = createKafkaProducer(kafkaServer)
  processCsvAndSendData(csvPath, topicName, producer)

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
      producer: KafkaProducer[String, String]
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

        println(s"Sending record: $recordData Interval: $intervalMs ms")
        sendRecordToKafka(producer, topicName, recordData)
        Thread.sleep(intervalMs)
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
    Seq(values(0), values(1), values(21), values(23), values(26)).mkString(",")
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
      data: String
  ): Unit = {
    val record = new ProducerRecord[String, String](topicName, data)
    try {
      // TODO: Key value ?
      producer.send(record).get()
    } catch {
      case e: Exception =>
        println(s"Failed to send record: $data; Exception: ${e.getMessage}")
    }
  }
}
