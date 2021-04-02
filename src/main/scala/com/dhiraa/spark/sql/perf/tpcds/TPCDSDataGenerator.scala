/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dhiraa.spark.sql.perf.tpcds

import java.util.Locale
import scala.util.Try

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.tpcds.TPCDSTables

class TPCDSDatagenArguments(val args: Array[String]) {

  var outputLocation: String = null
  var scaleFactor = "1"
  var format = "parquet"
  var overwrite = false
  var partitionTables = false
  var useDoubleForDecimal = false
  var clusterByPartitionColumns = false
  var filterOutNullPartitionValues = false
  var tableFilter: Set[String] = Set.empty
  var numPartitions = "100"

  parseArgs(args.toList)
  validateArguments()

  private def parseArgs(inputArgs: List[String]): Unit = {
    var args = inputArgs

    while(args.nonEmpty) {
      args match {
        case "--output-location" :: value :: tail =>
          outputLocation = value
          args = tail

        case "--scale-factor" :: value :: tail =>
          scaleFactor = value
          args = tail

        case "--format" :: value :: tail =>
          format = value
          args = tail

        case "--overwrite" :: tail =>
          overwrite = true
          args = tail

        case "--partition-tables" :: tail =>
          partitionTables = true
          args = tail

        case "--use-double-for-decimal" :: tail =>
          useDoubleForDecimal = true
          args = tail

        case "--cluster-by-partition-columns" :: tail =>
          clusterByPartitionColumns = true
          args = tail

        case "--filter-out-null-partition-values" :: tail =>
          filterOutNullPartitionValues = true
          args = tail

        case "--table-filter" :: value :: tail =>
          tableFilter = value.toLowerCase(Locale.ROOT).split(",").map(_.trim).toSet
          args = tail

        case "--num-partitions" :: value :: tail =>
          numPartitions = value
          args = tail

        case "--help" :: tail =>
          printUsageAndExit(0)

        case _ =>
          System.err.println("Unknown/unsupported param " + args)
          printUsageAndExit(1)
      }
    }
  }

  private def printUsageAndExit(exitCode: Int): Unit = {
    System.err.println("""
                         |Usage: spark-submit --class <this class> --conf key=value <spark tpcds datagen jar> [Options]
                         |Options:
                         |  --output-location [STR]                Path to an output location
                         |  --scale-factor [NUM]                   Volume of data to generate in GB (default: 1)
                         |  --format [STR]                         Output format (default: parquet)
                         |  --overwrite                            Whether it overwrites existing data (default: false)
                         |  --partition-tables                     Whether it partitions output data (default: false)
                         |  --use-double-for-decimal               Whether it prefers double types (default: false)
                         |  --cluster-by-partition-columns         Whether it cluster output data by partition columns (default: false)
                         |  --filter-out-null-partition-values     Whether it filters out NULL partitions (default: false)
                         |  --table-filter [STR]                   Queries to filter, e.g., catalog_sales,store_sales
                         |  --num-partitions [NUM]                 # of partitions (default: 100)
                         |
      """.stripMargin)
    System.exit(exitCode)
  }

  private def validateArguments(): Unit = {
    if (outputLocation == null) {
      System.err.println("Must specify an output location")
      printUsageAndExit(-1)
    }
    if (Try(scaleFactor.toInt).getOrElse(-1) <= 0) {
      System.err.println("Scale factor must be a positive number")
      printUsageAndExit(-1)
    }
    if (Try(numPartitions.toInt).getOrElse(-1) <= 0) {
      System.err.println("Number of partitions must be a positive number")
      printUsageAndExit(-1)
    }
  }
}

object TPCDSDataGenerator {

  def run(outLocation: String,
          format: String,
          overwrite: Boolean,
          partitionTables: Boolean,
          useDoubleForDecimal: Boolean,
          clusterByPartitionColumns: Boolean,
          filterOutNullPartitionValues: Boolean,
          tableFilter: Set[String] = Set.empty,
          scaleFactor: String,
          numPartitions: Int = 100): Unit = {

    val spark = SparkSession.builder.getOrCreate()
    spark.sparkContext.setLogLevel("Error")

    val tpcdsTables = new TPCDSTables(sqlContext=spark.sqlContext,
      scaleFactor=scaleFactor,
      useDoubleForDecimal=useDoubleForDecimal,
      useStringForDate=false)

    tpcdsTables.genData(
      location=outLocation,
      format=format,
      overwrite=overwrite,
      partitionTables=partitionTables,
      clusterByPartitionColumns=clusterByPartitionColumns,
      filterOutNullPartitionValues=filterOutNullPartitionValues,
      tableFilter=tableFilter,
      numPartitions=numPartitions.toInt)
  }

  def main(args: Array[String]): Unit = {

    val datagenArgs = new TPCDSDatagenArguments(args)

    run(outLocation=datagenArgs.outputLocation,
      format=datagenArgs.format,
      overwrite=datagenArgs.overwrite,
      partitionTables=datagenArgs.partitionTables,
      useDoubleForDecimal=datagenArgs.useDoubleForDecimal,
      clusterByPartitionColumns=datagenArgs.clusterByPartitionColumns,
      filterOutNullPartitionValues=datagenArgs.filterOutNullPartitionValues,
      tableFilter=datagenArgs.tableFilter,
      numPartitions=datagenArgs.numPartitions.toInt,
      scaleFactor = datagenArgs.scaleFactor)
    
  }
}

