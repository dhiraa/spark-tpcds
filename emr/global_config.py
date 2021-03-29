import requests
import getpass
import pulumi

config = pulumi.Config()
exp = config.require_object('exp')
print("Experiment specific configs: ", exp)

r = requests.get(r'http://jsonip.com')

# Global
CURRENT_MACHINE_IP = r.json()['ip']
CURRENT_USER = getpass.getuser()
TAGS = {"user": CURRENT_USER, "tool": "Pulumi", "purpose": "Spark_TPCDS_Benchmark"}

# Cluster
CLUSTER_NAME = CURRENT_USER + "-Spark-TPCDS-Benchmark-Pulumi"
EMR_RELEASE_LABEL = "emr-6.2.0"
PEM_FILE_KEY_NAME = "PySparkImagineaLabs"
MASTER_NODE_NAME = CURRENT_USER + "-Master-Nodes-Pulumi"
CORE_NODE_NAME = CURRENT_USER + "-Core-Nodes-Pulumi"
TASK_NODE_NAME = CURRENT_USER + "-Task-Nodes-Pulumi"
LOG_URI = "s3://imaginealabs/logs/emr/"

# https://www.ec2instances.info/
MASTER_NODE_TYPE = exp["master_type"]
MASTER_NODE_INSTANCE_COUNT = exp["master_count"]
CORE_NODE_TYPE = exp["core_type"]
CORE_NODE_INSTANCE_COUNT = exp["core_count"]
TASK_NODE_TYPE = exp["task_type"]
TASK_NODE_INSTANCE_COUNT = exp["task_count"]

EBS_VOLUME = 32 #In GBs

#Networking

VPC_NAME = CURRENT_USER + "-VPC-Pulumi"
SUBNET_NAME = CURRENT_USER + "-Subnet-Pulumi"
GATEWAY_NAME = CURRENT_USER + "-GW-Pulumi"
ROUTE_TABLE_NAME = CURRENT_USER + "-Route-Pulumi"
MAIN_ROUTE_TABLE_ASSOCIATION_NAME = CURRENT_USER + "-Main-RouteAssociation-Pulumi"
SECURITY_GROUP_NAME = CURRENT_USER + "-SecurityGroup-Pulumi"