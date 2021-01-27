/*
 * Copyright 2021 IBM Corp.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.xskipper.metadatastore.parquet

import java.io.File
import java.nio.file.Files

import io.xskipper.plugins.regex.RegexValueListIndexSuiteBase
import io.xskipper.plugins.regex.filter.RegexValueListMetaDataFilterFactory
import io.xskipper.plugins.regex.index.RegexIndexFactory
import io.xskipper.plugins.regex.parquet.{RegexValueListClauseTranslator, RegexValueListMetaDataTranslator}
import io.xskipper.{Registration, Xskipper}

abstract class RegexValueListIndexSuiteParquet(
                                                override val datasourceV2: Boolean)
  extends RegexValueListIndexSuiteBase(datasourceV2,
    ParquetMetadataStoreManager) {

  val mdPath = Files.createTempDirectory("xskipper_regex_plugin_tests").toString
  new File(mdPath).deleteOnExit()

  override def getXskipper(uri: String): Xskipper = {
    val params = Map[String, String](
      ParquetMetadataStoreConf.PARQUET_MD_LOCATION_KEY -> mdPath,
      ParquetMetadataStoreConf.PARQUET_MD_LOCATION_TYPE_KEY
        -> ParquetMetadataStoreConf.PARQUET_MD_LOCATION_EXPLICIT_BASE_PATH_LOCATION
    )
    val res = new Xskipper(spark, uri, ParquetMetadataStoreManager)
    res.setParams(params)
    res
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    Registration.addIndexFactory(RegexIndexFactory)
    Registration.addMetadataFilterFactory(RegexValueListMetaDataFilterFactory)
    Registration.addClauseTranslator(RegexValueListClauseTranslator)
    Registration.addMetaDataTranslator(RegexValueListMetaDataTranslator)
  }
}

class RegexValueListIndexSuiteParquetV1 extends RegexValueListIndexSuiteParquet(false)

class RegexValueListIndexSuiteParquetV2 extends RegexValueListIndexSuiteParquet(true)
