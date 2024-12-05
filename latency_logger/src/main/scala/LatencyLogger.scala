import java.util.{Collections, Properties}
import scala.collection.JavaConversions._
import java.io.{FileWriter, BufferedWriter}
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
  val emaTopicName: String = sys.env.getOrElse("EMA_TOPIC_NAME", "ema")
  val advisoryTopicName: String =
    sys.env.getOrElse("ADVISORY_TOPIC_NAME", "advisory")
  val pollingInterval: Int = sys.env.getOrElse("POLLING_INTERVAL", "500").toInt
  val latencyLogPath: String =
    sys.env.getOrElse("LATENCY_LOG_PATH", "./latency.log")
  val kafkaServer: String = "kafka:9092"
  val log_path: String = sys.env.getOrElse("LOG_PATH", "/logs/latency.log")
  if (!checkTopicExistence(kafkaServer, emaTopicName)) {
    println(s"Topic $emaTopicName does not exist")
    System.exit(1)
  }
  // val log_files = new BufferedWriter(new FileWriter(log_path, true))

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

  // def parseAndPrintLatency(
  //     record: org.apache.kafka.clients.consumer.ConsumerRecord[String, String]
  // ): Unit = {
  //   val recordValues = record.value().split(",")
  //   val t0: Long = ???
  //   val t4: Long = java.time.Instant.now.toEpochMilli

  //   val endToEndLatency: Long = ???

  //   // println(...)
  // }
  def write_message_to_file(message: String): Unit = {
    val log_files = new BufferedWriter(new FileWriter(log_path, true))
    log_files.write(message)
    log_files.close()
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
    println(s"Polling interval: $pollingInterval ms")
    write_message_to_file("EMA Calculation Latency,Arrival Latency,Symbol,EMA Values,Human Readable Timestamp\n")
    try {
      while (true) {
        val records =
          consumer.poll(java.time.Duration.ofMillis(pollingInterval))
        for (record <- records.iterator()) {
          val recordValues = record.value().split(",")
          val t0: Long =
            recordValues(1).toLong // data producer timestamp (trade time)
          val t1: Long = toEpochMilli(
            recordValues(2)
          ) // kafka auto-generated timestamp (trade-events)
          val t2: Long = toEpochMilli(          printLatency(parseRecord(record))

            recordValues(3)
          ) // data analyzer timestamp
          val t3: Long =
            record.timestamp() // kafka auto-generated timestamp (timestamps)
          val t4: Long = java.time.Instant.now.toEpochMilli

          val endToEndLatency: Long =
            t4 - t0

          println(
            s"end-to-end (t4-t0): ${endToEndLatency}ms, t1-t0: ${t1 - t0}ms, t2-t1: ${t2 - t1}ms, t3-t2: ${t3 - t2}ms, t4-t3: ${t4 - t3}ms"
          )
        }
      }
    } finally {
      consumer.close()
    }
  }

  println(s"Consuming messages from topic: $emaTopicName")
}
