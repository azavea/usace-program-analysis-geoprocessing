package com.azavea.usace.programanalysis.geop

import geotrellis.spark.LayerId
import geotrellis.vector.MultiPolygon
import geotrellis.vector.io._
import geotrellis.vector.io.json.GeoJsonSupport

import spray.httpx.SprayJsonSupport
import spray.json._
import spray.json.DefaultJsonProtocol._


// TODO Nest under "input"
case class CountArgs (rasters: Seq[LayerId], multiPolygons: Seq[MultiPolygon])

object JsonProtocol extends SprayJsonSupport with GeoJsonSupport {
  implicit object CountArgsJsonFormat extends RootJsonFormat[CountArgs] {
    def write(args: CountArgs) = JsObject(
      "zoom" -> JsNumber(args.rasters.head.zoom),
      "rasters" -> JsArray(args.rasters.map(r => JsString(r.name)).toVector),
      "multiPolygons" -> JsArray(args.multiPolygons.map(m => JsString(m.toGeoJson())).toVector)
    )

    def read(value: JsValue) = {
      value.asJsObject.getFields("zoom", "rasters", "multiPolygons") match {
        case Seq(JsNumber(zoom), JsArray(rasters), JsArray(multiPolygons)) =>
          new CountArgs(
            rasters.map { r => LayerId(r.convertTo[String], zoom.toInt) },
            multiPolygons.map { m => m.convertTo[String].parseGeoJson[MultiPolygon] }
          )
        case _ =>
          throw new DeserializationException("Bad Count Arguments")
      }
    }
  }
}
