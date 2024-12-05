import org.apache.kafka.streams.KafkaStreams
import java.time.{Duration, Instant}

import org.slf4j.LoggerFactory

import Serializers.{TradeEventSerde, EMASerde}
import Models.{TradeEvent, EMA}
import Mappers.{advisoryMapper, emaMapper}
import Reducers.windowReducer

object KafkaStreamProcessor extends App {

  val emaTopicName = sys.env.getOrElse("EMA_TOPIC_NAME", "ema")
  val readTopicName = sys.env.getOrElse("READ_TOPIC_NAME", "trade-events")
  val advisoryTopicName = sys.env.getOrElse("ADVISORY_TOPIC_NAME", "advisory")
  val kafkaServer = sys.env.getOrElse("KAFKA_SERVER", "kafka:9092")

  val topology = StreamBuilder.buildTopology(
    readTopicName,
    emaTopicName,
    advisoryTopicName
  )

  println(topology.describe())

  val streams =
    new KafkaStreams(topology, KafkaConfig.getProperties(kafkaServer))

  streams.setUncaughtExceptionHandler((thread, throwable) => {
    println(s"Thread: $thread, Throwable: $throwable")
  })

  try {
    streams.start()
  } catch {
    case e: Throwable => e.printStackTrace()
  }
}
