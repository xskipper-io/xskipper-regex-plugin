/*
 * Copyright 2021 IBM Corp.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.xskipper.plugins.regex.index

import java.util.Locale
import java.util.regex.Pattern

import io.xskipper.XskipperException
import io.xskipper.index.metadata.MetadataType
import io.xskipper.index.{Index, IndexCompanion}
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, Row}

object RegexValueListIndex extends IndexCompanion[RegexValueListIndex] {
  val REGEX_PATTERN_KEY_PREFIX = "io.xskipper.plugins.regex.pattern"

  override def apply(params: Map[String, String],
                     keyMetadata: Option[String],
                     cols: Seq[String]): RegexValueListIndex = {
    if (cols.size != 1) {
      throw new XskipperException("ValueList Index takes exactly 1 Column!")
    }
    val col = cols(0)
    val patterns = (0 until params.size).map(i => {
      params.get(s"${RegexValueListIndex.REGEX_PATTERN_KEY_PREFIX}.r${i}") match {
        case Some(pattern) => pattern
        case _ =>
          throw new XskipperException(s"Index cannot be created - " +
            s"RegexValueListIndex must contain the parameter " +
            s"${RegexValueListIndex.REGEX_PATTERN_KEY_PREFIX}.r${i}")
      }
    })
    RegexValueListIndex(col, patterns, keyMetadata)
  }
}

/**
 * Represents an abstract regex value list index
 *
 * @param col the column on which the index is applied
 *
 */
case class RegexValueListIndex(col : String, patterns: Seq[String],
                               keyMetadata : Option[String] = None)
  extends Index(Map(patterns.zipWithIndex.map {
    case (p: String, i: Int) => s"${RegexValueListIndex.REGEX_PATTERN_KEY_PREFIX}.r${i}" -> p}: _*),
    keyMetadata, col) {

  val regexPatterns = patterns.map(p => Pattern.compile(p))

  override def getName: String = "regexvaluelist"

  /**
   * Gets a [[DataFrame]] row and extract the raw metadata needed by the index
   *
   * @param row [[Row]] a row to be indexed
   * @return raw metadata needed by the index or null if the row contain null value
   */
  override def getRowMetadata(row: Row) : scala.collection.mutable.HashSet[String] = {
    // For each row get a sequence of matches
    val value = row.getString(row.fieldIndex(colsMap(col).name))
    val matches = scala.collection.mutable.HashSet[String]()
    regexPatterns.foreach(p => {
      val m = p.matcher(value)
      // add all matches to the list
      while (m.find()) {
        matches += m.group(1)
      }
    })
    matches
  }

  /**
   * Same as above reduce given two accumulated metadata
   *
   * @return updated metadata for the index
   */
  override def reduce(md1: MetadataType, md2: MetadataType): MetadataType = {
    (md1, md2) match {
      case (null, null) => null
      case (md1: MetadataType, null) => md1
      case (null, md2: MetadataType) => md2
      case (md1: MetadataType, md2: MetadataType) =>
        md1.asInstanceOf[RegexValueListMetadata].values ++=
          md2.asInstanceOf[RegexValueListMetadata].values
        md1
    }
  }

  /**
   * Given an accumulated metadata and new value - process the new value and returns an updated
   * accumulated metadata
   *
   * @param accuMetadata accumulated metadata created by processing all values until curr
   * @param curr new value to be processed
   * @return updated metadata for the index
   */
  override def reduce(accuMetadata: MetadataType, curr: Any): MetadataType = {
    (accuMetadata, curr) match {
      case (null, null) => RegexValueListMetadata(scala.collection.mutable.HashSet[String]())
      case (accuMetadata: MetadataType, null) => accuMetadata
      case (null, curr: scala.collection.mutable.HashSet[String]) =>
        val res = RegexValueListMetadata(scala.collection.mutable.HashSet[String]())
        res.values ++= curr
        res
      case (accuMetadata: MetadataType, curr: scala.collection.mutable.HashSet[String]) =>
        accuMetadata.asInstanceOf[RegexValueListMetadata].values ++= curr
        accuMetadata
      case (_: MetadataType, curr: Any) =>
        throw new XskipperException(s"Invalid value type for RegexValueListIndex -" +
          s" ${curr.getClass.toString}")
    }
  }

  /**
   * Gets a [[DataFrame]] and checks whether it is valid for the index
   *
   * @param df the [[DataFrame]] to be checked
   * @param schemaMap a map containing column names (as appear in the object) and their data types
   *                  the key is the column name in lower case
   * @throws [[XskipperException]] if invalid index
   */
  override def isValid(df: DataFrame,
                       schemaMap: Map[String, (String, DataType)]): Unit = {
    // Supporting only String type
    if (!schemaMap(col.toLowerCase(Locale.ROOT))._2.isInstanceOf[StringType]) {
      throw new XskipperException(
        "RegexValueList Index can only be created on StringType")
    }
  }

  /**
   * @return the (full) name of the MetaDataType class used by this index
   */
  override def getMetaDataTypeClassName(): String = classOf[RegexValueListMetadata].getName
}
