import java.io.File
import com.github.tototoshi.csv._
import org.apache.kafka.clients.admin.{AdminClient, NewTopic}
import org.apache.kafka.clients.producer.{
  KafkaProducer,
  ProducerConfig,
  ProducerRecord
}
import java.util.{Properties, Collections}

object DataGenerator extends App {

  val intervalMs: Int = sys.env.getOrElse("INTERVAL_MS", "500").toInt
  val csvPath: String =
    sys.env.getOrElse("CSV_PATH", "/data/debs2022-gc-trading-day-08-11-21.csv")
  val topicName: String = sys.env.getOrElse("TOPIC_NAME", "trade-events")
  val partitions: Int = sys.env.getOrElse("TOPIC_PARTITIONS", "1").toInt
  val replicationFactor: Short =
    sys.env.getOrElse("TOPIC_REPLICATION_FACTOR", "1").toShort
  val kafkaServer: String = "kafka:9092"

  validateCsvPath(csvPath)
  createKafkaTopic(topicName, partitions, replicationFactor, kafkaServer)
  val producer = createKafkaProducer(kafkaServer)
  processCsvAndSendData(csvPath, topicName, intervalMs, producer)

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
      intervalMs: Int,
      producer: KafkaProducer[String, String]
  ): Unit = {
    val file = new File(csvPath)
    val reader = CSVReader.open(file)

    reader.foreach { values =>
      if (isValidTickRow(values)) {
        val recordData = formatRecordData(values)
        println(s"Sending record: $recordData")
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
    Seq(values(0), values(1), values(21), values(23), values(26)).mkString(",")
  }

  def sendRecordToKafka(
      producer: KafkaProducer[String, String],
      topicName: String,
      data: String
  ): Unit = {
    val record = new ProducerRecord[String, String](topicName, data)
    try {
      producer.send(record).get()
    } catch {
      case e: Exception =>
        println(s"Failed to send record: $data; Exception: ${e.getMessage}")
    }
  }
}
