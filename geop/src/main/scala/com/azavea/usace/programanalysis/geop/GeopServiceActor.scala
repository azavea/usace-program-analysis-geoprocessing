package com.azavea.usace.programanalysis.geop

import akka.actor.Actor

import geotrellis.proj4.{ConusAlbers, LatLng}

import org.apache.spark.SparkContext

import scala.concurrent.future

import spray.http.AllOrigins
import spray.http.HttpHeaders.{`Access-Control-Allow-Headers`, `Access-Control-Allow-Methods`, `Access-Control-Allow-Origin`}
import spray.http.HttpMethods.{DELETE, GET, OPTIONS, POST}
import spray.json.{JsNumber, JsObject}
import spray.routing.{Directive0, HttpService, RejectionHandler}


class GeopServiceActor(sc: SparkContext) extends Actor with HttpService {
  import scala.concurrent.ExecutionContext.Implicits.global
  import JsonProtocol._

  implicit val _sc = sc

  def actorRefFactory = context
  def receive = runRoute(root)

  val corsHeaders = List(
    `Access-Control-Allow-Origin`(AllOrigins),
    `Access-Control-Allow-Methods`(GET, POST, OPTIONS, DELETE),
    `Access-Control-Allow-Headers`("Origin, X-Requested-With, Content-Type, Accept, Accept-Encoding, Accept-Language, Host, Referer, User-Agent, Access-Control-Request-Method, Access-Control-Request-Headers")
  )

  def cors: Directive0 = {
    val rh = implicitly[RejectionHandler]
    respondWithHeaders(corsHeaders) & handleRejections(rh)
  }

  def root =
    pathPrefix("count") { rasterGroupedCount } ~
    path("ping") { complete { "OK" } }

  def rasterGroupedCount =
    cors {
      import spray.json.DefaultJsonProtocol._

      entity(as[CountArgs]) { args =>
        complete {
          future {
            val multiPolygon = args.multiPolygon.reproject(LatLng, ConusAlbers)
            val rasterLayers = ClippedLayers(args.rasters, multiPolygon, sc)
            val rasterGroupedCount = RasterGroupedCount(rasterLayers, multiPolygon)

            JsObject(
              rasterGroupedCount
                .map { case (keys, count) =>
                  keys.mkString(",") -> JsNumber(count)
                }
            )
          }
        }
      }
    }
}
