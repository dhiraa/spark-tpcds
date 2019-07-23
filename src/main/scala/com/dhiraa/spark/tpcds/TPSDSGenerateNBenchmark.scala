package com.dhiraa.spark.tpcds

import java.io.{BufferedReader, IOException, InputStreamReader}
import java.util.Locale

import org.apache.spark.sql.execution.benchmark.TPCDSQueryBenchmark

import scala.util.Try

class TPSDSGenerateNBenchmarkArguments(val args: Array[String]) {
  println("=======================================================================")

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
  var tpcdsDataLocation: String = null
  var outDataLocation: String = ""
  var queryFilter: Set[String] = Set.empty

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

        case "--tpcds-data-location" :: value :: tail =>
          tpcdsDataLocation = value
          args = tail

        case "--out-data-location" :: value :: tail =>
          outDataLocation = value
          args = tail

        case "--query-filter" :: value :: tail =>
          queryFilter = value.toLowerCase(Locale.ROOT).split(",").map(_.trim).toSet
          args = tail

        case "--help" :: tail =>
          printUsageAndExit(0)

        case _ =>
          // scalastyle:off println
          System.err.println("Unknown/unsupported param " + args)
          // scalastyle:on println
          printUsageAndExit(1)
      }
    }
  }

  private def printUsageAndExit(exitCode: Int): Unit = {
    // scalastyle:off
    System.err.println("""
                         |Usage: spark-submit --class <this class> --conf key=value <spark tpcds datagen jar> [Options]
                         |Options:
                         |  --output-location [STR]                Path to an output location
                         |  --scale-factor [NUM]                   Scale factor (default: 1)
                         |  --format [STR]                         Output format (default: parquet)
                         |  --overwrite                            Whether it overwrites existing data (default: false)
                         |  --partition-tables                     Whether it partitions output data (default: false)
                         |  --use-double-for-decimal               Whether it prefers double types (default: false)
                         |  --cluster-by-partition-columns         Whether it cluster output data by partition columns (default: false)
                         |  --filter-out-null-partition-values     Whether it filters out NULL partitions (default: false)
                         |  --table-filter [STR]                   Queries to filter, e.g., catalog_sales,store_sales
                         |  --num-partitions [NUM]                 # of partitions (default: 100)
                         |  --tpcds-data-location                  Path to TPCDS data
                         |  --query-filter                         Queries to filter, e.g., q3,q5,q13
                         |  --out-data-location                    Path to store query results
                       """.stripMargin)
    // scalastyle:on
    System.exit(exitCode)
  }

  private def validateArguments(): Unit = {
    if (outputLocation == null) {
      // scalastyle:off println
      System.err.println("Must specify an output location")
      // scalastyle:on println
      printUsageAndExit(-1)
    }
    if (Try(scaleFactor.toInt).getOrElse(-1) <= 0) {
      // scalastyle:off println
      System.err.println("Scale factor must be a positive number")
      // scalastyle:on println
      printUsageAndExit(-1)
    }
    if (Try(numPartitions.toInt).getOrElse(-1) <= 0) {
      // scalastyle:off println
      System.err.println("Number of partitions must be a positive number")
      // scalastyle:on println
      printUsageAndExit(-1)
    }
  }
}

object TPSDSGenerateNBenchmark {

//  @throws[IOException]
//  def getYarnLog(appid: String): Unit = {
//    try {
//      val p = Runtime.getRuntime.exec(String.format("yarn logs -applicationId %s", appid))
//      var br = new BufferedReader(new InputStreamReader(p.getInputStream))
//      var line = ""
//      while ( {
//        (line = br.readLine) != null
//      }) System.out.println(line)
//    } catch {
//      case e: IOException =>
//        e.printStackTrace()
//    } finally if (br != null) br.close()
//  }


  def main(args: Array[String]): Unit = {

    val parsedArgs = new TPSDSGenerateNBenchmarkArguments(args)

    println("=======================================================================")
    println("=======================================================================")

    TPCDSDatagen.run(outLocation=parsedArgs.outputLocation,
      format=parsedArgs.format,
      overwrite=parsedArgs.overwrite,
      partitionTables=parsedArgs.partitionTables,
      useDoubleForDecimal=parsedArgs.useDoubleForDecimal,
      clusterByPartitionColumns=parsedArgs.clusterByPartitionColumns,
      filterOutNullPartitionValues=parsedArgs.filterOutNullPartitionValues,
      tableFilter=parsedArgs.tableFilter,
      scaleFactor=parsedArgs.scaleFactor,
      numPartitions=parsedArgs.numPartitions.toInt)

    TPCDSQueryBenchmark.run(queryFilter=parsedArgs.queryFilter,
      tpcdsDataLocation=parsedArgs.tpcdsDataLocation,
      outDataLocation=parsedArgs.outputLocation)
  }
}
