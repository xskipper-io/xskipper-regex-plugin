/*
 * Copyright 2021 IBM Corp.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.xskipper.plugins.regex.filter

import io.xskipper.index.Index
import io.xskipper.plugins.regex.index.RegexValueListIndex
import io.xskipper.search.filters.{MetadataFilter, MetadataFilterFactory}

object RegexValueListMetaDataFilterFactory extends MetadataFilterFactory {
  /**
    * Create [[MetaDataFilters]] according to given indexes
    *
    * @param descriptors the sequence of indexes to be processed
    * @return a sequence of MetaDataFilter matching the given indexes
    */
  override def getFilters(descriptors: Seq[Index]): Seq[MetadataFilter] = {
    descriptors.flatMap(descriptor => descriptor match {
      case RegexValueListIndex(col, pattern, _) =>
        Some(RegexValueListFilter(col, pattern))
      case _ => None
    })
  }
}
