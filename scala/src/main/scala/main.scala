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
      val headerString = (doc >> allText("h1"))
      val numberOfApartments = regex.findAllIn(headerString).next().slice(0,5).replace(",","").toInt
      val numberOfSitesToScrape = math.ceil(numberOfApartments/20).toInt
      numberOfSitesToScrape
    }


  /**
   * For given url retrieve all Rent prices on that page by matching the Regex.
   */
    def processOneUrl(url: String) = Future[List[Int]] {
      val numberPattern: Regex = "€.{1,9}per month".r
      val browser = JsoupBrowser()
      val oneUrl = browser.get(url)
      val candidates = oneUrl >> elementList("span")
      val candidatesAsString = candidates.map(_ >> allText("span"))
      val rentList = 
        candidatesAsString
          .map(elt => numberPattern.findAllMatchIn(elt))
          .flatten
          .map(d => d.toString.dropRight(10).drop(1).replace(",","").toInt)
      rentList
     } recover {case _ => List[Int]()}


  /**
   * Updates SQL Database with timestamp, avg, median, number_of_properties.
   */
    def writeToDB(table: String, stats: Vector[Int])(implicit session: scalikejdbc.AutoSession.type) = {
      val currentTimestamp = new java.sql.Timestamp(System.currentTimeMillis())
      val tableName = SQLSyntax.createUnsafely(table)
      sql"""insert into ${tableName} values(${currentTimestamp},${stats(0)},${stats(1)},${stats(2)})""".execute.apply()
    }

  /**
   * Get latest data from database (timestamp, avg, median, number_of_properties).
   */
    def selectFromDB(table: String)(implicit session: scalikejdbc.AutoSession.type) = {
      val tableName = SQLSyntax.createUnsafely(table)
      val latestData = sql"""select * from ${tableName} order by timestamp desc limit 1""".map(_.toMap).list.apply()
      latestData(0)
    }

  /**
   * Updates the html file used for the website.
   *
   * First queries the database for latest results.
   * Then writes a html-String to the file.
   */
  def updateWebsite()(implicit session: scalikejdbc.AutoSession.type) = {
    val stats1 = selectFromDB("dublinrents_1rooms")
    val stats2 = selectFromDB("dublinrents_2rooms")
    val stats3 = selectFromDB("dublinrents_3rooms")
    val stats4 = selectFromDB("dublinrents_4rooms")

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
            <p>(Based on ${stats4("number_of_properties")} 4+-bedroom Dublin-City properties)</p>
            </div>
            <div>
            <p>Last updated: ${stats1("timestamp")}</p>
            </div>

        </body>
        </html>"""

    val websitePath = "/home/pdywilson/rentmanhost/public/index.html"

    val pw = new PrintWriter(new File(websitePath))
    pw.write(website)
    pw.close()
  }


  def main(args: Array[String]): Unit = {

    // 1. Crawl rents

    val urls: Map[String, String] = Map(
      //"dublinrents"->"", 
      //"dublinrents_1rooms"->"", 
      //"dublinrents_2rooms"->"", 
      //"dublinrents_3rooms"->"", 
      //"dublinrents_4rooms"->""
    )

    val numberOfSitesToScrape: Map[String,Int] = 
      urls.map { case (k, v) => k -> getNumberOfSitesToScrape(v.format(0)) }

    // Populate Url blueprints using helperMap and numberOfSitesToScrape
    val helperMap: Map[String, IndexedSeq[Int]] = 
      numberOfSitesToScrape.map { case (k, v) => k -> (0 to v).map(x => x*20) }

    val urlsPopulated: Map[String, IndexedSeq[String]] = 
      urls.map { case (k, v) => k -> helperMap(k).map(x => v.format(x)) }

    val urlLengths: Map[String,Int] = urlsPopulated.map {case (table, lists) => table -> lists.length}
    println("Processing: "+urlLengths)

    // Map Step. populated Urls to the process each Url asynchronously
    val urlsProcessing: ParMap[String,ParSeq[Future[List[Int]]]] = urlsPopulated.par.map {
      case (table, urls) => table -> urls.par.map(url => processOneUrl(url))
    }

    // Reduce Step. Collect all processed Urls.
    val urlsProcessed: ParMap[String,ParSeq[Int]] = urlsProcessing.par.map {
      case (table, futures) => table -> futures.par.map(one => Await.result(one, 60 seconds)).flatten
    }
  
    val stats: ParMap[String,Vector[Int]] = urlsProcessed.map { 
      case (k, v) => k -> Vector(v.sum/v.length, v.seq.sortWith(_ < _).drop(v.length/2).head, v.length) 
    }
    println("Results: "+stats)

    // 2. Write to SQL

    // ad-hoc session provider on the REPL
    implicit val session = AutoSession
    Class.forName("org.postgresql.Driver")
    val config = scala.io.Source.fromFile(".config").getLines
    ConnectionPool.singleton(config.next().toString, config.next().toString, config.next().toString)

    stats.map { case (k, v) => k -> writeToDB(k, v)(session) }


    // 3. Update Website
    updateWebsite()(session)

  }
}
