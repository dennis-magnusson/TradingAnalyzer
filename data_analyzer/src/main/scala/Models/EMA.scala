package Models

case class EMA(
    var shortEMA: Double,
    var longEMA: Double
) {

  def update(
      lastPrice: Double,
      smoothingFactorShort: Double,
      smoothingFactorLong: Double
  ): EMA = {
    shortEMA =
      shortEMA * (1 - smoothingFactorShort) + lastPrice * smoothingFactorShort
    longEMA =
      longEMA * (1 - (smoothingFactorLong / 2)) + lastPrice * (smoothingFactorLong / 2)
    this
  }
}
