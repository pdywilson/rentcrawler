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

import scalikejdbc._
import java.util.{Calendar}
import java.text.SimpleDateFormat

import java.io._

object MainGuy {

  /**
   * Calculates how many websites have to be scraped to get all properties for given url.
   */
    def getNumberOfSitesToScrape(url: String): Int = {
      val browser = JsoupBrowser()
      val regex = """[0-9]{0,1},{0,1}[0-9]{1,3}""".r
      val doc = browser.get(url)
      val header_string = (doc >> allText("h1"))
      val num_apartments = regex.findAllIn(header_string).next().slice(0,5).replace(",","").toInt
      val num_sites_to_scrape = math.ceil(num_apartments/20).toInt
      num_sites_to_scrape
    }


  /**
   * For given url retrieve all Rent prices on that page by matching the Regex.
   */
    def processOneUrl(url: String) = Future[List[Int]] {
      val numberPattern: Regex = "€.{1,9}per month".r
      val browser = JsoupBrowser()
      val one_url = browser.get(url)
      val output = one_url >> elementList("span")
      val output2 = output.map(_ >> allText("span"))
      val output3 = output2.map(elt => numberPattern.findAllMatchIn(elt)).flatten
      val output4 = output3.map(d => d.toString.dropRight(10).drop(1).replace(",","").toInt)
      output4
     } recover {case _ => List[Int]()}


  /**
   * Updates SQL Database with timestamp, avg, median, number_of_properties.
   */
    def insert_to_db(table: String, stats: Vector[Int])(implicit session: scalikejdbc.AutoSession.type) = {
      val t = new java.sql.Timestamp(System.currentTimeMillis())
      val tableName = SQLSyntax.createUnsafely(table)
      sql"""insert into ${tableName} values(${t},${stats(0)},${stats(1)},${stats(2)})""".execute.apply()
    }

  /**
   * Get latest data from database (timestamp, avg, median, number_of_properties).
   */
    def select_from_db(table: String)(implicit session: scalikejdbc.AutoSession.type) = {
      val tableName = SQLSyntax.createUnsafely(table)
      val sql_out = sql"""select * from ${tableName} order by timestamp desc limit 1""".map(_.toMap).list.apply()
      sql_out(0)
    }

  /**
   * Updates the html file used for the website.
   *
   * First queries the database for latest results.
   * Then writes a html-String to the file.
   */
  def update_website()(implicit session: scalikejdbc.AutoSession.type) = {
    val stats1 = select_from_db("dublinrents_1rooms")
    val stats2 = select_from_db("dublinrents_2rooms")
    val stats3 = select_from_db("dublinrents_3rooms")
    val stats4 = select_from_db("dublinrents_4rooms")

    val website = s"""<!DOCTYPE html>
        <html>
        <style>
        .content {
        max-width: 500px;
        margin: auto;
        text-align: center;
        }
        </style>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>Dublin Rents</title>
        </head>
        <body>
        <div class="content">
            <div>
            <h2>Average Monthly Rent in Dublin</h2>
            <h1>Rents are on 🔥</h1>
            </div>
            <div>
            <h2>1-bedroom</h2>
            <p>The average rent is: €${stats1("avg")}</p>
            <p>The median rent is: €${stats1("median")}</p>
            <p>(Based on ${stats1("number_of_properties")} 1-bedroom Dublin-City properties)</p>
            </div>
            <div>
            <h2>2-bedroom</h2>
            <p>The average rent is: €${stats2("avg")}</p>
            <p>The median rent is: €${stats2("median")}</p>
            <p>(Based on ${stats2("number_of_properties")} 2-bedroom Dublin-City properties)</p>
            </div>
            <div>
            <h2>3-bedroom</h2>
            <p>The average rent is: €${stats3("avg")}</p>
            <p>The median rent is: €${stats3("median")}</p>
            <p>(Based on ${stats3("number_of_properties")} 3-bedroom Dublin-City properties)</p>
            </div>
            <div>
            <h2>4-bedroom</h2>
            <p>The average rent is: €${stats4("avg")}</p>
            <p>The median rent is: €${stats4("median")}</p>
            <p>(Based on ${stats4("number_of_properties")} 4-bedroom Dublin-City properties)</p>
            </div>
            <div>
            <p>Last updated: ${stats1("timestamp")}</p>
            </div>

        </body>
        </html>"""

    val website_path = "/home/pdywilson/rentmanhost/public/index.html"

    val pw = new PrintWriter(new File(website_path))
    pw.write(website)
    pw.close()
  }


  def main(args: Array[String]): Unit = {

    // 1. Crawl rents

    val urls: Map[String, String] = Map(
      //"dublinrents"->"https://www.daft.ie/property-for-rent/dublin-city/apartments?numBeds_to=2&numBeds_from=1&sort=publishDateDesc&from=%s&pageSize=20", 
      "dublinrents_1rooms"->"https://www.daft.ie/property-for-rent/dublin-city?numBeds_to=1&numBeds_from=1&sort=publishDateDesc&from=%s&pageSize=20", 
      "dublinrents_2rooms"->"https://www.daft.ie/property-for-rent/dublin-city?numBeds_to=2&numBeds_from=2&sort=publishDateDesc&from=%s&pageSize=20", 
      "dublinrents_3rooms"->"https://www.daft.ie/property-for-rent/dublin-city?numBeds_to=3&numBeds_from=3&sort=publishDateDesc&from=%s&pageSize=20", 
      "dublinrents_4rooms"->"https://www.daft.ie/property-for-rent/dublin-city?numBeds_from=4&sort=publishDateDesc&from=%s&pageSize=20"
    )

    val num_sites_to_scrape: Map[String,Int] = urls.map { case (k, v) => k -> getNumberOfSitesToScrape(v.format(0)) }
    println(num_sites_to_scrape)

    val r: Map[String, IndexedSeq[Int]] = num_sites_to_scrape.map { case (k, v) => k -> (0 to v).map(x => x*20) }

    val urls2: Map[String, IndexedSeq[String]] = urls.map { case (k, v) => k -> r(k).map(x => v.format(x)) }
    
    println(s"Scraping %s websites".format(urls2.map { case (k, v) => v.length}.reduce( (a, b) => a + b)))

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

    val stats: ParMap[String,Vector[Int]] = urls4.map { 
      case (k, v) => k -> Vector(v.sum/v.length, v.seq.sortWith(_ < _).drop(v.length/2).head, v.length) 
    }


    // 2. Write to SQL

    // ad-hoc session provider on the REPL
    implicit val session = AutoSession
    Class.forName("org.postgresql.Driver")
    val config = scala.io.Source.fromFile(".config").getLines
    ConnectionPool.singleton(config.next().toString, config.next().toString, config.next().toString)

    stats.map { case (k, v) => k -> insert_to_db(k, v)(session) }


    // 3. Update Website
    update_website()(session)

  }
}
