package com.gwi.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import com.gwi.actors.TaskManagerActor.MasterMessage
import com.gwi.actors.TaskManagerActor.MasterMessage.{TaskFailedResponse, TaskFinishedResponse, TaskStartedResponse}
import com.gwi.domain.{Dataset, Task}
import com.gwi.domain.ServiceMarshaller.datasetJsonFormat
import org.slf4j.LoggerFactory

import java.io.{BufferedWriter, File, FileWriter}
import spray.json._

object TaskWorkerActor {

  private val log = LoggerFactory.getLogger(classOf[TaskWorkerActor.type])

  sealed trait WorkerMessage

  object WorkerMessage {
    case object IdentifyYourself extends WorkerMessage
    case class WorkerStartTask(task: Task, actorRef: ActorRef[MasterMessage]) extends WorkerMessage
  }

  val key  : ServiceKey[WorkerMessage] = ServiceKey("Worker")
  import WorkerMessage._
  def apply(id : String) : Behavior[WorkerMessage] = Behaviors.setup { context =>

    context.system.receptionist ! Receptionist.Register(key, context.self)
    Behaviors.receiveMessage {
      case IdentifyYourself =>
        println(s"Hello, I am worker $id")
        Behaviors.same
      case WorkerStartTask(task, ref)=>

        log.debug("Start processing")
        val start = System.currentTimeMillis()
        ref ! TaskStartedResponse(task)

        var csv = s"data/${task.csv}"
        val bufferedSource = io.Source.fromFile(getClass.getClassLoader.getResource(csv).getPath)
        val w = new BufferedWriter(new FileWriter(new File(s"/tmp/${task.id.get}.json")))
        try{
          var data = Vector[Map[String,String]]()

          var count = 0
          var colNumber = 0
          var header = Array[String]()
          for (line <- bufferedSource.getLines) {
            val cols = line.split(",").map(_.trim)
            colNumber = cols.length
            if (count == 0){
              header = line.split(",").map(_.trim)
            }
            else{
              var m = Map[String, String]()
              cols.zipWithIndex.foreach{
                case (elem, idx) =>
                  m = m + (header(idx) -> elem)
              }
              data = data :+ m
            }
            count += 1
          }
          bufferedSource.close

          val result:String = s"dataset/${task.id.get}.json"
          val dataset = Dataset(task.id.get, task.csv, "json" , data)


          w.write(dataset.toJson.prettyPrint)
          val end = System.currentTimeMillis()
          val time = end - start
          val avg = count.toDouble / time.toDouble
          ref ! TaskFinishedResponse(task.id.get, result, count, avg*1000)
          Behaviors.stopped

        }catch{
          case e: Exception => {
            ref ! TaskFailedResponse(task.id.get)
            log.error(s"Task with task id ${task.id.get} finished with an error, ERROR: ${e.getMessage}")
            Behaviors.stopped
          }
        }finally {
          bufferedSource.close()
          w.close()
        }
    }
  }
}