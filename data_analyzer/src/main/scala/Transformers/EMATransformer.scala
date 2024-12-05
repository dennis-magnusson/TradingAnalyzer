package Transformers

import org.apache.kafka.streams.processor.{AbstractProcessor, ProcessorContext}
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.kstream.{
  TransformerSupplier,
  Transformer,
  Windowed
}
import org.apache.kafka.streams.KeyValue

import Models.{TradeEvent, EMA}

class EMATransformer(storeName: String)
    extends Transformer[Windowed[String], TradeEvent, KeyValue[String, EMA]] {
  private var stateStore: KeyValueStore[String, EMA] = _

  override def init(context: ProcessorContext): Unit = {
    stateStore =
      context.getStateStore(storeName).asInstanceOf[KeyValueStore[String, EMA]]
  }

  override def transform(
      key: Windowed[String],
      value: TradeEvent
  ): KeyValue[String, EMA] = {
    val symbol = key.key()
    val ema = Option(stateStore.get(symbol)).getOrElse(new EMA())
    val updatedEma = ema.update(
      value,
      key.window().startTime().toEpochMilli(),
      key.window().endTime().toEpochMilli()
    )
    stateStore.put(symbol, updatedEma)
    KeyValue.pair(symbol, updatedEma)
  }

  override def close(): Unit = {}
}
