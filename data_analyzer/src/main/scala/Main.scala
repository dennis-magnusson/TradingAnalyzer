import org.apache.kafka.streams.{StreamsBuilder, KeyValue}
import org.apache.kafka.streams.kstream.{
  Suppressed,
  TimeWindows,
  TimeWindowedKStream,
  Produced,
  KeyValueMapper,
  ValueMapper,
  Windowed,
  KStream,
  Materialized,
  Reducer
}
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.state.WindowStore
import org.apache.kafka.common.serialization.{Serdes, Serde}
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
import org.apache.kafka.clients.consumer.ConsumerConfig
import java.time.{Duration, Instant}
import java.util.Properties

import Serializers.{TradeEventSerde, EMASerde}
import Models.{TradeEvent, EMA}
import org.slf4j.LoggerFactory

object KafkaStreamProcessor extends App {

  val logger = LoggerFactory.getLogger(getClass)

  val emaTopicName = sys.env.getOrElse("EMA_TOPIC_NAME", "ema")
  val readTopicName = sys.env.getOrElse("READ_TOPIC_NAME", "trade-events")
  val advisoryTopicName = sys.env.getOrElse("ADVISORY_TOPIC_NAME", "advisory")
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
    Serdes.String().getClass
  )
  // props.put(
  //   StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
  //   classOf[TradeEventSerde].getName
  // )
  props.put(StreamsConfig.STATE_DIR_CONFIG, "/data")
  props.put(StreamsConfig.TOPIC_PREFIX + "cleanup.policy", "compact")
  props.put(StreamsConfig.TOPIC_PREFIX + "retention.ms", "172800000")
  props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, "1")

  props.put(
    ConsumerConfig.GROUP_INSTANCE_ID_CONFIG,
    "data-analyzer-group-inst-id"
  )
  props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "10000")
  props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, "1000")

  var emaValues = Map[String, EMA]()

  val builder = new StreamsBuilder()
  val inputStream = builder.stream[String, String](readTopicName)

  val parsedStream: KStream[String, TradeEvent] =
    inputStream.mapValues(value => {
      val parts = value.split(",")
      TradeEvent.fromParts(value.split(","))
    })

  val reducer: Reducer[TradeEvent] = new Reducer[TradeEvent] {
    override def apply(
        lastEvent: TradeEvent,
        newEvent: TradeEvent
    ): TradeEvent = {
      if (newEvent.tradeTime > lastEvent.tradeTime) newEvent else lastEvent
    }
  }

  val materialized
      : Materialized[String, TradeEvent, WindowStore[Bytes, Array[Byte]]] =
    Materialized
      .`as`[String, TradeEvent, WindowStore[Bytes, Array[Byte]]](
        "trade-events-store"
      )
      .withKeySerde(Serdes.String())
      .withValueSerde(new TradeEventSerde())

  val tradeEventStream: KStream[Windowed[String], TradeEvent] = parsedStream
    .groupByKey()
    .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(2)))
    .reduce(reducer, materialized)
    .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
    .toStream()

  val emaCalculation
      : KeyValueMapper[Windowed[String], TradeEvent, KeyValue[String, EMA]] =
    new KeyValueMapper[Windowed[String], TradeEvent, KeyValue[String, EMA]] {
      override def apply(
          windowed: Windowed[String],
          tradeEvent: TradeEvent
      ): KeyValue[String, EMA] = {
        val winStart = windowed.window().startTime()
        val winEnd = windowed.window().endTime()
        val symbol = windowed.key()
        val lastEventTime = tradeEvent.tradeTime

        val ema = emaValues.getOrElse(symbol, new EMA())
        emaValues += (symbol -> ema.update(tradeEvent.lastPrice))
        KeyValue.pair(symbol, ema)
      }
    }

  val emaStream: KStream[String, EMA] = tradeEventStream
    .map[String, EMA](emaCalculation)
  // .peek((key, value) => {
  //   logger.info(s"$key -> $value")
  // })

  val emaOutput: KStream[String, String] = emaStream.mapValues(ema => {
    ema.toString
  })

  emaOutput.to(
    emaTopicName,
    Produced.`with`(Serdes.String(), Serdes.String())
  )

  val advisoryMapper: KeyValueMapper[String, EMA, KeyValue[String, String]] =
    new KeyValueMapper[String, EMA, KeyValue[String, String]] {
      override def apply(symbol: String, ema: EMA): KeyValue[String, String] = {
        if (ema.bullishCrossOver) KeyValue.pair(symbol, "BUY")
        else if (ema.bearishCrossOver) KeyValue.pair(symbol, "SELL")
        else KeyValue.pair(symbol, "-")
      }
    }

  val advisoryStream: KStream[String, String] = emaStream
    .map[String, String](advisoryMapper)
    .filter((_, advisory) => advisory != "-")

  advisoryStream.to(
    advisoryTopicName,
    Produced.`with`(Serdes.String(), Serdes.String())
  )
  // .peek((key, advisory) => {
  //   logger.info(s"ALERT: ${key},${advisory}")
  // })

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
