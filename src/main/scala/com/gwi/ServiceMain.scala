package com.gwi

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.{Route, ExceptionHandler => _}
import com.gwi.actors.DatasetActor.datasetActor
import com.gwi.actors.TaskManagerActor
import com.gwi.actors.TaskManagerActor.taskManagerActor
import com.gwi.domain.Task
import com.gwi.http.Routes
import com.typesafe.config.ConfigFactory

import scala.collection.immutable.Queue
import scala.util.{Failure, Success}


//object ServiceMain extends App {
//
//  val config = ConfigFactory.load("gwiapp.conf")
//  val host = config.getString("host")
//  val port = config.getInt("port")
//
////  implicit val system: ActorSystem = ActorSystem()
////  implicit val ec: ExecutionContextExecutor = system.dispatcher
//
//  val t = config.getString("timeout")
//  val d = Duration(t)
//  val timeout = FiniteDuration(d.length, d.unit)
//
//  val api = new TaskRoutes(system, timeout).routes // the RestApi provides a Route
//
//  val bindingFuture: Future[ServerBinding] = Http().bindAndHandle(api, host, port) // starts the HTTP server
//
//}


object ServiceMain{

  private def startHttpServer(routes: Route, host:String, port: Int)(implicit system: ActorSystem[_]): Unit = {
    import system.executionContext


    val futureBinding = Http().newServerAt(host, port).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
      case Failure(ex) =>
        system.terminate()
    }
  }

  def main(args: Array[String]): Unit = {

    val rootBehavior = Behaviors.setup[Nothing] { context =>

      val config = ConfigFactory.load("gwiapp.conf")
      val host = config.getString("host")
      val port = config.getInt("port")
      val maxWorkers = config.getInt("maxWorkersUp")

      val taskActor = context.spawn(taskManagerActor(maxWorkersUp=maxWorkers), "TaskActor")
      context.watch(taskActor)

      val actor = context.spawn(datasetActor(), name = "DatasetActor")
      context.watch(actor)
      val routes = new Routes(taskActor, actor)(context.system)


      startHttpServer(routes.routes, host, port)(context.system)

      Behaviors.empty
    }
    ActorSystem[Nothing](rootBehavior, "AkkaHttpServer")
  }
}