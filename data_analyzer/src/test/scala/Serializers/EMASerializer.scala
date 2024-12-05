import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers._
import org.apache.kafka.common.serialization.{Deserializer, Serializer}
import Serializers.{EMASerializer, EMADeserializer}
import Models.EMA

class EMASerializerSpec extends AnyFlatSpec with Matchers {

  "EMASerializer" should "serialize EMA to byte array" in {
    val serializer: Serializer[EMA] = new EMASerializer
    val tradeEvent = EMA(0.2, 0.1)
    val bytes = serializer.serialize("test-topic", tradeEvent)
    bytes should not be null
  }

  "EMADeserializer" should "deserialize byte array to EMA" in {
    val deserializer: Deserializer[EMA] = new EMADeserializer
    val tradeEvent = EMA(0.2, 0.1)
    val serializer: Serializer[EMA] = new EMASerializer
    val bytes = serializer.serialize("test-topic", tradeEvent)
    val deserializedEMA = deserializer.deserialize("test-topic", bytes)
    deserializedEMA shouldEqual tradeEvent
  }

}
