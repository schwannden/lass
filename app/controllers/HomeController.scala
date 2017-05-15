package controllers

import javax.inject._

import akka.actor.ActorSystem
import akka.http.scaladsl.model.DateTime
import akka.pattern.ask
import akka.util.Timeout
import controllers.Database.{DBActor, Dataset, Datasets}
import controllers.Database.DBActor.{Update, getFeeds}
import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext}

import scala.concurrent.Future
import scala.concurrent.duration._
import play.api.mvc._
import play.api.libs.ws._
import play.api.libs.json.{JsArray, JsValue, Json}

import scala.concurrent.ExecutionContext

trait PMValue {
  val lat: Double
  val lon: Double
  val pm: Double

  override def toString: String = s"lat: $lat, lon: $lon, pm: $pm"
}
case class PMRecord(lat: Double, lon: Double, pm: Double) extends PMValue
case class PMUnknown(lat: Double, lon: Double) extends PMValue {
  val pm = 0.0
  override def toString: String = s"lat: $lat, lon: $lon, pm: unknown"
}

object SparkCommon {
}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject() (ws: WSClient) extends Controller {
  val testCoordinate = List(
    (25.0415370, 121.615852), // Academia Sinica
    (25.0463408, 121.515374), // Taipei Train Station
    (25.0298030, 121.535907)  // Daan Park
  )

  implicit val system = ActorSystem("lass")
  implicit val askTimeout = Timeout(50 seconds)
  val db = system.actorOf(DBActor.props(ws), "database")
  import system.dispatcher

  def index = Action { implicit request =>
    Ok(s"list of datasets:\n" ++ (new Datasets("data")).getDatasets.mkString("\n"))
  }

  def collector = Action { implicit request =>
    system.scheduler.schedule(0 seconds, 60 seconds, db, Update)
    Ok("as long as we are in this page, we will collect data every 60 seconds")
  }

  def map: Action[AnyContent] = Action.async {
    (db ? getFeeds(1)).map {
      _.asInstanceOf[List[Dataset]] match {
        case Nil => {
          Ok("no dataset is selected")
        }
        case List(dataset) => {
          val feeds = dataset.feeds.cache
          val avgPM: Double = if (feeds.count < 2) 0 else feeds.map(_.pm).sum / feeds.count
          val predictionResault = testCoordinate.map {
            case (lat, lon) => {
              if (avgPM == 0) PMUnknown(lat, lon)
              else PMRecord(lat, lon, avgPM)
            }
          }
          Ok(s"using dataset for time period ${dataset.timeStamp}\n" + predictionResault.mkString("\n"))
        }
        case _ => Ok("illegal response")
      }
    }
  }

  def forecast: Action[AnyContent] = Action.async {
    (db ? getFeeds(3)).map {
      _.asInstanceOf[List[Dataset]] match {
        case Nil => {
          Ok("no dataset is selected")
        }
        case datasets: List[Dataset] => {
          val predictionResault = testCoordinate.map {
            case (lat, lon) => PMUnknown(lat, lon)
          }
          val timeStamps = datasets.map(_.timeStamp).mkString(", ")
          Ok(s"using dataset for time period $timeStamps\n" + predictionResault.mkString("\n"))
        }
      }
    }
  }
}
