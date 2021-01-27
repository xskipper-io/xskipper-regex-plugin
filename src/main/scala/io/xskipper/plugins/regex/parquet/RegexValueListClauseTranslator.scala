/*
 * Copyright 2021 IBM Corp.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.xskipper.plugins.regex.parquet

import io.xskipper.metadatastore.parquet.{Parquet, ParquetUtils}
import io.xskipper.metadatastore.{ClauseTranslator, MetadataStoreManagerType}
import io.xskipper.plugins.regex.clause.RegexValueListClause
import io.xskipper.search.clause.Clause
import org.apache.spark.sql.Column
import org.apache.spark.sql.functions.{arrays_overlap, col, lit, _}

object RegexValueListClauseTranslator extends ClauseTranslator {
  /**
    * Translates a clause to the representation of Parquet [[MetaDataStoreType]]
    *
    * @param metaDataStoreType the [[MetadataStoreManagerType]] to translate to
    * @param clause the clause to be translated
    * @param clauseTranslators a sequence of clause translation factories
    *                          to enable recursive translation
    * @return return type is Any since each metadatastore may require different representation
    */
  override def translate(metaDataStoreType: MetadataStoreManagerType, clause: Clause,
                         clauseTranslators: Seq[ClauseTranslator]): Option[Column] = {
    metaDataStoreType match {
      case Parquet =>
        clause match {
          case RegexValueListClause(column, values, false) =>
            val mdColName = ParquetUtils.getColumnNameForCols(Seq(column), "regexvaluelist")
            Some(arrays_overlap(col(mdColName), lit(values)))
          // checks if the regex value list metadata contain values which
          // are different than the given list of values
          // (used for inequality checks)
          case RegexValueListClause(column, values, true) =>
            val mdColName = ParquetUtils.getColumnNameForCols(Seq(column), "regexvaluelist")
            Some(size(array_except(col(mdColName), lit(values))) > 0)
          case _ => None
        }
      case _ => None
    }
  }
}

