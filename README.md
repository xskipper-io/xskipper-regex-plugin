<!--
 -- Copyright 2021 IBM Corp.
 -- SPDX-License-Identifier: Apache-2.0
 -->

# Xskipper Regex Plugin

![Build Status](https://github.com/xskipper-io/xskipper-regex-plugin/workflows/build/badge.svg)

A sample plugin for [Xskipper](https://github.com/xskipper-io/xskipper). See Xskipper [documentation](https://xskipper-io.github.io/xskipper/api/creating-new-plugin/) for more details on how to create a plugin.

The plugin enable to index a column by specifying a list of patterns and saving the matching groups as a value list.

For example, given the following dataset:
```csv
application_name,log_line
batch job,20/12/29 18:04:39 INFO FileSourceStrategy: Pruning directories with:
batch job,20/12/29 18:04:40 INFO DAGScheduler: ResultStage 22 (collect at ParquetMetadataHandle.scala:324) finished in 0.011 s
```

and the regex pattern `".* .* .* (.*): .*"`

The metadata that will be saved is `List("FileSourceStrategy", "DAGScheduler")`.

Then the following query will benefit from the regex index:
```SQL
SELECT * 
FROM tbl 
WHERE 
regexp_extract(log_line, '.* .* .* (.*): .*', 1) = 'MemoryStore'
```

# Run as a project

To build a project using the Xskipper binaries from the Maven Central Repository, use the following Maven coordinates:

## Maven

Include Xskipper Regex plugin in a Maven project by adding it as a dependency in the project's POM file along. The plugin should be compiled with Scala 2.12.

```XML
<dependency>
  <groupId>io.xskipper</groupId>
  <artifactId>xskipper-regex-plugin_2.12</artifactId>
  <version>0.1.0</version>
</dependency>
```

## SBT
Include the plugin in an SBT project by adding the following line to its build.sbt file:

```Scala
libraryDependencies += "io.xskipper" %% "xskipper-regex-plugin" % "0.1.0"
```

# Usage Example

The following shows a simple usage example

<details>
  <summary>Python</summary>
  
  ```python
  from xskipper import Xskipper
  from xskipper import Registration
  
  metadata_location = "src/test/resources/metadata"  

  conf = dict([
              ('io.xskipper.parquet.mdlocation', metadata_location),
              ("io.xskipper.parquet.mdlocation.type", "EXPLICIT_BASE_PATH_LOCATION")])
  Xskipper.setConf(spark, conf)
  # Register the needed classes
  # Add MetadataFilterFactor
  Registration.addMetadataFilterFactory(spark, 'io.xskipper.plugins.regex.filter.RegexValueListMetaDataFilterFactory')
  # Add IndexFactory
  Registration.addIndexFactory(spark, 'io.xskipper.plugins.regex.index.RegexIndexFactory')
  # Add MetaDataTranslator
  Registration.addMetaDataTranslator(spark, 'io.xskipper.plugins.regex.parquet.RegexValueListMetaDataTranslator')
  # Add ClauseTranslator
  Registration.addClauseTranslator(spark, 'io.xskipper.plugins.regex.parquet.RegexValueListClauseTranslator')
  
  dataset_location = "src/test/resources/sample_dataset"
  reader = spark.read.format("csv").option("inferSchema", "true").option("header", "true")

  xskipper = Xskipper(spark, dataset_location)
  
  # test adding all index types including using the custom index API
  xskipper.indexBuilder() \
      .addCustomIndex("io.xskipper.plugins.regex.index.RegexValueListIndex", ["log_line"],
                      {"io.xskipper.plugins.regex.pattern.r0": ".* .* .* (.*): .*"}) \
      .build(reader) \
      .show(10, False)
  
  Xskipper.enable(spark)
  
  spark.sql("SELECT * FROM tbl WHERE regexp_extract(log_line,'.* .* .* (.*): .*', 1) = 'MemoryStore'").show()
  
  xskipper.getLatestQueryAggregatedStats(spark).show(10, False)
  ```

</details>

<details>
  <summary>Scala</summary>
  
  ```Scala
  import io.xskipper._
  import io.xskipper.implicits._
  import io.xskipper.plugins.regex.implicits._
  import io.xskipper.plugins.regex.implicits._
  import io.xskipper.plugins.regex.filter.RegexValueListMetaDataFilterFactory
  import io.xskipper.plugins.regex.index.RegexIndexFactory
  import io.xskipper.plugins.regex.parquet.{RegexValueListClauseTranslator, RegexValueListMetaDataTranslator}
   
  // Register the plugin classes
  Registration.addIndexFactory(RegexIndexFactory)
  Registration.addMetadataFilterFactory(RegexValueListMetaDataFilterFactory)
  Registration.addClauseTranslator(RegexValueListClauseTranslator)
  Registration.addMetaDataTranslator(RegexValueListMetaDataTranslator)
  
  val metadata_location = "src/test/resources/metadata" 

  // Set JVM Wide parameters
  val conf = Map(
    "io.xskipper.parquet.mdlocation" -> metadata_location,
    "io.xskipper.parquet.mdlocation.type" -> "EXPLICIT_BASE_PATH_LOCATION")
  Xskipper.setConf(conf)
    
  val dataset_location = "src/test/resources/sample_dataset"
  val reader = spark.read.format("csv").option("inferSchema", "true").option("header", "true")

  // index the dataset
  val xskipper = new Xskipper(spark, dataset_location)
  
  // remove existing index if needed
  if (xskipper.isIndexed()) {
    xskipper.dropIndex()
  }
  
  xskipper
        .indexBuilder()
        .addRegexValueListIndex("log_line", Seq(".* .* .* (.*): .*"))
        .build(reader).show(false)
  
  // enable xskipper
  spark.enableXskipper()
  
  spark.sql("SELECT * FROM tbl WHERE regexp_extract(log_line," +
        "'.* .* .* (.*): .*', 1) = 'MemoryStore'")
        .show(false)
  
  // show data skipping stats
  Xskipper.getLatestQueryAggregatedStats(spark).show(false)
  ```

</details>

# Building

xskipper-regex-plugin is compiled using [SBT](https://www.scala-sbt.org/1.x/docs/Command-Line-Reference.html).

To compile, run

    build/sbt compile

To generate artifacts, run

    build/sbt package

To execute tests, run

    build/sbt test

Refer to [SBT docs](https://www.scala-sbt.org/1.x/docs/Command-Line-Reference.html) for more commands.

# Collaboration

xskipper-regex-plugin tracks issues in GitHub and prefers to receive contributions as pull requests.

# Compatibility

xskipper-regex-plugin is compatible with Xskipper 1.2.0 and requires Apache Spark 3.0.0 

# License
Apache License 2.0, see [LICENSE](LICENSE).
