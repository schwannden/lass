package controllers

import java.io.{File, FileOutputStream, PrintWriter}

import akka.actor.{Actor, Props}
import akka.http.scaladsl.model.DateTime
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import play.api.libs.json.JsValue
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.ExecutionContext
import util._

import scala.collection.mutable
import scala.io.Source


package object Database {
  val dept = Seq("/home/schwannden/Dropbox/schwannden/Documents/computerScience/play/play-scala-seed/target/scala-2.11/lass_2.11-1.0-SNAPSHOT.jar",
    "/home/schwannden/.ivy2/cache/com.typesafe.akka/akka-http-core_2.11/jars/akka-http-core_2.11-10.0.6.jar")

  case class Record(dateTime: DateTime, lon: Double, lat: Double, pm: Double)

  val conf = new SparkConf().setMaster("spark://10.0.0.2:7077").setAppName(s"lass").setJars(dept)
    .set("spark.executor.memory", "16g")
  val sc = SparkContext.getOrCreate(conf)

  type Feeds = RDD[Record]

  class Datasets(val root: String = "data") {
    private val datasets: mutable.Stack[Dataset] = mutable.Stack[Dataset]() ++
      getListOfFiles(root).map(file => Dataset(root, file.getName)).sortWith(_.timeStamp > _.timeStamp)

    def getDatasets = datasets

    def contains(dataset: Dataset): Boolean = {
      datasets contains dataset
    }

    def add(response: WSResponse): Unit = {
      val dataset: Dataset = {
        val dateTime = (response.json \ "version").get.toString.split('"')(1).dropRight(1).split('T')
        Dataset(root, (dateTime(0).split('-') ++ dateTime(1).split(':')).mkString("_"))
      }


      if (contains(dataset)) {
        println(s"dataset [$dataset] already exists")
      } else {
        val writer = new PrintWriter(new FileOutputStream(new File(pathOf(dataset)), false))
        val jsonResults = (response.json \ "feeds").as[List[JsValue]]
        jsonResults.filter(r => {
          (r \ "timestamp").toOption.isDefined &&
            DateTime.fromIsoDateTimeString((r \ "timestamp").as[String].dropRight(1)).isDefined &&
            (r \ "gps_lon").toOption.isDefined &&
            (r \ "gps_lat").toOption.isDefined &&
            (r \ "s_d0").toOption.isDefined
        }).foreach(r => {
          val dateTime = (r \ "timestamp").as[String].dropRight(1)
          val lon = (r \ "gps_lon").as[Double]
          val lat = (r \ "gps_lat").as[Double]
          val pm = (r \ "s_d0").as[Double]
          writer.write(s"$dateTime,$lon,$lat,$pm\n")
        })
        println(s"dataset [$dataset] written")
        datasets.push(dataset)
        writer.close()

      }

    }

    def take(n: Int): List[Dataset] = datasets.take(n).toList

    private def pathOf(dataset: Dataset): String =
      s"$root/${dataset.version}"

  }

  case class Dataset(root: String, version: String) {
    override def toString: String = version


    val timeStamp: DateTime = {
      val v = version.split('_').map(_.toInt)
      DateTime(v(0), v(1), v(2), v(3), v(4), 0)

    }

    lazy val feeds: Feeds =
      sc.textFile(s"$root/$version").map(line => {
        val fields: Array[String] = line.split(',')
        val dateTime = DateTime.fromIsoDateTimeString(fields(0)).get
        Record(dateTime, fields(1).toDouble, fields(2).toDouble, fields(3).toDouble)
      })
  }

  object DBActor {
    sealed trait Message
    case object Update extends Message
    case class getFeeds(n: Int) extends Message

    def props(ws: WSClient) = Props(new DBActor(ws))
  }


  class DBActor(val ws: WSClient) extends Actor {
    import DBActor._
    import context.dispatcher

    private val dataUrl = "https://data.lass-net.org/data/last-all-airbox.json"
    private val root = "/home/schwannden/Dropbox/schwannden/Documents/computerScience/play/play-scala-seed/data"
    private val datasets: Datasets = new Datasets(root)


    override def preStart(): Unit = println("db started")
    override def postStop(): Unit = println("db stopped")

    override def receive: Receive = {

      case Update => {
        ws.url(dataUrl).get().map {
          response => datasets.add(response)
        }
      }

      case getFeeds(n) => {
        sender ! datasets.take(n)
      }
    }
  }
/*  r => {
              val dateTime = DateTime.fromIsoDateTimeString((r \ "timestamp").as[String].dropRight(1)).get
              Record(dateTime, (r \ "gps_lon").as[Double], (r \ "gps_lat").as[Double], (r \ "s_d0").as[Double])
            }*/

}
