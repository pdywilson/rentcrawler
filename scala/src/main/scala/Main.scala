import scala.util.matching.Regex
import net.ruippeixotog.scalascraper.browser.{HtmlUnitBrowser, JsoupBrowser}
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import scala.collection.View.DropRight
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import scala.util.Random
import scala.language.postfixOps


object MainGuy {
def main(args: Array[String]): Unit = {

val urls = Map(
  "dublinrents"->"https://www.daft.ie/property-for-rent/dublin-city/apartments?numBeds_to=2&numBeds_from=1&sort=publishDateDesc&from=%s&pageSize=20", 
  "dublinrents_1rooms"->"https://www.daft.ie/property-for-rent/dublin-city?numBeds_to=1&numBeds_from=1&sort=publishDateDesc&from=%s&pageSize=20", 
  "dublinrents_2rooms"->"https://www.daft.ie/property-for-rent/dublin-city?numBeds_to=2&numBeds_from=2&sort=publishDateDesc&from=%s&pageSize=20", 
  "dublinrents_3rooms"->"https://www.daft.ie/property-for-rent/dublin-city?numBeds_to=3&numBeds_from=3&sort=publishDateDesc&from=%s&pageSize=20", 
  "dublinrents_4rooms"->"https://www.daft.ie/property-for-rent/dublin-city?numBeds_from=4&sort=publishDateDesc&from=%s&pageSize=20"
)

val r = (0 to 2).map(x => x*20) //ToDo: change to 2000 later (100)

val urls2 = urls.map {case (k, v) => k -> r.map(x => v.format(x))}


//println(urls2)
//println(urls2.getClass())



val numberPattern: Regex = "€.{1,9}per month".r
val browser = JsoupBrowser()

val url = urls2("dublinrents")(0)


// val output = one_url >> elementList("span")
// val output2 = output.map(_ >> allText("span"))
// val output3 = output2.map(elt => numberPattern.findAllMatchIn(elt)).flatten
// val output4 = output3.map(d => d.toString.dropRight(10).drop(1).replace(",","").toInt)
// println(output4)


// def processOneUrl(url: String) = Future { 
//   numberPattern
//   .findAllMatchIn(browser.get(url) >> allText)
//   .toList
//   .map(d => d.toString.dropRight(10).drop(1).replace(",","").toInt)
//  }
def processOneUrl(url: String) = Future { 
  val one_url = browser.get(url)
  val output4 = one_url
  //val output4 = (one_url >> allText).take(0)
  // val output = one_url >> elementList("span")
  // val output2 = output.map(_ >> allText("span"))
  // val output3 = output2.map(elt => numberPattern.findAllMatchIn(elt)).flatten
  // val output4 = output3.map(d => d.toString.dropRight(10).drop(1).replace(",","").toInt)
  output4
 }



// val one = processOneUrl(url)
// val actualResult = Await.result(one, 10 seconds)
// print(actualResult.take(0))

// Step: Map
val urls3 = urls2.map {
  case (table, urls) => table -> urls.map(url => processOneUrl(url))
}
// Step: Reduce
val urls4 = urls3.map {
  case (table, futures) => table -> futures.map(one => Await.result(one, 10 seconds))//.flatten
}
val print_urls = urls4.map {case (table, lists) => table -> lists.length}
print(print_urls)
print("done")

// takes 43 sec at 100 with new implementation

// val result = one.onComplete{
//   case Success(result) => print(result)//result.toList.map(d => d.toString.dropRight(10).drop(1).replace(",","").toInt)).flatten
//   case Failure(e) => e.printStackTrace
// }
// takes 60 sec at 100:
// val doc2 = urls2("dublinrents").map(x => numberPattern.findAllMatchIn(browser.get(x) >> allText).toList.map(d => d.toString.dropRight(10).drop(1).replace(",","").toInt))
// print(doc2.flatten)

// takes 190 sec at 100:
//val doc3 = urls2.map {
 // case (k, v) => k -> v.map(x => numberPattern.findAllMatchIn(browser.get(x) >> allText).
 // toList.map(d => d.toString.dropRight(10).drop(1).replace(",","").toInt)).flatten
  //}
// print(doc3)
//def processOneUrl(url: String): Future[List[String]] = Future {
//  url => numberPattern.findAllMatchIn(browser.get(url) >> allText)
//}
//val doc3 = urls2.map {case (k, v) => k -> v.map(x => processOneUrl(x))}

// val result = doc3.onComplete{
//   case Success(result) => print(result)//result.toList.map(d => d.toString.dropRight(10).drop(1).replace(",","").toInt)).flatten
//   case Failure(e) => e.printStackTrace
// }
// print(result)

  //toList.map(d => d.toString.dropRight(10).drop(1).replace(",","").toInt)).flatten
// print(doc3)

//def sleep(time: Long) { Thread.sleep(time) }

// def longRunningComputation(i: Int): Future[Int] = Future {
//         sleep(100)
//         i + 1
//     }

//     // this does not block
//     longRunningComputation(11).onComplete {
//         case Success(result) => println(s"result = $result")
//         case Failure(e) => e.printStackTrace
//     }

//     // important: keep the jvm from shutting down
//     sleep(1000)

}
}