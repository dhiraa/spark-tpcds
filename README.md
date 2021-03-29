# spark-tpcds

## Introduction

A simple suit to explore Spark performance tuning experiments.

Please visit the original [TPS-DS](http://www.tpc.org/tpcds/) site for more details. 
This repo is fork of databricks TPC-DS, with added support of running over `spark-submit`, 
giving more control to developers for further modification as and when needed.  

TPC-DS is the de-facto industry standard benchmark for measuring the performance of decision support solutions including, 
but not limited to, Big Data systems. The current version is v2. It models several generally applicable aspects of a decision 
support system, including queries and data maintenance. Although the underlying business model of TPC-DS is a retail 
product supplier, the database schema, data population, queries, data maintenance model and implementation rules have 
been designed to be broadly representative of modern decision support systems.

This benchmark illustrates decision support systems that:

    - Examine large volumes of data
    - Give answers to real-world business questions
    - Execute queries of various operational requirements and complexities (e.g., ad-hoc, reporting, iterative OLAP, data mining)
    - Are characterized by high CPU and IO load
    - Are periodically synchronized with source OLTP databases through database maintenance functions
    - Run on “Big Data” solutions, such as RDBMS as well as Hadoop/Spark based systems

- [Specification](http://www.tpc.org/tpc_documents_current_versions/current_specifications.asp)
- [tpc-ds_v2.1.0.pdf](http://www.tpc.org/tpc_documents_current_versions/pdf/tpc-ds_v2.1.0.pdf)
- [Schema](https://gerardnico.com/data/type/relation/benchmark/tpcds/schema)
- [Making of TPC DS Paper](https://www.researchgate.net/publication/221311196_The_Making_of_TPC-DS)

## Benchmark Experiments

As with any experiment, Spark TPC-DS benchmark also comes with a lot of knobs that can be tweaked.

![](docs/spark-tpcds-experiment.png)

Following list comprises high level yet important knobs that are needed replicating consistent test results.

- TPC-DS format and data size
- EMR Cluster Configuration
    - Node Types
    - Number of Core and Task Nodes
    - Network speed
- Apache Spark Software Configuration
    - Executor configuration
        - Number of executors
        - Number Cores
        - Memory
    - Adaptive Query Engine
    
The whole infrastructure can be provisioned by Pulumi IaC library. Check [here](emn) for more details. 

### [Standalone Setup](https://jaceklaskowski.gitbooks.io/mastering-apache-spark/spark-standalone-example-2-workers-on-1-node-cluster.html)
Download the latest version from [here](https://spark.apache.org/downloads.html).
and Unzip to /some/path/spark-x.y.z-bin-hadoop2.7/
```
cd /some/path/spark-x.y.z-bin-hadoop2.7/
cp conf/spark-defaults.conf.template conf/spark-defaults.conf
cp conf/slaves.template conf/slaves
cp conf/spark-env.sh.template conf/spark-env.sh
vim conf/spark-env.sh #My machine has 12 cores and 32GB RAM
    SPARK_WORKER_CORES=4
    SPARK_WORKER_MEMORY=8g
    SPARK_WORKER_INSTANCES=2
sbin/start-all.sh #this should start two workers in yor machine
sbin/stop-all.sh #to stop 
```

Check your [Spark UI](http://localhost:8080/)!

![](docs/spark_ui.png)


### Build

```
gradle jar
```

### How to run ?

**Generate Data**
Note: `scale-factor` is volume of data to generate in GB

```
spark-submit \
--master spark://IMCHLT276:7077 \
--executor-memory 8G \
--total-executor-cores 8 \
--class com.dhiraa.spark.tpcds.TPCDSDatagen \
build/libs/spark-tpcds.jar \
--output-location /tmp/tpcds/ \
--scale-factor 1 \
--partition-tables \
--format parquet 
```

**Run TPCSDS Query Suite**
```
spark-submit \
--conf spark.sql.crossJoin.enabled=true \
--conf spark.sql.autoBroadcastJoinThreshold=-1 \
--master spark://IMCHLT276:7077 \
--executor-memory 8G \
--total-executor-cores 8 \
--class org.apache.spark.sql.execution.benchmark.TPCDSQueryBenchmark \
build/libs/spark-tpcds.jar \
--tpcds-data-location /tmp/tpcds/ \
--out-data-location /tmp/tpcds_results \
--query-filter q3,q5,q6,q7,q9,q13,q15,q17,q20,q25,q28,q29,q37,q44,q46,q48,q49,q50,a51,q52,q61,q68,q70,q73,q78,q79,q80,q82,q87,q88,q96,q97
```

**Generate And BenchMark**
```
export FACTOR=1
export TPSDS_DATA_PATH=/tmp/tpcds/$FACTOR/
export TPSDS_RESULT_PATH=/tmp/tpcds/$FACTOR/results/

spark-submit \
--conf spark.sql.crossJoin.enabled=true \
--conf spark.sql.autoBroadcastJoinThreshold=-1 \
--master spark://IMCHLT276:7077 \
--executor-memory 8G \
--total-executor-cores 8 \
--class com.dhiraa.spark.tpcds.TPSDSGenerateNBenchmark \
build/libs/spark-tpcds.jar \
--output-location $TPSDS_DATA_PATH \
--scale-factor $FACTOR \
--partition-tables \
--format parquet \
--tpcds-data-location $TPSDS_DATA_PATH \
--out-data-location $TPSDS_RESULT_PATH \
--query-filter q3,q5,q6,q7,q9,q13,q15,q17,q20,q25,q28,q29,q37,q44,q46,q48,q49,q50,a51,q52,q61,q68,q70,q73,q78,q79,q80,q82,q87,q88,q96,q97
```


**[Spark Lens](https://github.com/qubole/sparklens)**
```
export FACTOR=1
export TPSDS_DATA_PATH=hdfs://imaginealabscluster/user/mageswarand/tpcds/$RUN_ID/
export TPSDS_RESULT_PATH=hdfs://imaginealabscluster/user/mageswarand/tpcds/$RUN_ID/results/

spark-submit \
--conf spark.sql.crossJoin.enabled=true \
--conf spark.sql.autoBroadcastJoinThreshold=-1 \
--packages qubole:sparklens:0.3.0-s_2.11 \
--conf spark.extraListeners=com.qubole.sparklens.QuboleJobListener \
--conf spark.sparklens.data.dir=
--master spark://IMCHLT276:7077 \
--executor-memory 8G \
--total-executor-cores 8 \
--class com.dhiraa.spark.tpcds.TPSDSGenerateNBenchmark \
build/libs/spark-tpcds.jar \
--output-location $TPSDS_DATA_PATH \
--scale-factor $FACTOR \
--partition-tables \
--format parquet \
--tpcds-data-location $TPSDS_DATA_PATH \
--out-data-location $TPSDS_RESULT_PATH \
--query-filter q3,q5,q6,q7,q9,q13,q15,q17,q20,q25,q28,q29,q37,q44,q46,q48,q49,q50,a51,q52,q61,q68,q70,q73,q78,q79,q80,q82,q87,q88,q96,q97

```

### TODOs
- Add support for cloud containers


**References**
- https://github.com/IBM/spark-tpc-ds-performance-test
- https://github.com/databricks/spark-sql-perf
- https://github.com/maropu/spark-tpcds-datagen
- [Spark SQL Test suite](https://github.com/apache/spark/tree/master/sql/core/src/test/scala/org/apache/spark/sql/execution/benchmark)
- https://medium.com/google-cloud/data-catalog-hands-on-guide-a-mental-model-dae7f6dd49e
- https://developer.ibm.com/patterns/explore-spark-sql-and-its-performance-using-tpc-ds-workload/

**Blogs**
- https://databricks.com/session/spark-sql-2-0-experiences-using-tpc-ds
- https://www.youtube.com/watch?v=CvZ7QelRFak
- https://aws.amazon.com/blogs/big-data/performance-updates-to-apache-spark-in-amazon-emr-5-24-up-to-13x-better-performance-compared-to-amazon-emr-5-16/
- https://medium.com/@rhbutani/https-medium-com-oracle-snap-benefits-of-bi-semantics-in-spark-sql-a-view-through-the-tpcds-benchmark-5cca8d6d25d2
- https://db-blog.web.cern.ch/blog/luca-canali/2017-06-diving-spark-and-parquet-workloads-example
