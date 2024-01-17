package com.gwi.domain

import spray.json.{JsonFormat, NullOptions}
import spray.json.RootJsonFormat
import spray.json.DefaultJsonProtocol
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.gwi.actors.TaskManagerActor.MasterMessage.StartTaskResponse

trait HttpMarshaller extends SprayJsonSupport with DefaultJsonProtocol with NullOptions{
  implicit val taskJsonFormat : RootJsonFormat[Task] = jsonFormat6(Task.apply)
  implicit val tasksJsonFormat : RootJsonFormat[Tasks] = jsonFormat1(Tasks.apply)
  implicit val metaJsonFormat: RootJsonFormat[Meta] = jsonFormat2(Meta.apply)
  implicit val datasetJsonFormat: RootJsonFormat[Dataset] = jsonFormat4(Dataset.apply)
  implicit val startTaskResponseJsonFormat: JsonFormat[StartTaskResponse] = jsonFormat1(StartTaskResponse.apply)
  implicit val datasetFoundJsonFormat: JsonFormat[Dataset] = jsonFormat4(Dataset.apply)
}

object ServiceMarshaller extends SprayJsonSupport with DefaultJsonProtocol{
  implicit val taskJsonFormat : JsonFormat[Task] = jsonFormat6(Task.apply)
  implicit val tasksJsonFormat : RootJsonFormat[Tasks] = jsonFormat1(Tasks.apply)
  implicit val metaJsonFormat: RootJsonFormat[Meta] = jsonFormat2(Meta.apply)
  implicit val datasetJsonFormat: JsonFormat[Dataset] = jsonFormat4(Dataset.apply)
  implicit val errorJsonFormat: RootJsonFormat[ErrorMessage] = jsonFormat2(ErrorMessage.apply)
}
