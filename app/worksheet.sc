import akka.http.scaladsl.model.DateTime

import scala.collection.mutable

val dateTime = "2017-05-09T06:43:15Z"
dateTime.dropRight(1)

val d = DateTime.fromIsoDateTimeString("2017-05-09T06:43:15").get

val q = mutable.Stack[Int]() ++ List(1, 2, 3, 4).sortWith(_ > _)

q.sortWith(_ > _)
q.take(3)
