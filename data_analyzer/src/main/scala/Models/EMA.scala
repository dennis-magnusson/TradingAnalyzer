package Models

class EMA() {

  var latestShortEMA: Double = 0.0
  var latestLongEMA: Double = 0.0

  private var previousShortEMA: Double = 0.0
  private var previousLongEMA: Double = 0.0

  private var lastUpdated: Long = System.currentTimeMillis()

  private var lastUpdateWindowStart: Long = 0
  private var lastUpdateWindowEnd: Long = 0

  private val smoothingFactorShort: Double = 2.0 / (1 + 38)
  private val smoothingFactorLong: Double = 2.0 / (1 + 100)

  def update(lastEvent: TradeEvent, windowStart: Long, windowEnd: Long): EMA = {
    val lastPrice = lastEvent.lastPrice

    lastUpdateWindowStart = windowStart
    lastUpdateWindowEnd = windowEnd

    previousLongEMA = latestLongEMA
    previousShortEMA = latestShortEMA

    latestShortEMA =
      latestShortEMA * (1 - smoothingFactorShort) + lastPrice * smoothingFactorShort
    latestLongEMA =
      latestLongEMA * (1 - smoothingFactorLong) + lastPrice * smoothingFactorLong
    lastUpdated = System.currentTimeMillis()
    this
  }

  def bullishCrossOver(): Boolean = {
    previousShortEMA < previousLongEMA && latestShortEMA > latestLongEMA
  }

  def bearishCrossOver(): Boolean = {
    previousShortEMA > previousLongEMA && latestShortEMA < latestLongEMA
  }

  override def toString(): String = {
    s"$latestShortEMA,$previousShortEMA,$latestLongEMA,$previousLongEMA,$lastUpdated,$lastUpdateWindowStart,$lastUpdateWindowEnd"
  }
}
