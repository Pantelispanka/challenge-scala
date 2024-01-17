package com.gwi.http

import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.server.Directives.{complete, pathEnd, pathPrefix}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.gwi.actors.DatasetActor.datasetActor
import com.gwi.actors.TaskManagerActor.taskManagerActor
import com.gwi.domain.ErrorMessage
import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec



// Not yet valid tests here
class RoutesSpec extends AnyWordSpec with Matchers with ScalatestRouteTest{


//  "The service" should {
//
//    "Test error response as an example" in {
//      // tests:
//      Get("/csv-parser/v1/dataset/1") ~> testRoute ~> check {
//        responseAs[String] shouldEqual  """{"status":404, "error":"Dataset not found"}"""
//      }
//    }


  }

}
