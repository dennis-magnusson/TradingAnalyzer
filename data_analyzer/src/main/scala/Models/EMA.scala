package Models

case class EMA(
    shortEMA: Double,
    longEMA: Double
) {
  def update(
      lastPrice: Double,
      smoothingFactorShort: Double,
      smoothingFactorLong: Double
  ): EMA = {
    val newShortEMA =
      shortEMA * (1 - smoothingFactorShort) + lastPrice * smoothingFactorShort
    val newLongEMA =
      longEMA * (1 - (smoothingFactorLong / 2)) + lastPrice * (smoothingFactorLong / 2)
    EMA(newShortEMA, newLongEMA)
  }
}

object EMA {
  def fromParts(parts: Array[String]): EMA = {
    EMA(parts(0).toDouble, parts(1).toDouble)
  }
}
