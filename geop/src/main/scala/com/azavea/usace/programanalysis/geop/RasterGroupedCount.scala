package com.azavea.usace.programanalysis.geop

import geotrellis.raster.rasterize.{Callback, Rasterizer}
import geotrellis.raster.{RasterExtent, Tile}
import geotrellis.spark.{SpatialKey, TileLayerRDD}
import geotrellis.vector.{MultiPolygon, MultiPolygonResult, PolygonResult}

import org.apache.spark.rdd.RDD

import scala.collection.mutable.ListBuffer


object RasterGroupedCount {
  /**
    * Given a sequence of 1 - 3 raster layers, and a multipolygon, returns a
    * mapping from a tuple of raster values of the layers in order to the
    * count of pixels matching that combination.
    *
    * @param    rasterLayers    The list of [[TileLayerRDD]]s
    * @param    multiPolygon    The [[MultiPolygon]] to count within
    * @return                   Map from a tuple of integer values to the count
    *                           of pixels matching that combination.
    */
  def apply(
    rasterLayers: Seq[TileLayerRDD[SpatialKey]],
    multiPolygon: MultiPolygon
  ): Map[Seq[Int], Int] = {
    joinRasters(rasterLayers)
      .map { case (key, tiles) =>
        getDistinctPixels(rasterLayers.head, key, tiles.head, multiPolygon)
          .map { case (col, row) => tiles.map { tile => tile.get(col, row) } }
          .groupBy(identity).map { case (k, list) => k -> list.length }
          .toList
      }
      .reduce { (left, right) => left ++ right }
      .groupBy(_._1).map { case (k, list) => k -> list.map(_._2).sum }
  }

  /**
    * Given a sequence of 1 - 3 raster layers, returns a join of them all.
    *
    * @param    rasterLayers    The list of [[TileLayerRDD]]s
    * @return                   Joined RDD with a list of tiles, corresponding
    *                           to each raster in the list, matching a spatial
    *                           key.
    */
  def joinRasters(
    rasterLayers: Seq[TileLayerRDD[SpatialKey]]
  ): RDD[(SpatialKey, List[Tile])] = {
    rasterLayers.length match {
      case 1 =>
        rasterLayers.head
          .map { case (k, v) => (k, List(v)) }
      case 2 =>
        rasterLayers.head.join(rasterLayers.tail.head)
          .map { case (k, (v1, v2)) => (k, List(v1, v2)) }
      case 3 =>
        rasterLayers.head.join(rasterLayers.tail.head).join(rasterLayers.tail.tail.head)
          .map { case (k, ((v1, v2), v3)) => (k, List(v1, v2, v3)) }

      case 0 => throw new Exception("At least 1 raster must be specified")
      case _ => throw new Exception("At most 3 rasters can be specified")
    }
  }

  /**
    * Given a layer, a key, a tile, and a multipolygon, returns a list of
    * distinct pixels present in the multipolygon clipped to an extent
    * corresponding to the key and tile.
    *
    * @param    rasterLayer   The [[TileLayerRDD]] to clip
    * @param    key           The [[SpatialKey]] to transform extent to
    * @param    tile          The [[Tile]] to calculate raster extent from
    * @param    multiPolygon  The [[MultiPolygon]] to clip to
    * @return                 List of distinct pixels
    */
  def getDistinctPixels(
    rasterLayer: TileLayerRDD[SpatialKey],
    key: SpatialKey,
    tile: Tile,
    multiPolygon: MultiPolygon
  ): ListBuffer[(Int, Int)] = {
    val extent = rasterLayer.metadata.mapTransform(key)
    val rasterExtent = RasterExtent(extent, tile.cols, tile.rows)

    val pixels = ListBuffer.empty[(Int, Int)]
    val cb = new Callback {
      def apply(col: Int, row: Int): Unit = {
        val pixel = (col, row)
        pixels += pixel
      }
    }

    multiPolygon & extent match {
      case PolygonResult(p) =>
        Rasterizer.foreachCellByPolygon(p, rasterExtent)(cb)
      case MultiPolygonResult(mp) =>
        mp.polygons.foreach { p =>
          Rasterizer.foreachCellByPolygon(p, rasterExtent)(cb)
        }

      case _ =>
    }

    pixels.distinct
  }
}
