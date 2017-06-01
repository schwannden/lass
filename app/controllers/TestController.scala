package controllers

import javax.inject._

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import controllers.Database.{DBActor, Dataset}
import controllers.Database.DBActor.{getFeeds}

import scala.concurrent.duration._
import play.api.mvc._
import play.api.libs.ws._

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class TestController @Inject() (ws: WSClient) extends Controller {
  import util._

  val testCoordinate = List(
    (25.0415370, 121.615852), // Academia Sinica
    (25.0463408, 121.515374), // Taipei Train Station
    (25.0298030, 121.535907)  // Daan Park
  )

  implicit val system = ActorSystem("lass")
  implicit val askTimeout = Timeout(50 seconds)
  val db = system.actorOf(DBActor.props(ws), "database")
  import system.dispatcher

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
            case (lat, lon) => mapPredict(lat, lon, dataset)
          }
          Ok(s"using dataset for time period ${dataset.timeStamp}\n" + predictionResault.mkString("\n"))
        }
        case _ => Ok("illegal response")
      }
    }
  }

  def forecast: Action[AnyContent] = Action.async {
    (db ? getFeeds(6)).map {
      _.asInstanceOf[List[Dataset]] match {
        case Nil => {
          Ok("no dataset is selected")
        }
        case datasets => {
          val predictionResault = testCoordinate.map {
            case (lat, lon) => forecastPredict(datasets, lat, lon)
          }
          Ok(s"using dataset for time period ${datasets.map(_.timeStamp).mkString(", ")}\n" +
            predictionResault.mkString("\n"))
        }
        case _ => Ok("illegal response")
      }
    }
  }
}
