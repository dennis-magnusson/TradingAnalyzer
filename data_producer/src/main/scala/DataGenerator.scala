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

  val intervalMs = sys.env.getOrElse("INTERVAL_MS", "500").toInt
  val csvPath =
    sys.env.getOrElse("CSV_PATH", "/data/debs2022-gc-trading-day-08-11-21.csv")
  val topicName = sys.env.getOrElse("TOPIC_NAME", "trade-events")
  val partitions = sys.env.getOrElse("TOPIC_PARTITIONS", "1").toInt
  val replicationFactor: Short =
    sys.env.getOrElse("TOPIC_REPLICATION_FACTOR", "1").toShort

  if (!(new File(csvPath)).exists()) {
    println("Source data file not found: " + csvPath)
    System.exit(1)
  }

  println("Using csv file: " + csvPath)

  val kafkaServer =
    "kafka:9092"

  val adminClientProps = new Properties()
  adminClientProps.put(
    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
    kafkaServer
  )
  val adminClient = AdminClient.create(adminClientProps)

  val topic = new NewTopic(topicName, partitions, replicationFactor)
  val topicCollection = Collections.singleton(topic)

  try {
    adminClient.createTopics(topicCollection)
    println("Successfully created topic: " + topicName)
  } catch {
    case e: Exception =>
      println(
        "Failed to create topic: " + topicName + "; Exception: " + e.getMessage
      )
  } finally {
    adminClient.close()
  }

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

  val producer = new KafkaProducer[String, String](producerProps)

  val file = new File(csvPath)
  val reader = CSVReader.open(file)

  var row = reader.readNext()

  while (row.isDefined) {
    val values = row.get
    val isTickRow = values.length == 40 && !values(21).trim.isEmpty
    if (isTickRow) {
      // TODO: Check that critical values are not empty
      val record =
        new ProducerRecord[String, String](topicName, values.mkString(","))
      try {
        val metadata = producer.send(record).get()
      } catch {
        case e: Exception =>
          println(
            "Failed to send record: " + values.mkString(
              ","
            ) + "; Exception: " + e.getMessage
          )
      }

      Thread.sleep(intervalMs)

    }

    row = reader.readNext()
  }

  reader.close()

  println("Reached end of file: " + csvPath)
}
