import org.apache.kafka.streams.{StreamsBuilder, KeyValue}
import org.apache.kafka.streams.kstream.{
  TimeWindows,
  TimeWindowedKStream,
  Produced,
  KeyValueMapper,
  Windowed
}
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
import java.time.Duration
import java.util.Properties

object KafkaStreamProcessor extends App {

  val writeTopicName = sys.env.getOrElse("WRITE_TOPIC_NAME", "timestamps")
  val readTopicName = sys.env.getOrElse("READ_TOPIC_NAME", "trade-events")
  val kafkaServer = sys.env.getOrElse("KAFKA_SERVER", "kafka:9092")

  // read from readTopicName on kafkaServer, aggregate a 5 minute tumbling window, write something for each window to writeTopicName

  // the events in readTopicName are key: [String], value: [String]

  val props = new Properties()
  props.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-stream-processor")
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

  val windowed: TimeWindowedKStream[String, String] =
    builder
      .stream[String, String](readTopicName)
      .groupByKey()
      .windowedBy(TimeWindows.of(Duration.ofMinutes(1)))
      .aggregate(
        () => (0.0, 0),
        (key: String, value: String, aggregate: (Double, Int)) => {
          val (sum, count) = aggregate
          val lastTradePrice = value.split(",")(1).toDouble
          (sum + lastTradePrice, count + 1)
        }
      )
      .toStream()
      .foreach((key, value) => {
        println(
          s"Key: ${key.key()} Window start: ${key
              .window()
              .start()}, Window end: ${key.window().end()}, Count: $value"
        )
      })

  val a: Nothing = windowed

  // counts.foreach((key, value) => {
  //   println(
  //     s"Key: ${key.key()} Window start: ${key.window().start()}, Window end: ${key.window().end()}, Count: $value"
  //   )
  // })

  val producedInstance =
    Produced.`with`(Serdes.String(), Serdes.String())

  val topology = builder.build()

  val streams = new KafkaStreams(topology, props)
  streams.start()
}
