import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.state.WindowStore
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.kstream.Windowed
import org.apache.kafka.streams.kstream.{
  KStream,
  Materialized,
  Produced,
  Suppressed,
  TimeWindows
}
import java.time.Duration

import Models.{TradeEvent, EMA}
import Reducers.windowReducer
import Serializers.TradeEventSerde
import Mappers.{advisoryMapper, emaMapper}

object StreamBuilder {

  def buildTopology(
      readTopicName: String,
      emaTopicName: String,
      advisoryTopicName: String
  ) = {
    val builder = new StreamsBuilder()
    val inputStream = builder.stream[String, String](readTopicName)

    val parsedStream: KStream[String, TradeEvent] =
      inputStream.mapValues(value => {
        val parts = value.split(",")
        TradeEvent.fromParts(value.split(","))
      })

    val windowMaterialized
        : Materialized[String, TradeEvent, WindowStore[Bytes, Array[Byte]]] =
      Materialized
        .`as`[String, TradeEvent, WindowStore[Bytes, Array[Byte]]](
          "trade-events-store"
        )
        .withKeySerde(Serdes.String())
        .withValueSerde(new TradeEventSerde())

    val tradeEventStream: KStream[Windowed[String], TradeEvent] =
      parsedStream
        .groupByKey()
        .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(2)))
        .reduce(windowReducer, windowMaterialized)
        .suppress(
          Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded())
        )
        .toStream()

    val emaStream: KStream[String, EMA] =
      tradeEventStream.map[String, EMA](emaMapper)

    val emaOutput: KStream[String, String] = emaStream.mapValues(ema => {
      ema.toString()
    })

    emaOutput.to(
      emaTopicName,
      Produced.`with`(Serdes.String(), Serdes.String())
    )

    val advisoryStream: KStream[String, String] = emaStream
      .map[String, String](advisoryMapper)
      .filter((_, advisory) => advisory != "-")

    advisoryStream.to(
      advisoryTopicName,
      Produced.`with`(Serdes.String(), Serdes.String())
    )

    builder.build()
  }
}
