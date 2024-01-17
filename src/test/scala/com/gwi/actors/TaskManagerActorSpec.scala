package com.gwi.actors

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, ActorTestKitBase, LoggingTestKit, ScalaTestWithActorTestKit}
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.pattern.StatusReply
import com.gwi.actors.TaskManagerActor.MasterMessage.{GetTasks, StartTask, StopTask}
import com.gwi.actors.TaskManagerActor.taskManagerActor
import com.gwi.domain.{Task, TaskStatus, Tasks}
import com.typesafe.config.ConfigFactory
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContext.Implicits.global


class TaskManagerActorSpec  extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  override def afterAll(): Unit = testKit.shutdownTestKit()

  "Task Manager Actor" should {
    val taskActor = testKit.spawn(taskManagerActor(), name = "test-manager")

    "Test variables through logging" in {
      val probe = testKit.createTestProbe[Task]()
      LoggingTestKit.info(messageIncludes = "Queue size is: 0").expect {
        val task = Task(Some("Id"), Some(TaskStatus.SCHEDULED), "1.csv", Some(10), Some(12.12), Some("result"))
        taskActor ! StartTask(task, probe.ref)
      }
    }


    "Create task and return scheduled task" in {
      val task = Task(Some("id"), Some(TaskStatus.CANCELED), "1.csv", Some(10), Some(12.12), Some("result"))
      val t = taskActor.ask(StartTask(task, _)).mapTo[Task]
      t.onComplete(t=> {
        assert(t.get.status.get == TaskStatus.SCHEDULED)
      })
    }

    "Create task and return task" in {
      val task = Task(Some("id"), Some(TaskStatus.CANCELED), "1.csv", Some(10), Some(12.12), Some("result"))
      taskActor.ask(StartTask(task, _))
      val task1 = Task(Some("id"), Some(TaskStatus.CANCELED), "1.csv", Some(10), Some(12.12), Some("result"))
      taskActor.ask(StartTask(task1, _))
      val task2 = Task(Some("id"), Some(TaskStatus.CANCELED), "2.csv", Some(10), Some(12.12), Some("result"))
      taskActor.ask(StartTask(task2, _))
      val task3 = Task(Some("id"), Some(TaskStatus.CANCELED), "2.csv", Some(10), Some(12.12), Some("result"))
      taskActor.ask(StartTask(task3, _))

      val tasks = taskActor.ask(GetTasks)
      tasks.onComplete(t => {
        assert(t.get.tasks.size == 6)
      })

    }


    "Check queue size" in {
      val probe = testKit.createTestProbe[Task]()
      LoggingTestKit.info(messageIncludes = "Queue size is: 4").expect {
        val task = Task(Some("Id"), Some(TaskStatus.SCHEDULED), "1.csv", Some(10), Some(12.12), Some("result"))
        taskActor ! StartTask(task, probe.ref)
      }
    }

    "Stop a task" in {

      // 7 total tasks, Two running and 1 canceled. So in queue should be 4
      // Also the total running tasks is tested
      val probe = testKit.createTestProbe[StatusReply[Task]]()

      var tasks = taskActor.ask(GetTasks)

      tasks.onComplete(t =>{

        var totalRunning = 0
        t.get.tasks.foreach(t => {
          if(t.status.get == TaskStatus.RUNNING){
            totalRunning += 1
          }
        })
        assert(totalRunning == 2)
        var task = t.get.tasks.last

        LoggingTestKit.info(messageIncludes = "Canceled a task. Queue length now is 4").expect{
          taskActor ! StopTask(task.id.get, probe.ref)
        }

      })
    }

  }

}



