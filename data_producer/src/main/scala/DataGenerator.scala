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

  val INTERVAL_MS = 500 // TODO: Make this dynamically configurable
  val CSV_PATH =
    "./data/debs2022-gc-trading-day-08-11-21.csv" // TODO: What about other days' data?

  val kafkaServer =
    "localhost:9092" // TODO: Point to the Kafka container

  val adminClientProps = new Properties()
  adminClientProps.put(
    "bootstrap.servers",
    kafkaServer
  )
  val adminClient = AdminClient.create(adminClientProps)

  val topicName = "trade-events"
  val partitions = 1
  val replicationFactor: Short = 1
  val topic = new NewTopic(topicName, partitions, replicationFactor)
  val topicCollection = Collections.singleton(topic)
  adminClient.createTopics(topicCollection)

  val producerProps = new Properties()
  producerProps.put("bootstrap.servers", kafkaServer)
  producerProps.put(
    "key.serializer",
    "org.apache.kafka.common.serialization.StringSerializer"
  )
  producerProps.put(
    "value.serializer",
    "org.apache.kafka.common.serialization.StringSerializer"
  )

  val producer = new KafkaProducer[String, String](producerProps)

  val file = new File(CSV_PATH)
  val reader = CSVReader.open(file)

  var row = reader.readNext()

  while (row.isDefined) {
    val values = row.get

    val record =
      new ProducerRecord[String, String](topicName, values.mkString(","))
    producer.send(record)

    Thread.sleep(INTERVAL_MS)
    row = reader.readNext()
  }

  reader.close()
}
