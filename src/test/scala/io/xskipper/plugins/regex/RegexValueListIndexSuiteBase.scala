/*
 * Copyright 2021 IBM Corp.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.xskipper.plugins.regex

import io.xskipper.implicits._
import io.xskipper.metadatastore.{MetadataStoreManager, MetadataStoreManagerType}
import io.xskipper.plugins.regex.implicits._
import io.xskipper.testing.util.LogTrackerBuilder
import io.xskipper.{Registration, Xskipper}
import org.apache.log4j.{Level, LogManager}
import org.apache.spark.sql.SparkSession
import org.scalatest.{BeforeAndAfterEach, FunSuite}

abstract class RegexValueListIndexSuiteBase(val datasourceV2: Boolean,
                                            val mdStore: MetadataStoreManager)
  extends FunSuite
    with BeforeAndAfterEach {

  // set debug log level specifically for xskipper search package
  LogManager.getLogger("io.xskipper.search").setLevel(Level.DEBUG)

  // monitor skipped files
  val regex = "(.*).*#.*--------> SKIPPED!".r

  lazy val spark: SparkSession = {
    var builder = SparkSession.builder()
      .appName("Xskipper Tests")
      .master("local[*]")
      .config("spark.ui.enabled", "false")
    if (datasourceV2) {
      builder =
        builder.config("spark.sql.sources.useV1SourceList", "avro,kafka,text")
    }
    builder.enableHiveSupport().getOrCreate()
  }

  val baseDir = System.getProperty("user.dir")

  def getXskipper(uri: String): Xskipper

  test("Testing skipping on a basic Regex") {
    val inputPath = baseDir + "/src/test/resources/sample_dataset/"

    val xskipper = getXskipper(inputPath)
    val reader = spark
      .read
      .format("csv")
      .option("header", "true")
      .option("inferSchema", "true")
    xskipper
      .indexBuilder()
      .addRegexValueListIndex("log_line", Seq(".* .* .* (.*): .*"))
      .build(reader)
    assert(xskipper.isIndexed())
    val query = "SELECT * FROM tbl WHERE regexp_extract(log_line," +
      "'.* .* .* (.*): .*', 1) = 'MemoryStore'"

    val expectedSkippedFilesFormatted = Set("a.csv")
      .map(inputPath + _)
    // first run the query w/o skipping
    spark.disableXskipper()
    val df1 = reader.load(inputPath)
    df1.createOrReplaceTempView("tbl")
    val vanillaRes = spark.sql(query).collect()

    // now run the query again
    val tracker = LogTrackerBuilder.getRegexTracker(regex)
    tracker.startCollecting()
    val df2 = reader.load(inputPath)
    df2.createOrReplaceTempView("tbl")
    spark.enableXskipper()
    val withSkippingRes = spark.sql(query).collect()
    tracker.stopCollecting()
    assertResult(expectedSkippedFilesFormatted)(tracker.getResultSet())
    assertResult(vanillaRes)(withSkippingRes)
  }

  override def beforeEach(): Unit = {
    Xskipper.reset(spark)
    // Reregister the metadata store default
    Registration.setActiveMetadataStoreManager(mdStore.getType)
  }
}
