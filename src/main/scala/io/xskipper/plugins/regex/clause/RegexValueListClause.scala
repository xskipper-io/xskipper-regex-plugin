/*
 * Copyright 2021 IBM Corp.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.xskipper.plugins.regex.clause

import io.xskipper.search.clause.Clause
import org.apache.spark.sql.catalyst.expressions.Literal

/**
  * Represents an abstract regex value list clause
  *
  * @param col    the column on which the clause is applied
  * @param values array literal with values representing the value list from the query
  * @param negated  if true the clause is used to check whether the value list contain values which
  *                 are different from all of the values in the list (used for inequality checks)
  *                 if false the clause is used to check whether the value list metadata contain all
  *                  of the values in the list (used for equality checks)
  */
case class RegexValueListClause(col : String, values : Literal, negated: Boolean) extends Clause
