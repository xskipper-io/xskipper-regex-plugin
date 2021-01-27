# Copyright 2021 IBM Corp.
# SPDX-License-Identifier: Apache-2.0

import unittest
import sys
import tempfile
import shutil
import os
from os import path

from pyspark.sql.types import *

from xskipper import Xskipper
from xskipper import Registration
from xskipper.testing.utils import XskipperTestCase


class RegexPluginTests(XskipperTestCase):
    def setUp(self):
        self.metadata_location = tempfile.mkdtemp()
        super(RegexPluginTests, self).setUp()

    def tearDown(self):
        shutil.rmtree(self.metadata_location)
        super(RegexPluginTests, self).tearDown()

    def test_latlon_indexing(self):
        Xskipper.installExceptionWrapper()
        conf = dict([
            ('io.xskipper.parquet.mdlocation', self.metadata_location),
            ("io.xskipper.parquet.mdlocation.type", "EXPLICIT_BASE_PATH_LOCATION")])
        Xskipper.setConf(self.spark, conf)
        # Register the needed classes
        # Add MetadataFilterFactor
        Registration.addMetadataFilterFactory(self.spark, 'io.xskipper.plugins.regex.filter.RegexValueListMetaDataFilterFactory')
        # Add IndexFactory
        Registration.addIndexFactory(self.spark, 'io.xskipper.plugins.regex.index.RegexIndexFactory')
        # Add MetaDataTranslator
        Registration.addMetaDataTranslator(self.spark, 'io.xskipper.plugins.regex.parquet.RegexValueListMetaDataTranslator')
        # Add ClauseTranslator
        Registration.addClauseTranslator(self.spark, 'io.xskipper.plugins.regex.parquet.RegexValueListClauseTranslator')

        # indexing
        root_dir = path.dirname(path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
        dataset_location = path.join(root_dir, path.join("src", "test", "resources", "sample_dataset"))
        reader = self.spark.read.format("csv").option("inferSchema", "true").option("header", "true")
        xskipper = Xskipper(self.spark, dataset_location)

        # call set params to make sure it overwrites JVM wide config
        params = dict([
            ('io.xskipper.parquet.mdlocation', "{0}_parquet".format(self.metadata_location)),
            ("io.xskipper.parquet.mdlocation.type", "EXPLICIT_BASE_PATH_LOCATION")])
        xskipper.setParams(params)

        # remove index if exists
        if xskipper.isIndexed():
            xskipper.dropIndex()

        # test adding all index types including using the custom index API
        xskipper.indexBuilder() \
            .addCustomIndex("io.xskipper.plugins.regex.index.RegexValueListIndex", ["log_line"],
                            {"io.xskipper.plugins.regex.pattern.r0": ".* .* .* (.*): .*"}) \
            .build(reader) \
            .show(10, False)

        # validate dataset is indexed
        self.assertTrue(xskipper.isIndexed(), 'Dataset should be indexed following index building')

        # describe the index
        xskipper.describeIndex(reader).show(10, False)

        # refresh the index
        xskipper.refreshIndex(reader).show(10, False)

        # enable skipping and run test query
        Xskipper.enable(self.spark)
        df = reader.load(dataset_location)
        df.createOrReplaceTempView("tbl")

        self.spark.sql("SELECT * FROM tbl WHERE regexp_extract(log_line,'.* .* .* (.*): .*', 1) = 'MemoryStore'").show()

        # get latest stats
        # JVM wide
        xskipper.getLatestQueryAggregatedStats(self.spark).show(10, False)
        # local
        xskipper.getLatestQueryStats().show(10, False)

        # drop the index and verified it is dropped
        xskipper.dropIndex()
        self.assertFalse(xskipper.isIndexed(), 'Dataset should not be indexed following index dropping')

if __name__ == "__main__":
    regexplugin_test = unittest.TestLoader().loadTestsFromTestCase(RegexPluginTests)
    result = unittest.TextTestRunner(verbosity=3).run(regexplugin_test)
    sys.exit(not result.wasSuccessful())
