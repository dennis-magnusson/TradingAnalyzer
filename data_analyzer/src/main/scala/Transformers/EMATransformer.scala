package Transformers

import org.apache.kafka.streams.processor.{
  AbstractProcessor,
  ProcessorContext,
  Punctuator,
  PunctuationType,
  Cancellable
}
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.kstream.{
  TransformerSupplier,
  Transformer,
  Windowed
}
import org.apache.kafka.streams.KeyValue
import java.time.Duration

import Models.{TradeEvent, EMA}

class EMATransformer(storeName: String)
    extends Transformer[Windowed[String], TradeEvent, KeyValue[String, EMA]] {
  private var stateStore: KeyValueStore[String, EMA] = _
  // private var punctuator: Cancellable = _

  override def init(context: ProcessorContext): Unit = {
    stateStore =
      context.getStateStore(storeName).asInstanceOf[KeyValueStore[String, EMA]]
    // punctuator = context.schedule(
    //   Duration.ofMinutes(1),
    //   PunctuationType.WALL_CLOCK_TIME,
    //   new Punctuator {
    //     override def punctuate(timestamp: Long): Unit = {
    //       val iterator = stateStore.all()
    //       while (iterator.hasNext()) {
    //         val entry = iterator.next()
    //         val symbol = entry.key
    //         val ema: EMA = entry.value
    //         if (timestamp > ema.lastUpdateWindowEnd) {
    //           context.forward(symbol, ema)
    //         }
    //       }
    //     }
    //   }
    // )
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
