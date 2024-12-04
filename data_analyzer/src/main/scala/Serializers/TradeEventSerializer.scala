package Serializers

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.serialization.{Deserializer, Serdes, Serializer}
import Models.TradeEvent

class TradeEventSerde
    extends Serdes.WrapperSerde[TradeEvent](
      new TradeEventSerializer(),
      new TradeEventDeserializer()
    )

class TradeEventSerializer extends Serializer[TradeEvent] {
  private val objectMapper = new ObjectMapper()

  override def serialize(topic: String, data: TradeEvent): Array[Byte] = {
    objectMapper.writeValueAsBytes(data)
  }
}

class TradeEventDeserializer extends Deserializer[TradeEvent] {
  private val objectMapper = new ObjectMapper()

  override def deserialize(topic: String, data: Array[Byte]): TradeEvent = {
    objectMapper.readValue(data, classOf[TradeEvent])
  }
}
