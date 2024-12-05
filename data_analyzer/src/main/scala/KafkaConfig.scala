import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig

import java.util.Properties

object KafkaConfig {

  def getProperties(kafkaServer: String): Properties = {
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
    props
  }
}
