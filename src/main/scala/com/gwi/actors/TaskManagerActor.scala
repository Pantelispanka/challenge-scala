package com.gwi.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.pattern.StatusReply
import akka.pattern.StatusReply.success
import com.gwi.actors.TaskWorkerActor.WorkerMessage
import com.gwi.actors.TaskWorkerActor.WorkerMessage.WorkerStartTask
import com.gwi.domain.{Task, TaskStatus, Tasks}
import org.slf4j.LoggerFactory

import java.util.UUID.randomUUID
import scala.collection.immutable
import scala.collection.immutable.Queue


object TaskManagerActor {

  private val log = LoggerFactory.getLogger(classOf[TaskManagerActor.type])

  sealed trait MasterMessage

  object MasterMessage {
    case class StartWorkers(numWorker: Int) extends MasterMessage
    case class IdentifyWorker(id: Int) extends MasterMessage
    case class StartTask(task: Task, actor:ActorRef[Task]) extends MasterMessage
    case class RunTask(task: Task) extends MasterMessage
    case class StopTask(taskId: String, actor:ActorRef[StatusReply[Task]]) extends MasterMessage
    case class GetTask(taskId: String, actor:ActorRef[Option[Task]]) extends MasterMessage
    case class GetTasks(actor:ActorRef[Tasks]) extends MasterMessage
    case class StartTaskResponse(task: Task) extends MasterMessage
    case class TaskStartedResponse(task:Task) extends MasterMessage
    case class TaskFinishedResponse(task:String, result:String, lines: Int, avg:BigDecimal) extends MasterMessage
    case class TaskFailedResponse(task:String) extends MasterMessage
    case class TaskStatusResponse(task:Task) extends MasterMessage
    case class TaskWorkerResponse(response: WorkerMessage) extends MasterMessage
  }

  private final case class WorkerResponse(response: TaskWorkerActor.WorkerMessage) extends MasterMessage

  private final case class WrappedBackendResponse(response: WorkerMessage) extends MasterMessage

  import MasterMessage._

  final case class TasksStored(tasks: immutable.Seq[Task])


  def taskManagerActor(
                        maxWorkersUp: Int = 2,
                        queue: Queue[Task] = Queue.empty[Task],
                        tasksStored: Map[String, Task] = Map.empty[String, Task],
                        workers: Map[String, ActorRef[TaskWorkerActor.WorkerMessage]] = Map.empty[String, ActorRef[TaskWorkerActor.WorkerMessage]]
                      ): Behavior[MasterMessage] = Behaviors.receive( onMessage = (context, message) => {

    message match {
      case StartWorkers(numWorker) =>
        for (id <- 0 to numWorker) {
          context.spawn(TaskWorkerActor(s"Worker-$id"), s"Worker-$id")
        }

        Behaviors.same

      case StartTask(task, ref) =>
        log.debug(s"Queue size is: ${queue.size}")
        val id = randomUUID().toString
        val taskSceduled = task.copy(id = Some(id), status = Some(TaskStatus.SCHEDULED))
        val ts = tasksStored + (id -> taskSceduled)
        ref ! taskSceduled
        var newQueue = queue.enqueue(taskSceduled)
        var wks = workers
        if (workers.size < maxWorkersUp) {
          var (task, unqueued) = newQueue.dequeue
          newQueue = unqueued
          val taskWorker = context.spawn(TaskWorkerActor(s"Worker-${task.id.get}"), s"Worker-${task.id.get}")
          taskWorker ! WorkerStartTask(taskSceduled, context.self)
          wks = workers + (id -> taskWorker)
        }
        else{
          newQueue = newQueue
        }
        taskManagerActor(maxWorkersUp, newQueue, ts, wks)

      case StopTask(taskId, actor) =>
        var t = tasksStored.get(taskId)
        val taskFinished = t.get.copy(status = Some(TaskStatus.CANCELED))
        var ts = Map.empty[String, Task]
        var queueNew = Queue.empty[Task]
        var wks = Map.empty[String, ActorRef[TaskWorkerActor.WorkerMessage]]

        if(t.get.status.get == TaskStatus.RUNNING){
          ts = tasksStored + (taskFinished.id.get -> taskFinished)
          val w = workers.get(taskId)
          var wks = workers.removed(taskId)
          context.stop(w.get)
          if (queue.nonEmpty) {
            val task = queue.dequeue._1
            queueNew = queue
            if (wks.size < maxWorkersUp) {
              val taskWorker = context.spawn(TaskWorkerActor(s"Worker-${task.id.get}"), s"Worker-${task.id.get}")
              taskWorker ! WorkerStartTask(task, context.self)
              wks = workers + (task.id.get -> taskWorker)
            }
          }
        }
        if(t.get.status.get == TaskStatus.SCHEDULED){
          queue.foreach(t=> {
            if(t.id.get != taskId){
              queueNew = queueNew.enqueue(t)
              queueNew = queueNew.reverse
            }
          })
          wks = workers
          ts = tasksStored + (taskFinished.id.get -> taskFinished)
        }
        log.debug(s"Canceled a task. Queue length now is ${queueNew.size}")
        actor ! success(taskFinished)
        taskManagerActor(maxWorkersUp, queueNew, ts, wks)

      case GetTask(taskId, actor) =>
        val taskFound: Option[Task] = tasksStored.get(taskId)
        actor ! taskFound
        Behaviors.same

      case GetTasks(actor) =>
        var tasksFoundVector = immutable.Seq[Task]()
        tasksStored.values.foreach(t => {
          tasksFoundVector = tasksFoundVector :+ t
        })
        val tasks: Tasks = new Tasks(tasksFoundVector)
        actor ! tasks
        Behaviors.same

      case TaskStartedResponse(task) =>
        val taskRunning = task.copy(status = Some(TaskStatus.RUNNING))
        val ts = tasksStored + (taskRunning.id.get -> taskRunning)
        val nowQueue = queue
        val wks = workers
        taskManagerActor(maxWorkersUp, nowQueue, ts, wks)

      case TaskFailedResponse(taskId) =>
        val taskFound: Option[Task] = tasksStored.get(taskId)
        val taskFailed = taskFound.get.copy(status = Some(TaskStatus.FAILED))
        val ts = tasksStored + (taskFound.get.id.get -> taskFailed)
        val nowQueue = queue
        val wks = workers.removed(taskFound.get.id.get)
        taskManagerActor(maxWorkersUp, nowQueue, ts, wks)

      case TaskFinishedResponse(taskId, result, count, avg) =>
        var t = tasksStored.get(taskId)
        val taskFinished = t.get.copy(status = Some(TaskStatus.DONE)
          , result = Some(result), avgLines = Some(avg), linesProcessed = Some(count))
        val ts = tasksStored + (taskFinished.id.get -> taskFinished)
        var nowQueue = queue
        var wks = workers.removed(taskId)
        val w = workers.get(taskId)

        context.stop(w.get)

        if (queue.nonEmpty) {
          val task = queue.dequeue._1
          nowQueue = queue
          if (wks.size < maxWorkersUp) {
            val taskWorker = context.spawn(TaskWorkerActor(s"Worker-${task.id.get}"), s"Worker-${task.id.get}")
            taskWorker ! WorkerStartTask(task, context.self)
            wks = workers + (task.id.get -> taskWorker)
          }
        }
        taskManagerActor(maxWorkersUp, nowQueue, ts, wks)
    }
  })

}