/*
 * Copyright 2021 IBM Corp.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.xskipper.plugins.regex.index

import io.xskipper.XskipperException
import io.xskipper.index.{Index, IndexFactory}
import org.apache.spark.internal.Logging

object RegexIndexFactory extends IndexFactory with Logging {
  /**
    * Create an [[Index]] instance according to the given parameters
    *
    * @param indexType the index type to be created
    * @param cols the columns on which the index should be created
    * @param params the index parameters map
    * @param keyMetadata the key metadata associated with the index
    * @return The index that can be created from the parameters if possible
    */
  override def getIndex(indexType: String, cols: Seq[String], params: Map[String, String],
                        keyMetadata: Option[String] = None) : Option[Index] = {
    indexType match {
      case "regexvaluelist" =>
        // get all regex from the parameters
        val patterns = (0 until params.size).map(i => {
          params.get(s"${RegexValueListIndex.REGEX_PATTERN_KEY_PREFIX}.r${i}") match {
            case Some(pattern) => pattern
            case _ =>
              throw new XskipperException(s"Index cannot be loaded - " +
                s"RegexValueListIndex must contain the parameter " +
                s"${RegexValueListIndex.REGEX_PATTERN_KEY_PREFIX}.r${i}")
          }
        })
        Some(RegexValueListIndex(cols(0), patterns, keyMetadata = keyMetadata))
      case _ => None
    }
  }
}
