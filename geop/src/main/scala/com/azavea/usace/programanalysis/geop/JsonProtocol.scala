package com.azavea.usace.programanalysis.geop

import geotrellis.spark.LayerId
import geotrellis.vector.MultiPolygon
import geotrellis.vector.io._
import geotrellis.vector.io.json.GeoJsonSupport

import spray.httpx.SprayJsonSupport
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.collection.parallel.immutable.ParVector


// TODO Nest under "input"
case class CountArgs (
  rasters: Seq[LayerId],
  multiPolygons: ParVector[MultiPolygon]
)

object JsonProtocol extends SprayJsonSupport with GeoJsonSupport {
  implicit object CountArgsJsonFormat extends RootJsonFormat[CountArgs] {
    def write(args: CountArgs) = args match {
      case CountArgs(rasters, multiPolygons) =>
        JsObject(
          "zoom" -> JsNumber(rasters.head.zoom),
          "rasters" -> JsArray(rasters.map(r => JsString(r.name)).toVector),
          "multiPolygons" -> JsArray(multiPolygons.map(m => JsString(m.toGeoJson())).toVector)
        )
      case _ =>
        throw new SerializationException("")
    }

    def read(value: JsValue) = {
      value.asJsObject.getFields("zoom", "rasters", "multiPolygons", "lat", "lng") match {
        case Seq(JsNumber(zoom), JsArray(rasters), JsArray(multiPolygons)) =>
          new CountArgs(
            rasters.map { r => LayerId(r.convertTo[String], zoom.toInt) },
            new ParVector[MultiPolygon](multiPolygons.map { m => m.convertTo[String].parseGeoJson[MultiPolygon] })
          )
        case _ =>
          throw new DeserializationException("Bad Count Arguments")
      }
    }
  }
}
