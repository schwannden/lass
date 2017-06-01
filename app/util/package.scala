import java.io.File

import controllers.Database.Dataset
import controllers.Database.sc
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.{LabeledPoint, LinearRegressionWithSGD}
import org.apache.spark.rdd.RDD

import scala.math.pow

package object util {

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


  def getListOfFiles(dir: String):List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }

  def forecastPredict(datasets: List[Dataset], lat:Double, lon:Double): PMValue = {
    def pm_map(d: Dataset, lat: Double, lon: Double): Double = {
      //map function here, using fake data now
      return lat + lon + d.timeStamp.hour * 0.1 + d.timeStamp.minute * 0.1 / 60
    }

    def label_data(v: List[Double]): LabeledPoint = {
      return LabeledPoint(v.head, Vectors.dense(v.tail.toArray))
    }

    def create_training(v: List[Double]): RDD[LabeledPoint] = {
      val n = v.size
      val k = 3
      val label = (0 until n - k + 1).map(i => v.slice(i, i + k)).map(x => label_data(x))
      return sc.parallelize(label).cache
    }

    val pm_history = datasets.map(x => mapPredict(lat, lon, x).pm)
    val training = create_training(pm_history)

    val num_iterations = 1000
    val traing_step = 0.001
    val model = LinearRegressionWithSGD.train(training, num_iterations, traing_step)

    val predict_value = label_data(pm_history.head :: pm_history.take(2)).features
    val predict_result = model.predict(predict_value)
    PMRecord(lat, lon, predict_result)
  }

  def getAve(lat_mean: Double, lon_mean: Double, data: RDD[(Double,Double,Double)]): Double={

    val range_lat, range_lon = 0.5 // Set lat range
    val GaussianRange_x = 4 //Set Gaussian x range
    val max_d2 = range_lat * range_lat  + range_lon * range_lon
    val max_d = pow(max_d2, 0.5)
    val all_d = data.map({ case (lat, lon, v) =>
        (pow(lat-lat_mean, 2) + pow(lon-lon_mean, 2), v)
      }).
      filter(_._1 <= max_d2).
      map(x => (pow(x._1, 0.5), x._2))

    if (all_d.count == 0) 0
    else {
      val all_wejght = all_d.map({
        case (d,v) => {
          val u = (d/max_d) * GaussianRange_x
          (math.exp(-u*u/2), v)
        }
      }).cache
      all_wejght.map{case(w,v) => w*v}.sum /
        all_wejght.map{case(w,v) => w}.sum
    }
  }

  def mapPredict(lat: Double, lon: Double, dataset: Dataset): PMValue ={
    val feeds = dataset.feeds.cache
    val Newdata = feeds.map(x=> (x.lat,x.lon,x.pm)).
      filter(x => x._2 < 122 && x._2 > 120 && x._1 > 21.9 && x._1 < 25.3)

    if (Newdata.count < 2) PMUnknown(lat, lon)
    else {
      val GroupData = Newdata.groupBy(x =>(x._1,x._2)).
        mapValues( x => x.map(_._3).sum/x.size).
        map{case((x,y),v)=> (x,y,v)}.cache
      PMRecord(lat, lon, getAve(lat, lon, GroupData))
    }

  }
}
