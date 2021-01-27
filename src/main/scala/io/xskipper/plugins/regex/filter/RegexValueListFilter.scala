/*
 * Copyright 2021 IBM Corp.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.xskipper.plugins.regex.filter

import io.xskipper.plugins.regex.clause.RegexValueListClause
import io.xskipper.search.clause.{Clause, FalseClause}
import io.xskipper.search.expressions.MetadataWrappedExpression
import io.xskipper.search.filters.BaseMetadataFilter
import io.xskipper.utils.Utils
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.types.{ArrayType, DataType, StringType}

/**
  * Represents a value list filter in - used to add [[RegexValueListClause]]-s
  * to [[MetadataWrappedExpression]]
  *
  * @param col the column on which the filter is applied
  */
case class RegexValueListFilter(col : String, patterns: Seq[String]) extends BaseMetadataFilter {
  /**
   * Returns a clause representing the query contents associated with the expression
   *
   * @param wrappedExpr the expression to be processed
   */
  override protected def getQueryContent(wrappedExpr: MetadataWrappedExpression): Option[Clause] = {
    val (expression, negate) = wrappedExpr.expr match {
      case Not(exp) => (exp, true)
      case e => (e, false)
    }

    def isValidExpr(regExpExtract: RegExpExtract): Boolean = {
      Utils.getName(regExpExtract.subject).equalsIgnoreCase(col) &&
      patterns.contains(regExpExtract.regexp.asInstanceOf[Literal].value.toString)
    }

    val conditionVals: Option[(Array[Any], DataType)] = expression match {
      case In(regExpExtract: RegExpExtract, values) if isValidExpr(regExpExtract)
        && values.forall(_.isInstanceOf[Literal]) =>
        Some(values.map(_.asInstanceOf[Literal].value).toArray, StringType)
      case InSet(regExpExtract: RegExpExtract, values) if isValidExpr(regExpExtract) =>
        // InSet contains the values as their scala types and not Literal
        // converting to scala and back to literal to handle cases such as UT8String
        // being use for String which is internal only for Spark use
        Some(values.toArray, StringType)
      case EqualTo(regExpExtract: RegExpExtract, v: Literal) if isValidExpr(regExpExtract) =>
        Some(Array(v.value), StringType)
      case EqualTo(v: Literal, regExpExtract: RegExpExtract) if isValidExpr(regExpExtract)
        => Some(Array(v), StringType)
      case _ => None
    }

    conditionVals match {
      case Some((vals, dataType)) =>
        if (vals.nonEmpty) {
          val values = Literal.create(vals, ArrayType(dataType))
          Some(RegexValueListClause(col, values, negate))
        } else {
          Some(FalseClause(col))
        }
      case _ => None // the match entered the default... no query to return.
    }
  }
}
