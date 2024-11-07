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

  val INTERVAL_MS =
    500 // TODO: Make this dynamically configurable or simulate timestamps
  val CSV_PATH =
    "/data/debs2022-gc-trading-day-08-11-21.csv" // TODO: What about other days' data?

  val kafkaServer =
    "kafka:9092"

  val adminClientProps = new Properties()
  adminClientProps.put(
    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
    kafkaServer
  )
  val adminClient = AdminClient.create(adminClientProps)

  val topicName = "trade-events"
  val partitions = 1
  val replicationFactor: Short = 1
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

  val file = new File(CSV_PATH)
  val reader = CSVReader.open(file)

  println("Successfully opened CSV file: " + CSV_PATH)

  var row = reader.readNext()

  while (row.isDefined) {
    val values = row.get

    val record =
      new ProducerRecord[String, String](topicName, values.mkString(","))
    try {
      val metadata = producer.send(record).get()
      println(
        "Sent record: " + values.mkString(",") + " to topic: " + metadata
          .topic()
      )
    } catch {
      case e: Exception =>
        println(
          "Failed to send record: " + values.mkString(
            ","
          ) + "; Exception: " + e.getMessage
        )
    }

    Thread.sleep(INTERVAL_MS)
    row = reader.readNext()
  }

  reader.close()

  println("Reached end of file: " + CSV_PATH)
}
