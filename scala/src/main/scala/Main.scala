import scala.util.matching.Regex
import net.ruippeixotog.scalascraper.browser.{HtmlUnitBrowser, JsoupBrowser}
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import scala.collection.View.DropRight
import java.util.concurrent.Executors
import scala.concurrent._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import scala.util.Random
import scala.language.postfixOps
import scala.collection.parallel.CollectionConverters._
import scala.collection.parallel.immutable._


object MainGuy {
def main(args: Array[String]): Unit = {

println("Available Cores: ", Runtime.getRuntime().availableProcessors())

val t0: Long = System.nanoTime()
    
val urls: Map[String, String] = Map(
  //"dublinrents"->"https://www.daft.ie/property-for-rent/dublin-city/apartments?numBeds_to=2&numBeds_from=1&sort=publishDateDesc&from=%s&pageSize=20", 
  "dublinrents_1rooms"->"https://www.daft.ie/property-for-rent/dublin-city?numBeds_to=1&numBeds_from=1&sort=publishDateDesc&from=%s&pageSize=20", 
  "dublinrents_2rooms"->"https://www.daft.ie/property-for-rent/dublin-city?numBeds_to=2&numBeds_from=2&sort=publishDateDesc&from=%s&pageSize=20", 
  "dublinrents_3rooms"->"https://www.daft.ie/property-for-rent/dublin-city?numBeds_to=3&numBeds_from=3&sort=publishDateDesc&from=%s&pageSize=20", 
  "dublinrents_4rooms"->"https://www.daft.ie/property-for-rent/dublin-city?numBeds_from=4&sort=publishDateDesc&from=%s&pageSize=20"
)

def getNumberOfSitesToScrape(url: String): Int = {
      val browser = JsoupBrowser()
      val regex = """[0-9]{0,1},{0,1}[0-9]{1,3}""".r
      val doc = browser.get(url)
      val header_string = (doc >> allText("h1"))
      val num_apartments = regex.findAllIn(header_string).next().slice(0,5).replace(",","").toInt
      val num_sites_to_scrape = math.ceil(num_apartments/20).toInt
      num_sites_to_scrape
    }
    
    val num_sites_to_scrape: Map[String,Int] = urls.map { case (k, v) => k -> getNumberOfSitesToScrape(v.format(0)) }
    println(num_sites_to_scrape)

    val r: Map[String, IndexedSeq[Int]] = num_sites_to_scrape.map { case (k, v) => k -> (0 to v).map(x => x*20) }

    val urls2: Map[String, IndexedSeq[String]] = urls.map { case (k, v) => k -> r(k).map(x => v.format(x)) }
    
    println(s"Scraping %s websites".format(urls2.map { case (k, v) => v.length}.reduce( (a, b) => a + b)))



def processOneUrl(url: String) = Future[List[Int]] { 
  val numberPattern: Regex = "€.{1,9}per month".r
  val browser = JsoupBrowser()
  val one_url = browser.get(url)
  //val output4 = List[Int]()
  
  val output = one_url >> elementList("span")
  val output2 = output.map(_ >> allText("span"))
  val output3 = output2.map(elt => numberPattern.findAllMatchIn(elt)).flatten
  val output4 = output3.map(d => d.toString.dropRight(10).drop(1).replace(",","").toInt)
  output4
 } recover {case _ => List[Int]()}


// Step: Map
val urls3: ParMap[String,ParSeq[Future[List[Int]]]] = urls2.par.map {
  case (table, urls) => table -> urls.par.map(url => processOneUrl(url))
}
// Step: Reduce
val urls4: ParMap[String,ParSeq[Int]] = urls3.par.map {
 case (table, futures) => table -> futures.par.map(one => Await.result(one, 60 seconds)).flatten
}
  
val urls_lengths: ParMap[String,Int] = urls3.map {case (table, lists) => table -> lists.length}
println(urls_lengths)
//println(urls4)

val stats = urls4.map { case (k, v) => k -> Vector(v.sum/v.length, v.seq.sortWith(_ < _).drop(v.length/2).head, v.length) }

println(stats)


val t1 = System.nanoTime()
val elapsed = {(t1 - t0)/1000000000}
  
println(elapsed)
}
}