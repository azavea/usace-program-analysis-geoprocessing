package com.azavea.usace.programanalysis.geop

import geotrellis.raster.Tile
import geotrellis.spark.io.{Intersects, _}
import geotrellis.spark.{SpatialKey, TileLayerMetadata, _}
import geotrellis.spark.io.s3.{S3AttributeStore, S3LayerReader}
import geotrellis.vector.{Extent, MultiPolygon}

import org.apache.spark.SparkContext


object ClippedLayers {
  /**
    * Given a list of layer ids, a multipolygon and a spark context, returns
    * a list of [[TileLayerRDD]]s cropped to the multipolygon.
    *
    * @param    rasterLayerIds  List of [[LayerId]]s to crop
    * @param    multiPolygon    [[MultiPolygon]] to crop to
    * @param    sc              [[SparkContext]] for the [[S3LayerReader]]
    * @return                   List of [[TileLayerRDD]]s which includes only
    *                           the tiles that either completely or partially
    *                           overlap given multiPolygon.
    */
  def apply(
    rasterLayerIds: Seq[LayerId],
    multiPolygon: MultiPolygon,
    sc: SparkContext
  ): Seq[TileLayerRDD[SpatialKey]] = {
    val extent = multiPolygon.envelope
    val rasterLayers = rasterLayerIds.map(rasterLayerId =>
      queryAndCropLayer(catalog(sc), rasterLayerId, extent)
    )

    rasterLayers
  }

  /**
    * Given a catalog, layer id and extent, returns a [[TileLayerRDD]] of the
    * layer from the catalog with the matching id cropped to the extent.
    *
    * @param    catalog         The [[S3LayerReader]] to look up layers from
    * @param    layerId         The [[LayerId]] of the layer to look up
    * @param    extent          The [[Extent]] to crop to
    * @return                   A [[TileLayerRDD]] cropped to the extent
    */
  def queryAndCropLayer(
    catalog: S3LayerReader,
    layerId: LayerId,
    extent: Extent
  ): TileLayerRDD[SpatialKey] = {
    catalog.query[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](layerId)
      .where(Intersects(extent))
      .result
  }

  /**
    * Given a spark context, returns the correct catalog. This configures the
    * next method with defaults.
    *
    */
  def catalog(sc: SparkContext): S3LayerReader =
    catalog("azavea-datahub", "catalog")(sc)

  /**
    * Given an S3 bucket name and root path, returns a catalog of layers
    *
    * @param    bucket          The name of the S3 Bucket of the catalog
    * @param    rootPath        Root path of the catalog in the bucket
    * @param    sc              [[SparkContext]] to use when connecting
    * @return                   An [[S3LayerReader]] which represents the
    *                           catalog of available layers from which
    *                           individual layers can be looked up.
    */
  def catalog(
    bucket: String,
    rootPath: String
  )(implicit sc: SparkContext): S3LayerReader = {
    val attributeStore = new S3AttributeStore(bucket, rootPath)
    val catalog = new S3LayerReader(attributeStore)

    catalog
  }
}
