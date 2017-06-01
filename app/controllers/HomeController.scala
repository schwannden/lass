package controllers

import javax.inject._

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import controllers.Database.{DBActor, Dataset, Datasets}
import controllers.Database.DBActor.{Update, getFeeds}

import scala.concurrent.duration._
import play.api.mvc._
import play.api.libs.ws._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject() (ws: WSClient) extends Controller {
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

  def index = Action { implicit request =>
    Ok(views.html.index())
  }

  def collector = Action { implicit request =>
    system.scheduler.schedule(0 seconds, 60 seconds, db, Update)
    Ok("as long as we are in this page, we will collect data every 60 seconds")
  }

  def pm(lat: Double, lon: Double, name: String): Action[AnyContent] = Action.async {
    (db ? getFeeds(6)).map {
      _.asInstanceOf[List[Dataset]] match {
        case Nil => {
          Ok("no dataset is selected")
        }
        case datasets => {
          val feeds = datasets.head.feeds.cache
          val avgPM: Double = if (feeds.count < 2) 0 else feeds.map(_.pm).sum / feeds.count
          val pm = mapPredict(lat, lon, datasets.head)
          val pmPredict = forecastPredict(datasets, lat, lon)
          Ok(views.html.pm(lon, lat, name, pm.pm, pmPredict.pm, datasets.head.timeStamp.toString))
        }
        case _ => Ok("illegal response")
      }
    }
  }
}

