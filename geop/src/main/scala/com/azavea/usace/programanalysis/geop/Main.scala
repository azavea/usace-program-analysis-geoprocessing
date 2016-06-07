package com.azavea.usace.programanalysis.geop

import akka.actor.Props
import akka.io.IO

import org.apache.spark.{SparkConf, SparkContext}

import spray.can.Http


object Main {
  def main(args: Array[String]): Unit = {
    implicit val system = akka.actor.ActorSystem("usace-programanalysis-geop")

    val conf =
      new SparkConf()
        .setAppName("USACE Program Analysis Geoprocessing")
        .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
        .set("spark.kryo.registrator", "geotrellis.spark.io.kryo.KryoRegistrator")

    val sc = new SparkContext(conf)

    // Create and start Service Actor
    val service =
      system.actorOf(Props(classOf[GeopServiceActor], sc), "usace-programanalysis")

    // Start HTTP Server on 8090 with Service Actor as the handler
    IO(Http) ! Http.Bind(service, "0.0.0.0", 8090)
  }
}
