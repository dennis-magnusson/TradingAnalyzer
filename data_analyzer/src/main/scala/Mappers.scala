import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.kstream.{KeyValueMapper, Windowed}
import org.apache.kafka.streams.state.KeyValueStore
import Models.{TradeEvent, EMA}

object Mappers {

  val advisoryMapper: KeyValueMapper[String, EMA, KeyValue[String, String]] =
    new KeyValueMapper[String, EMA, KeyValue[String, String]] {
      override def apply(symbol: String, ema: EMA): KeyValue[String, String] = {
        if (ema.bullishCrossOver) KeyValue.pair(symbol, "BUY")
        else if (ema.bearishCrossOver) KeyValue.pair(symbol, "SELL")
        else KeyValue.pair(symbol, "-")
      }
    }
}
