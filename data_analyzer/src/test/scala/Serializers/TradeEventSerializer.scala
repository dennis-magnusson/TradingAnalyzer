import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers._
import org.apache.kafka.common.serialization.{Deserializer, Serializer}
import Serializers.{TradeEventSerializer, TradeEventDeserializer}
import Models.TradeEvent

class TradeEventSerializerSpec extends AnyFlatSpec with Matchers {

  "TradeEventSerializer" should "serialize TradeEvent to byte array" in {
    val serializer: Serializer[TradeEvent] = new TradeEventSerializer
    val tradeEvent = TradeEvent("AAPL", "E", 150.0, 100)
    val bytes = serializer.serialize("test-topic", tradeEvent)
    bytes should not be null
  }

  "TradeEventDeserializer" should "deserialize byte array to TradeEvent" in {
    val deserializer: Deserializer[TradeEvent] = new TradeEventDeserializer
    val tradeEvent = TradeEvent("AAPL", "E", 150.0, 100)
    val serializer: Serializer[TradeEvent] = new TradeEventSerializer
    val bytes = serializer.serialize("test-topic", tradeEvent)
    val deserializedTradeEvent = deserializer.deserialize("test-topic", bytes)
    deserializedTradeEvent shouldEqual tradeEvent
  }

}
