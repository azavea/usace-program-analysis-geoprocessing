package com.azavea.usace.programanalysis.geop

import akka.actor.Actor

import org.apache.spark.SparkContext

import scala.concurrent.future

import spray.http.{ AllOrigins, MediaTypes }
import spray.http.HttpHeaders.{`Access-Control-Allow-Headers`, `Access-Control-Allow-Methods`, `Access-Control-Allow-Origin`}
import spray.http.HttpMethods.{DELETE, GET, OPTIONS, POST}
import spray.json.{JsNumber, JsObject}
import spray.routing.{Directive0, HttpService, RejectionHandler}

import geotrellis.raster.Tile
import geotrellis.spark.io.{TileNotFoundError}
import geotrellis.proj4.{LatLng, ConusAlbers}
import geotrellis.raster.{IntConstantNoDataCellType}
import geotrellis.raster.render.{IntColorMap}


class GeopServiceActor(sc: SparkContext) extends Actor with HttpService {
  import scala.concurrent.ExecutionContext.Implicits.global
  import JsonProtocol._

  implicit val _sc = sc

  val NLCD_FOREST_COLOR: Int = 0x2B7B3D80
  val NLCD_WETLANDS_COLOR: Int = 0x75A5D080
  val NLCD_DISTURBED_COLOR: Int = 0xFF5D5D80

  val nlcdColorMap =
    new IntColorMap(Map(
      41 -> NLCD_FOREST_COLOR, 42 -> NLCD_FOREST_COLOR, 43 -> NLCD_FOREST_COLOR,
      90 -> NLCD_WETLANDS_COLOR, 95 -> NLCD_WETLANDS_COLOR,
      21 -> NLCD_DISTURBED_COLOR, 22 -> NLCD_DISTURBED_COLOR, 23 -> NLCD_DISTURBED_COLOR,
      24 -> NLCD_DISTURBED_COLOR, 81 -> NLCD_DISTURBED_COLOR, 82 -> NLCD_DISTURBED_COLOR
    ))

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
    pathPrefix("nlcd-agg-tiles") { tilesHandler } ~
    path("ping") { complete { "OK" } }

  def rasterGroupedCount =
    cors {
      import spray.json.DefaultJsonProtocol._

      entity(as[CountArgs]) { args =>
        complete {
          future {
            args.multiPolygons.map(m => {
                val multiPolygon = m.reproject(LatLng, ConusAlbers)
                val rasterLayers = ClippedLayers(args.rasters, multiPolygon, sc)
                val rasterGroupedCount = RasterGroupedCount(rasterLayers, multiPolygon)

                JsObject(
                  rasterGroupedCount
                    .map { case (keys, count) =>
                      keys.mkString(",") -> JsNumber(count)
                    })
            }).toVector
          }
        }
      }
    }

  def tilesHandler =
    pathPrefix(IntNumber / IntNumber / IntNumber) { (zoom, x, y) =>
      respondWithMediaType(MediaTypes.`image/png`) {
        complete {
          future {
            val result: Option[Tile] =
              try {
                Some(ClippedLayers(zoom, x, y, sc))
              } catch {
                case _: TileNotFoundError => None
              }

            result.map { tile =>
              tile
                .convert(IntConstantNoDataCellType)
                .renderPng(nlcdColorMap)
                .bytes
            }
          }
        }
      }
    }
}
