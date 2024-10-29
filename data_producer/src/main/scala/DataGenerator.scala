import java.io.File
import java.time._
import com.github.tototoshi.csv._

object DataGenerator extends App {

  val path = "/data/debs2022-gc-trading-day-08-11-21.csv"
  val file = new File(path)
  val reader = CSVReader.open(file)

  var row = reader.readNext()

  while (row.isDefined) {
    val values = row.get
    println(values.mkString(","))
    Thread.sleep(500)
    row = reader.readNext()
  }

  reader.close()
}
