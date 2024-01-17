package com.gwi.domain

import scala.collection.immutable.Map

case class Meta(id:String, avgLines:Double)

abstract class Data[T](d:T)
case class FloatData(data:Map[String, Float])
case class DoubleData(data:Map[String, Double]) extends Data
case class StringData(data:Map[String, String]) extends Data

case class Dataset(taskId:String, csvFile: String, jsonFile:String, data:Vector[Map[String,String]])