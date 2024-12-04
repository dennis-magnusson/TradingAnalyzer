import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.serialization.{Deserializer, Serializer}
import Models.TradeEvent

class TradeEventSerializer extends Serializer[TradeEvent] {
  private val objectMapper =
    new ObjectMapper().registerModule(DefaultScalaModule)

  override def serialize(topic: String, data: TradeEvent): Array[Byte] = {
    objectMapper.writeValueAsBytes(data)
  }
}

class TradeEventDeserializer extends Deserializer[TradeEvent] {
  private val objectMapper =
    new ObjectMapper().registerModule(DefaultScalaModule)

  override def deserialize(topic: String, data: Array[Byte]): TradeEvent = {
    objectMapper.readValue(data, classOf[TradeEvent])
  }
}
