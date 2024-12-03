import org.apache.kafka.streams.{KafkaStreams, StreamsBuilder, StreamsConfig}
import org.apache.kafka.streams.kstream.{TimeWindows, Materialized}
import org.apache.kafka.common.serialization.Serdes
import java.time.Duration
import java.util.Properties

object KafkaStreamProcessor extends App {

  val writeTopicName = sys.env.getOrElse("WRITE_TOPIC_NAME", "timestamps")
  val readTopicName = sys.env.getOrElse("READ_TOPIC_NAME", "trade-events")
  val kafkaServer = sys.env.getOrElse("KAFKA_SERVER", "kafka:9092")

  val props = new Properties()
  props.put(StreamsConfig.APPLICATION_ID_CONFIG, "trading-analyzer")
  props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer)
  props.put(
    StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,
    Serdes.String().getClass
  )
  props.put(
    StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
    Serdes.String().getClass
  )

  val builder = new StreamsBuilder()
  val tradingEvents = builder.stream[String, String](readTopicName)

  println("Hello World")
}
