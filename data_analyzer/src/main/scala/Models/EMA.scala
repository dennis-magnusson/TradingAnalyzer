package Models

class EMA() {

  var latestShortEMA: Double = 0.0
  var latestLongEMA: Double = 0.0
  var previousShortEMA: Double = 0.0
  var previousLongEMA: Double = 0.0

  private val smoothingFactorShort: Double = 2.0 / (1 + 38)
  private val smoothingFactorLong: Double = 2.0 / (1 + 100)

  def update(lastPrice: Double): EMA = {
    previousLongEMA = latestLongEMA
    previousShortEMA = latestShortEMA
    latestShortEMA =
      latestShortEMA * (1 - smoothingFactorShort) + lastPrice * smoothingFactorShort
    latestLongEMA =
      latestLongEMA * (1 - smoothingFactorLong) + lastPrice * smoothingFactorLong
    this
  }

  def bullishCrossOver(): Boolean = {
    previousShortEMA < previousLongEMA && latestShortEMA > latestLongEMA
  }

  def bearishCrossOver(): Boolean = {
    previousShortEMA > previousLongEMA && latestShortEMA < latestLongEMA
  }

  override def toString(): String = {
    s"Short: $latestShortEMA [${previousShortEMA}], Long: $latestLongEMA [$previousLongEMA]"
  }
}
