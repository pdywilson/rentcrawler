import scala.util.matching.Regex
import net.ruippeixotog.scalascraper.browser.{HtmlUnitBrowser, JsoupBrowser}
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import scala.collection.View.DropRight



object MainGuy {
def main(args: Array[String]): Unit = {

val urls = Map(
  "dublinrents"->"https://www.daft.ie/property-for-rent/dublin-city/apartments?numBeds_to=2&numBeds_from=1&sort=publishDateDesc&from=%s&pageSize=20", 
  "dublinrents_1rooms"->"https://www.daft.ie/property-for-rent/dublin-city?numBeds_to=1&numBeds_from=1&sort=publishDateDesc&from=%s&pageSize=20", 
  "dublinrents_2rooms"->"https://www.daft.ie/property-for-rent/dublin-city?numBeds_to=2&numBeds_from=2&sort=publishDateDesc&from=%s&pageSize=20", 
  "dublinrents_3rooms"->"https://www.daft.ie/property-for-rent/dublin-city?numBeds_to=3&numBeds_from=3&sort=publishDateDesc&from=%s&pageSize=20", 
  "dublinrents_4rooms"->"https://www.daft.ie/property-for-rent/dublin-city?numBeds_from=4&sort=publishDateDesc&from=%s&pageSize=20"
)

val r = (0 to 100).map(x => x*20) //ToDo: change to 2000 later (100)

val urls2 = urls.map {case (k, v) => k -> r.map(x => v.format(x))}


//println(urls2)

//println(urls2("dublinrents")(0))

val numberPattern: Regex = "€.{1,9}per month".r
val browser = JsoupBrowser()
// takes 60 sec at 100:
// val doc2 = urls2("dublinrents").map(x => numberPattern.findAllMatchIn(browser.get(x) >> allText).toList.map(d => d.toString.dropRight(10).drop(1).replace(",","").toInt))
// print(doc2.flatten)
// takes 190 sec at 100:
val doc3 = urls2.map {case (k, v) => k -> v.map(x => numberPattern.findAllMatchIn(browser.get(x) >> allText).toList.map(d => d.toString.dropRight(10).drop(1).replace(",","").toInt)).flatten}
print(doc3)



}
}