import org.apache.kafka.streams.kstream.Reducer
import Models.TradeEvent

object Reducers {

  val windowReducer: Reducer[TradeEvent] = new Reducer[TradeEvent] {
    override def apply(
        lastEvent: TradeEvent,
        newEvent: TradeEvent
    ): TradeEvent = {
      if (newEvent.tradeTime > lastEvent.tradeTime) newEvent else lastEvent
    }
  }

}
