import scala.util.matching.Regex
import net.ruippeixotog.scalascraper.browser.{HtmlUnitBrowser, JsoupBrowser}
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import scala.collection.View.DropRight
import java.util.concurrent.Executors
import scala.concurrent._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
// the following is equivalent to `implicit val ec = ExecutionContext.global`
//import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import scala.util.Random
import scala.language.postfixOps
import scala.collection.parallel.CollectionConverters._
import scala.collection.parallel.immutable._


object MainGuy {
def main(args: Array[String]): Unit = {

implicit val ec = ExecutionContext.global
//implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

val t0: Long = System.nanoTime()
    

val urls: Map[String, String] = Map(
  "dublinrents"->"https://www.daft.ie/property-for-rent/dublin-city/apartments?numBeds_to=2&numBeds_from=1&sort=publishDateDesc&from=%s&pageSize=20", 
  "dublinrents_1rooms"->"https://www.daft.ie/property-for-rent/dublin-city?numBeds_to=1&numBeds_from=1&sort=publishDateDesc&from=%s&pageSize=20", 
  "dublinrents_2rooms"->"https://www.daft.ie/property-for-rent/dublin-city?numBeds_to=2&numBeds_from=2&sort=publishDateDesc&from=%s&pageSize=20", 
  "dublinrents_3rooms"->"https://www.daft.ie/property-for-rent/dublin-city?numBeds_to=3&numBeds_from=3&sort=publishDateDesc&from=%s&pageSize=20", 
  "dublinrents_4rooms"->"https://www.daft.ie/property-for-rent/dublin-city?numBeds_from=4&sort=publishDateDesc&from=%s&pageSize=20"
)

val r: IndexedSeq[Int] = (0 to 10).map(x => x*20) //ToDo: change to 2000 later (100)

val urls2: Map[String, IndexedSeq[String]] = urls.map {case (k, v) => k -> r.map(x => v.format(x))}


val numberPattern: Regex = "€.{1,9}per month".r
val browser = JsoupBrowser()

val url: String = urls2("dublinrents")(0)
  

def processOneUrl(url: String) = Future[List[Int]] { 
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
// Step: Reduce - using foreach instead of onComplete, as I only want to handle successful results anyways
  

// Step: Reduce
val urls4: ParMap[String,ParSeq[Int]] = urls3.par.map {
 case (table, futures) => table -> futures.par.map(one => Await.result(one, 100 seconds)).flatten
}
  
val print_urls: ParMap[String,Int] = urls3.map {case (table, lists) => table -> lists.length}
println(print_urls)
//println(urls4)
println("done")


val t1 = System.nanoTime()
val elapsed = {(t1 - t0)/1000000000}
  
println(elapsed)
}
}