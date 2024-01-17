package com.gwi.domain

import com.gwi.domain
import spray.json.{DeserializationException, JsString, JsValue, JsonFormat}

import scala.collection.immutable

object TaskStatus extends Enumeration {
  type TaskStatus = Value

  val SCHEDULED: domain.TaskStatus.Value = Value("SCHEDULED")
  val RUNNING: domain.TaskStatus.Value = Value("RUNNING")
  val DONE: domain.TaskStatus.Value = Value("DONE")
  val FAILED: domain.TaskStatus.Value = Value("FAILED")
  val CANCELED: domain.TaskStatus.Value = Value("CANCELED")

  implicit object TaskStatusJsonFormat extends JsonFormat[TaskStatus.TaskStatus] {

    def write(obj: TaskStatus.TaskStatus): JsValue = JsString(obj.toString)

    def read(json: JsValue): TaskStatus.TaskStatus = json match {
      case JsString(str) => TaskStatus.withName(str)
      case _ => throw DeserializationException("Enum string expected")
    }

  }

}

case class Task(id:Option[String],
                status:Option[TaskStatus.TaskStatus],
                csv: String,
                linesProcessed: Option[Int],
                avgLines: Option[BigDecimal],
                result:Option[String])

case class Tasks(tasks: immutable.Seq[Task])