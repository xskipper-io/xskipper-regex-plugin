/*
 * Copyright 2021 IBM Corp.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.xskipper.plugins.regex

import io.xskipper._
import io.xskipper.implicits._
import io.xskipper.plugins.regex.implicits._
import io.xskipper.plugins.regex.filter.RegexValueListMetaDataFilterFactory
import io.xskipper.plugins.regex.index.RegexIndexFactory
import io.xskipper.plugins.regex.parquet.{RegexValueListClauseTranslator, RegexValueListMetaDataTranslator}
import org.apache.log4j.{Level, LogManager}
import org.apache.spark.sql.SparkSession

object RegexValueListPluginRunner {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .appName("RegexValueListPluginRunner")
      .config("spark.master", "local[*]") // comment out to run in production
      .getOrCreate()

    // set debug log level specifically for xskipper package
    LogManager.getLogger("io.xskipper.search").setLevel(Level.DEBUG)

    // registering the filter factories for user metadataFilters
    Registration.addIndexFactory(RegexIndexFactory)
    Registration.addMetadataFilterFactory(RegexValueListMetaDataFilterFactory)
    Registration.addClauseTranslator(RegexValueListClauseTranslator)
    Registration.addMetaDataTranslator(RegexValueListMetaDataTranslator)

    val base_path = System.getProperty("user.dir")
    val dataset_path = base_path + "/src/test/resources/sample_dataset"
    val md_base_path = base_path + "/src/test/resources/metadata"

    // Set JVM Wide parameters
    val conf = Map(
      "io.xskipper.parquet.mdlocation" -> md_base_path,
      "io.xskipper.parquet.mdlocation.type" -> "EXPLICIT_BASE_PATH_LOCATION")
    Xskipper.setConf(conf)

    // enable xskipper
    spark.enableXskipper()

    // index the dataset
    val xskipper = new Xskipper(spark, dataset_path)

    // remove existing index if needed
    if (xskipper.isIndexed()) {
      xskipper.dropIndex()
    }

    // build the index
    val reader = spark
      .read
      .format("csv")
      .option("header", "true")
      .option("inferSchema", "true")

    xskipper
      .indexBuilder()
      .addRegexValueListIndex("log_line", Seq(".* .* .* (.*): .*"))
      .build(reader).show(false)

    // Describe the index
    xskipper.describeIndex(reader).show(false)

    // Enable Xskipper and run a sample query
    spark.enableXskipper()

    val df = reader.load(dataset_path)
    df.createOrReplaceTempView("tbl")

    spark.sql("SELECT * FROM tbl WHERE regexp_extract(log_line," +
      "'.* .* .* (.*): .*', 1) = 'MemoryStore'")
      .show(false)

    // show data skipping stats
    Xskipper.getLatestQueryAggregatedStats(spark).show(false)
  }
}
