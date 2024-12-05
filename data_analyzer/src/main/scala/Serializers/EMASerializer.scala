package Serializers

import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.serialization.{Deserializer, Serdes, Serializer}

import Models.EMA

class EMASerde
    extends Serdes.WrapperSerde[EMA](
      new EMASerializer(),
      new EMADeserializer()
    )

class EMASerializer extends Serializer[EMA] {
  private val objectMapper =
    new ObjectMapper().registerModule(DefaultScalaModule)

  override def serialize(topic: String, data: EMA): Array[Byte] = {
    objectMapper.writeValueAsBytes(data)
  }
}

class EMADeserializer extends Deserializer[EMA] {
  private val objectMapper =
    new ObjectMapper().registerModule(DefaultScalaModule)

  override def deserialize(topic: String, data: Array[Byte]): EMA = {
    objectMapper.readValue(data, classOf[EMA])
  }
}
