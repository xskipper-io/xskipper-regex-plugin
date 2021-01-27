/*
 * Copyright 2021 IBM Corp.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.xskipper.plugins.regex

import io.xskipper.index.execution.IndexBuilder
import io.xskipper.plugins.regex.index.RegexValueListIndex

package object implicits {
  /**
   * regex implicit class on [[IndexBuilder]]
   */
  implicit class RegexValueListIndexBuilder(val indexBuilder: IndexBuilder) extends AnyVal {
    /**
     * Adds a RegexValueListIndex to the given IndexBuilder
     */
    def addRegexValueListIndex(col: String, patterns: Seq[String],
                                    keyMetadata: String): IndexBuilder = {
      indexBuilder.addCustomIndex(RegexValueListIndex(col, patterns, Some(keyMetadata)))
    }

    /**
     * Adds a RegexValueListIndex to the given IndexBuilder
     */
    def addRegexValueListIndex(col: String, patterns: Seq[String]): IndexBuilder = {
      indexBuilder.addCustomIndex(RegexValueListIndex(col, patterns))
    }
  }
}
