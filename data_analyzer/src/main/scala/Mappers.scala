import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.kstream.{KeyValueMapper, Windowed}
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

  var emaValues = Map[String, EMA]()

  val emaMapper
      : KeyValueMapper[Windowed[String], TradeEvent, KeyValue[String, EMA]] =
    new KeyValueMapper[Windowed[String], TradeEvent, KeyValue[String, EMA]] {
      override def apply(
          windowed: Windowed[String],
          tradeEvent: TradeEvent
      ): KeyValue[String, EMA] = {
        val winStart = windowed.window().startTime().toEpochMilli()
        val winEnd = windowed.window().endTime().toEpochMilli()
        val symbol = windowed.key()
        val ema = emaValues.getOrElse(symbol, new EMA())
        emaValues += (symbol -> ema.update(tradeEvent, winStart, winEnd))
        KeyValue.pair(symbol, ema)
      }
    }

}
