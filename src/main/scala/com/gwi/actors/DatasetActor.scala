package com.gwi.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.gwi.domain.ServiceMarshaller.datasetJsonFormat
import com.gwi.domain.Dataset
import spray.json.JsonParser


object DatasetActor {

  sealed trait DatasetMessage

  object DatasetMessage {
    case class GetDataset(result:String, actorRef:ActorRef[Option[Dataset]]) extends DatasetMessage
  }

  import DatasetMessage._
  def datasetActor() : Behavior[DatasetMessage] = Behaviors.receive( onMessage = (context, message) =>

    message match {
      case GetDataset(result, ref) =>
        var input_file = s"/tmp/${result}.json"

        try{
          val source = io.Source.fromFile(input_file)
          val contents = source.mkString
          val j = JsonParser(contents)
          val dataset = j.convertTo[Dataset]
          source.close()
          ref ! Option(dataset)
          Behaviors.same
        }

        catch{
          case e: Exception => {
            ref ! Option(null)
            Behaviors.same
          }
        }

        Behaviors.same
      }
  )

}

