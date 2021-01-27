/*
 * Copyright 2021 IBM Corp.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.xskipper.plugins.regex.parquet

import io.xskipper.index.Index
import io.xskipper.index.metadata.MetadataType
import io.xskipper.metadatastore.MetadataStoreManagerType
import io.xskipper.metadatastore.parquet.{Parquet, ParquetMetaDataTranslator}
import io.xskipper.plugins.regex.index.{RegexValueListIndex, RegexValueListMetadata}
import org.apache.spark.sql.types.{ArrayType, DataType, StringType}

object RegexValueListMetaDataTranslator extends ParquetMetaDataTranslator {
  /**
    * Translates a metaDataType to the metaDataStore representation
    *
    * @param metadataStoreManagerType the [[MetadataStoreManagerType]] to translate to
    * @param metadataType the [[MetadataType]] to translate
    * @param index the index that created [[MetadataType]]
    * @return return type is Any since each metaDataStore may require different representation
    */
  override def translate(metadataStoreManagerType: MetadataStoreManagerType,
                         metadataType: MetadataType, index: Index): Option[Any] = {
    metadataStoreManagerType match {
      case Parquet =>
        metadataType match {
          case RegexValueListMetadata(values) => Some(values.toArray)
          case _ => None
        }
      case _ => None
    }
  }

  /**
   * Given an index returns the [[org.apache.spark.sql.DataFrame]]
   * schema for the index [[MetadataType]]
   *
   * @param index
   */
  override def getDataType(index: Index): Option[DataType] = {
    index match {
      case _: RegexValueListIndex =>
        Some(ArrayType(StringType))
      case _ => None
    }
  }
}
