package com.gwi.http

import akka.actor.typed.scaladsl.AskPattern.{Askable, _}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes.{Created, NotFound, OK}
import akka.http.scaladsl.server.{Directives, Route}
import akka.util.Timeout
import com.gwi.actors.DatasetActor.DatasetMessage
import com.gwi.actors.DatasetActor.DatasetMessage.GetDataset
import com.gwi.actors.TaskManagerActor.MasterMessage
import com.gwi.actors.TaskManagerActor.MasterMessage._
import com.gwi.domain.ServiceMarshaller.errorJsonFormat
import com.gwi.domain.{Dataset, ErrorMessage, HttpMarshaller, Task, Tasks}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class Routes(taskActor: ActorRef[MasterMessage], datasetActor: ActorRef[DatasetMessage])(implicit val system: ActorSystem[_]) extends Directives with HttpMarshaller {

  implicit val timeout: Timeout = 10.second

  def createTask(task: Task): Future[Task] = {
    taskActor.ask(StartTask(task, _))
  }

  def getTasks: Future[Tasks] = {
    taskActor.ask(GetTasks).mapTo[Tasks]
  }

  def getTask(taskId:String): Future[Option[Task]] = {
    taskActor.ask(GetTask(taskId, _)).mapTo[Option[Task]]
  }

  def stopTask(taskId:String): Future[Task] = {
    taskActor.ask(StopTask(taskId, _)).mapTo[Task]
  }

  def getDataset(result:String): Future[Option[Dataset]] = {
    datasetActor.ask(GetDataset(result, _)).mapTo[Option[Dataset]]
  }


  val service = "csv-parser"
  val version = "v1"

  protected val getTasksRoute: Route = {
    pathPrefix(service / version / "task") {
      get {
        // GET csv-parser/v1/task

        pathEndOrSingleSlash {
          onSuccess(getTasks) { tasks ⇒
            complete(OK, tasks.tasks)
          }
        }
      }
    }
  }

  protected val createTaskRoute: Route = {
    pathPrefix(service / version / "task" ) {
      post {
        //    POST csv-parser/v1/task
        pathEndOrSingleSlash{
          entity(as[Task]) { task =>
            onSuccess(createTask(task)) { task =>
              complete(Created, task)
            }
          }
        }
      }
    }
  }

  protected val getTaskRoute: Route = {
    pathPrefix(service / version / "task" / Segment) { id ⇒
      get {
        // GET csv-parser/v1/task/:task
        pathEndOrSingleSlash {
          onSuccess(getTask(id)) { task =>
            if (task.isDefined)  complete(OK, task)
            else complete(NotFound, ErrorMessage(NotFound.intValue, "Task not found"))
          }
        }
      }
    }
  }

  protected val deleteTaskRoute: Route = {
    pathPrefix(service / version / "task" / Segment) { id ⇒
      delete {
        // GET csv-parser/v1/task/:task
        pathEndOrSingleSlash {
          onSuccess(stopTask(id)) { task =>
            complete(OK, task)
          }
        }
      }
    }
  }

  protected val getDatasetRoute: Route = {
    pathPrefix(service / version / "dataset" / Segment) { id ⇒
      get {
        // GET csv-parser/v1/task/:task
        pathEndOrSingleSlash {
          onSuccess(getDataset(id)) { dataset =>
            if (dataset.isDefined)  complete(OK, dataset)
            else complete(NotFound, ErrorMessage(NotFound.intValue, "Dataset not found"))
          }
        }
      }
    }
  }

  val routes: Route = getTasksRoute ~ createTaskRoute ~ getTaskRoute ~ deleteTaskRoute ~ getDatasetRoute
}
