/*
 * Copyright 2021 IBM Corp.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.xskipper.plugins.regex.index

import io.xskipper.index.metadata.MetadataType

import scala.collection.mutable.HashSet

/**
 * Regex value list metadata
 *
 * @param values Iterable containing the distinct values that appeared after applying the regex
 */
@SerialVersionUID(1L)
case class RegexValueListMetadata(values: HashSet[String]) extends MetadataType
