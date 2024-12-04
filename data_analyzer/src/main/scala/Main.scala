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
import org.apache.kafka.clients.consumer.ConsumerConfig
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
  props.put(StreamsConfig.APPLICATION_ID_CONFIG, "data-analyzer")
  props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer)
  props.put(
    StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,
    Serdes.String().getClass
  )
  props.put(
    StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
    classOf[TradeEventSerde].getName
  )
  props.put(StreamsConfig.STATE_DIR_CONFIG, "/data")
  props.put(StreamsConfig.TOPIC_PREFIX + "cleanup.policy", "compact")
  props.put(StreamsConfig.TOPIC_PREFIX + "retention.ms", "172800000")
  props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, "1") // 2 days

  props.put(
    ConsumerConfig.GROUP_INSTANCE_ID_CONFIG,
    "data-analyzer-group-inst-id"
  )
  props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "10000")
  // props.put(
  //   StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG,
  //   "org.apache.kafka.streams.errors.LogAndContinueExceptionHandler"
  // )

  val smoothingFactorShort = 2.0 / (1 + 38)
  val smoothingFactorLong = 2.0 / (1 + 100)

  val builder = new StreamsBuilder()
  val inputStream = builder.stream[String, String](readTopicName)

  val parsedStream: KStream[String, TradeEvent] =
    inputStream.mapValues(value => {
      val parts = value.split(",")
      TradeEvent.fromParts(value.split(","))
    })

  // def aggregator(symbol: String, tradeEvent: TradeEvent, aggr: EMA): EMA = {
  //   aggr.update(
  //     tradeEvent.lastPrice,
  //     smoothingFactorShort,
  //     smoothingFactorLong
  //   )
  // }

  // val emaStream = parsedStream
  //   .groupByKey()
  //   .windowedBy(TimeWindows.of(Duration.ofMinutes(1)))
  //   .aggregate(
  //     EMA.initializer
  //     aggregator,
  //     Materialized.`as`("ema-store")
  //   )
  //   .toStream()

  // print the emas
  parsedStream.foreach((key, value) => {
    println(s"key: $key, value: $value")
  })

  // val advisoryStream = emaStream
  //   .mapValues((key, ema) => detectAdvisory(ema.shortEMA, ema.longEMA))
  //   .filter((_, advisory) => advisory.isDefined)
  //   .to(writeTopicName)

  val topology = builder.build()

  println(topology.describe())

  val streams = new KafkaStreams(topology, props)

  streams.setUncaughtExceptionHandler((thread, throwable) => {
    println(s"Thread: $thread, Throwable: $throwable")
  })

  try {
    streams.start()
  } catch {
    case e: Throwable => e.printStackTrace()
  }
}
