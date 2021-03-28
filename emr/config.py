import requests
import getpass

r = requests.get(r'http://jsonip.com')

# Global
CURRENT_MACHINE_IP = r.json()['ip']
CURRENT_USER = getpass.getuser()
TAGS = {"user": CURRENT_USER, "tool": "Pulumi", "purpose" : "Spark_TPCDS_Benchmark"}

# Cluster
CLUSTER_NAME = CURRENT_USER + "_Spark_TPCDS_Benchmark_Pulumi"
EMR_RELEASE_LABEL = "emr-6.2.0"
PEM_FILE_KEY_NAME = "PySparkImagineaLabs"
LOG_URI = "s3://imaginealabs/logs/emr/"

# https://www.ec2instances.info/
MASTER_NODE_TYPE = "m4.large"
CORE_NODE_TYPE = "m4.large"
TASK_NODE_TYPE = "m4.large"

EBS_VOLUME = 32 #In GBs

#Networking

VPC_NAME = CURRENT_USER + "_VPC_Pulumi"
SUBNET_NAME = CURRENT_USER + "_Subnet_Pulumi"
GATEWAY_NAME = CURRENT_USER + "_GW_Pulumi"
ROUTE_TABLE_NAME = CURRENT_USER + "_Route_Pulumi"
MAIN_ROUTE_TABLE_ASSOCIATION_NAME = CURRENT_USER + "_MAin_RouteAssociation_Pulumi"