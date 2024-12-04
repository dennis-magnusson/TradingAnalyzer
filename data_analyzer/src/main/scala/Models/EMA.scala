package Models

case class EMA(
    shortEMA: Double,
    longEMA: Double
) {
  def update(lastPrice: Double, smoothingFactor: Double): EMA = {
    val newShortEMA =
      shortEMA * (1 - smoothingFactor) + lastPrice * smoothingFactor
    val newLongEMA =
      longEMA * (1 - (smoothingFactor / 2)) + lastPrice * (smoothingFactor / 2)
    EMA(newShortEMA, newLongEMA)
  }
}

object EMA {
  def init: EMA = EMA(0.0, 0.0) // Initial EMA values set to 0
}
