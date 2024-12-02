import java.util.{Collections, Properties}
import scala.collection.JavaConversions._

import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.clients.admin.{AdminClient, AdminClientConfig}

object LatencyLogger extends App {
  val topicName: String = sys.env.getOrElse("TOPIC_NAME", "timestamps")
  val pollingInterval: Int = sys.env.getOrElse("POLLING_INTERVAL", "5000").toInt
  val kafkaServer: String = "kafka:9092"

  if (!checkTopicExistence(kafkaServer, topicName)) {
    println(s"Topic $topicName does not exist")
    System.exit(1)
  }

  val consumer = createKafkaConsumer(topicName)

  consumeMessages(consumer, pollingInterval)

  def createKafkaConsumer(topicName: String): KafkaConsumer[String, String] = {
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
    consumer.subscribe(Collections.singletonList(topicName))
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

  def consumeMessages(
      consumer: KafkaConsumer[String, String],
      pollingInterval: Int
  ): Unit = {
    try {
      while (true) {
        val records =
          consumer.poll(java.time.Duration.ofMillis(pollingInterval))
        for (record <- records.iterator()) {
          val generatorTimestampEpoch = record.value().split(",")(1).toLong
          val analyzerTimestamp = record.value().split(",")(2)
          val isoFormattedAnalyzerTimestamp =
            analyzerTimestamp.replace(" ", "T") + "Z"
          val analyzerTimestampEpoch =
            java.time.Instant.parse(isoFormattedAnalyzerTimestamp).toEpochMilli
          val lastKafkaTimestamp = record.timestamp()
          val latency = lastKafkaTimestamp - generatorTimestampEpoch
          println(s"Latency: ${latency}ms -> record with key: ${record
              .key()} received at: ${record.timestamp()}")
        }
      }
    } finally {
      consumer.close()
    }
  }

  println(s"Consuming messages from topic: $topicName")
}
