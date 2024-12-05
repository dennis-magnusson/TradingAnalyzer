import java.util.{Collections, Properties}
import scala.collection.JavaConversions._

import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.clients.admin.{AdminClient, AdminClientConfig}

object LatencyLogger extends App {
  val emaTopicName: String = sys.env.getOrElse("EMA_TOPIC_NAME", "ema")
  val advisoryTopicName: String =
    sys.env.getOrElse("ADVISORY_TOPIC_NAME", "advisory")
  val pollingInterval: Int = sys.env.getOrElse("POLLING_INTERVAL", "500").toInt
  val kafkaServer: String = "kafka:9092"

  if (!checkTopicExistence(kafkaServer, emaTopicName)) {
    println(s"Topic $emaTopicName does not exist")
    System.exit(1)
  }

  val consumer = createKafkaConsumer(emaTopicName)

  consumeMessages(consumer, pollingInterval)

  def createKafkaConsumer(
      emaTopicName: String
  ): KafkaConsumer[String, String] = {
    val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer)
    props.put(
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
      "org.apache.kafka.common.serialization.StringDeserializer"
    )
    props.put(
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
      "org.apache.kafka.common.serialization.StringDeserializer"
    )
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "logger")

    val consumer = new KafkaConsumer[String, String](props)
    consumer.subscribe(Collections.singletonList(emaTopicName))
    consumer
  }

  def checkTopicExistence(kafkaServer: String, topicName: String): Boolean = {
    val props = new Properties()
    props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer)
    val adminClient = AdminClient.create(props)

    try {
      val topics = adminClient.listTopics().names().get()
      topics.contains(topicName)
    } catch {
      case e: Exception =>
        println(s"Failed to verify topic existence: ${e.getMessage}")
        false
    } finally {
      adminClient.close()
    }
  }

  def toEpochMilli(stringTimestamp: String): Long = {
    val isoFormattedTimestamp = stringTimestamp.replace(" ", "T") + "Z"
    java.time.Instant.parse(isoFormattedTimestamp).toEpochMilli
  }

  def parseAndPrintLatency(
      record: org.apache.kafka.clients.consumer.ConsumerRecord[String, String]
  ): Unit = {
    val recordValues = record.value().split(",")
    val t0: Long = ???
    val t4: Long = java.time.Instant.now.toEpochMilli

    val endToEndLatency: Long = ???

    // println(...)
  }

  def consumeMessages(
      consumer: KafkaConsumer[String, String],
      pollingInterval: Int
  ): Unit = {
    println(s"Polling interval: $pollingInterval ms")
    try {
      while (true) {
        val records =
          consumer.poll(java.time.Duration.ofMillis(pollingInterval))
        for (record <- records.iterator()) {
          println(s"${record.key()}, ${record.value()}")
        }
      }
    } finally {
      consumer.close()
    }
  }

  println(s"Consuming messages from topic: $emaTopicName")
}
