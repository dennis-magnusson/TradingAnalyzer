package Models

case class WindowResults(
    var lastTradeEvent: TradeEvent,
    var windowStart: Long = 0,
    var windowEnd: Long = 0
)

case class TradeEvent(
    symbol: String,
    secType: String,
    lastPrice: Double,
    tradeTime: Long
)

object TradeEvent {
  def fromParts(parts: Array[String]): TradeEvent = {
    TradeEvent(
      symbol = parts(0),
      secType = parts(1),
      lastPrice = parts(2).toDouble,
      tradeTime = parts(3).toLong
    )
  }
}
