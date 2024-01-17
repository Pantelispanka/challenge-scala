package com.gwi.actors

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

class TaskWorkerActorSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike  {

  override def afterAll(): Unit = testKit.shutdownTestKit()

  "A Task actor" must {
    val taskWorkerActor = testKit.spawn(TaskWorkerActor.apply("1"), name = "test-worker")

//    "Check start logging" in {
//      val probe = testKit.createTestProbe[MasterMessage]()
//      val task = new Task(Some("Id"), Some(TaskStatus.SCHEDULED), "1.csv", Some(10), Some(12.12), Some("result"))
//      LoggingTestKit.info(messageIncludes = "Start processing").expect {
//        taskWorkerActor ! WorkerStartTask(task, probe.ref)
//      }
//    }

  }

}
