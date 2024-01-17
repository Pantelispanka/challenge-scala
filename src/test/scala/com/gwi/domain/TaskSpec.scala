package com.gwi.domain

import com.gwi.domain.ServiceMarshaller.taskJsonFormat
import org.scalatest.funsuite.AnyFunSuite
import spray.json._


class TaskSpec extends AnyFunSuite {

  test("Test valid json task"){
    val json = """{"id":"id", "status":"RUNNING", "csv":"a_path", "linesProcessed": 10, "avgLines": 10.2, "result": "result"}""".parseJson
    val task = json.convertTo[Task]
    assert(task.id.get =="id")
    assert(task.status.get == TaskStatus.RUNNING)
    assert(task.csv == "a_path")
  }

  test("Task object to json"){
    val json = "{\"avgLines\":12.12,\"csv\":\"csv\",\"id\":\"id\",\"linesProcessed\":10,\"result\":\"result\",\"status\":\"CANCELED\"}"
    val task = new Task(Some("id"), Some(TaskStatus.CANCELED), "csv", Some(10), Some(12.12), Some("result"))
    assert(task.toJson.toString() == json)
  }

}
