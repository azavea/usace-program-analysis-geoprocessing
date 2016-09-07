package com.azavea.usace.programanalysis.geop

import geotrellis.raster.Tile
import geotrellis.spark.io._
import geotrellis.spark.{SpatialKey, LayerId}
import geotrellis.spark.io.s3.{S3AttributeStore, S3ValueReader}

import org.apache.spark.SparkContext

object LayerReader {
  /**
    * Given values for zoom, x and y, and a spark context, returns
    * the corresponding [[Tile]] object.
    *
    * @param    zoom  [[Int]] value of zoom level
    * @param    x     [[Int]] x-value of the spatial key
    * @param    y     [[Int]] y-value of the spatial key
    * @param    sc    [[SparkContext]] for the [[S3ValueReader]]
    * @return         [[Tile]] object
    */
  def apply(
    zoom: Int,
    x: Int,
    y: Int,
    sc: SparkContext
  ): Tile = {
    val sKey = SpatialKey(x, y)
    val rdr = catalog(sc, zoom)

    rdr.read(sKey)
  }

  /**
    * Given a spark context, returns the correct catalog. This configures the
    * next method with defaults.
    *
    */
  def catalog(sc: SparkContext, zoom: Int): Reader[SpatialKey, Tile] =
    catalog("azavea-datahub", "catalog", zoom)(sc)

  def catalog(
    bucket: String,
    rootPath: String,
    zoom: Int
  )(implicit sc: SparkContext): Reader[SpatialKey, Tile] = {
    val attributeStore = new S3AttributeStore(bucket, rootPath)
    val vr = new S3ValueReader(attributeStore)
    val layer = LayerId("nlcd-zoomed", zoom)
    val reader: Reader[SpatialKey, Tile] = vr.reader(layer)

    reader
  }
}
