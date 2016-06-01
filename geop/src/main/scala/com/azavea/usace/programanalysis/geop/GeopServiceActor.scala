package com.azavea.usace.programanalysis.geop

import akka.actor.Actor
import org.apache.spark.SparkContext
import spray.routing.HttpService


class GeopServiceActor(sc: SparkContext) extends Actor with HttpService {
  def actorRefFactory = context
  def receive = runRoute(root)

  def root =
    path("ping") { complete { "OK" } }
}
