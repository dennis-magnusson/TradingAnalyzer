import org.apache.kafka.streams.{StreamsBuilder, KeyValue}
import org.apache.kafka.streams.kstream.{
  TimeWindows,
  TimeWindowedKStream,
  Produced,
  KeyValueMapper,
  Windowed,
  KStream,
  Materialized
}
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.state.WindowStore
import org.apache.kafka.common.serialization.{Serdes, Serde}
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
import java.time.Duration
import java.util.Properties

import Serializers.TradeEventSerde
import Models.{TradeEvent, EMA}

object KafkaStreamProcessor extends App {

  val writeTopicName = sys.env.getOrElse("WRITE_TOPIC_NAME", "timestamps")
  val readTopicName = sys.env.getOrElse("READ_TOPIC_NAME", "trade-events")
  val kafkaServer = sys.env.getOrElse("KAFKA_SERVER", "kafka:9092")

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
  props.put(StreamsConfig.STATE_DIR_CONFIG, "/data")
  props.put(StreamsConfig.TOPIC_PREFIX + "cleanup.policy", "compact")
  props.put(StreamsConfig.TOPIC_PREFIX + "retention.ms", "172800000")
  props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, "1") // 2 days
  props.put(
    StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
    classOf[TradeEventSerde].getName
  )

  val smoothingFactorShort = 2.0 / (1 + 38)
  val smoothingFactorLong = 2.0 / (1 + 100)

  val builder = new StreamsBuilder()
  val inputStream = builder.stream[String, String](readTopicName)

  val parsedStream: KStream[String, TradeEvent] =
    inputStream.mapValues(value => {
      val parts = value.split(",")
      TradeEvent.fromParts(value.split(","))
    })

  val emaStream = parsedStream
    .groupByKey()
    .windowedBy(TimeWindows.of(Duration.ofMinutes(1)))
    .aggregate(
      EMA.init,
      (
          (symbol: Windowed[String], tradeEvent: TradeEvent, aggr: EMA) =>
            aggr.update(tradeEvent.lastPrice, smoothingFactorShort)
      ),
      Materialized.as[String, EMA, WindowStore[Bytes, Array[Byte]]]("ema-store")
    )
    .toStream()

  // print the emas
  emaStream.foreach((key, value) => {
    println(s"key: $key, value: $value")
  })

  // val advisoryStream = emaStream
  //   .mapValues((key, ema) => detectAdvisory(ema.shortEMA, ema.longEMA))
  //   .filter((_, advisory) => advisory.isDefined)
  //   .to(writeTopicName)

  val producedInstance =
    Produced.`with`(Serdes.String(), Serdes.String())

  val topology = builder.build()

  val streams = new KafkaStreams(topology, props)
  streams.start()
}
