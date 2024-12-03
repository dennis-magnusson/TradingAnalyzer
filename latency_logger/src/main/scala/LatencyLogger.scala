import java.util.{Collections, Properties}
import scala.collection.JavaConversions._

import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.clients.admin.{AdminClient, AdminClientConfig}

case class Timestamps(
    securityID: String,
    averageTradeValue: Float,
    maximumTradingTime: Long,
    windowStartEpoch: Long,
    windowEndEpoch: Long,
    windowStart: String,
    windowEnd: String,
    lastKafkaTimestamp: Long
)

object LatencyLogger extends App {
  val topicName: String = sys.env.getOrElse("TOPIC_NAME", "timestamps")
  val pollingInterval: Int = sys.env.getOrElse("POLLING_INTERVAL", "5000").toInt
  val latencyLogPath: String =
    sys.env.getOrElse("LATENCY_LOG_PATH", "./latency.log")
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

  def toEpochMilli(stringTimestamp: String): Long = {
    val isoFormattedTimestamp = stringTimestamp.replace(" ", "T") + "Z"
    java.time.Instant.parse(isoFormattedTimestamp).toEpochMilli
  }

  def parseRecord(
      record: org.apache.kafka.clients.consumer.ConsumerRecord[String, String]
  ): Timestamps = {
    val recordValues = record.value().split(",")
    val avg_value = recordValues(0).toFloat
    val maximum_trading_time = recordValues(1).toLong
    val windowStart = recordValues(2)
    val windowEnd = recordValues(3)
    val windowStartEpoch = toEpochMilli(windowStart)
    val windowEndEpoch = toEpochMilli(windowEnd)

    Timestamps(
      record.key(),
      avg_value,
      maximum_trading_time,
      windowStartEpoch,
      windowEndEpoch,
      windowStart,
      windowEnd,
      record.timestamp()
    )
  }

  def writeLatencyToFile(
      record: Timestamps
  ): Unit = {
    ???
  }

  def printLatency(timestamps: Timestamps): Unit = {
    val t0: Long = timestamps.maximumTradingTime
    val t1: Long = timestamps.lastKafkaTimestamp
    val t2: Long = java.time.Instant.now.toEpochMilli

    val endToEndLatency: Long = t2 - t0

    println(
      s"${timestamps.securityID} | window: ${timestamps.windowStart} - ${timestamps.windowEnd} | (t2-t0): ${endToEndLatency}ms | (t1-t0): ${t1 - t0}ms | (t2-t1): ${t2 - t1}ms"
    )
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
          printLatency(parseRecord(record))
        }
      }
    } finally {
      consumer.close()
    }
  }

  println(s"Consuming messages from topic: $topicName")
}
