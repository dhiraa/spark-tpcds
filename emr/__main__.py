"""An AWS Python Pulumi program"""

import json
import pulumi
import pulumi_aws as aws
from pulumi_aws import s3
from global_config import *


# EMR provisioning
#=======================================================================================================================

# Networking
# ----------------------------------------------------------------------------------------------------------------------
vpc = aws.ec2.Vpc(VPC_NAME,
                  cidr_block="173.31.0.0/16",
                  enable_dns_hostnames=True,
                  tags=TAGS)

subnet = aws.ec2.Subnet(SUBNET_NAME,
                        vpc_id=vpc.id,
                        cidr_block="173.31.0.0/20",
                        availability_zone="ap-south-1b",
                        tags=TAGS)

# Enable SSH login from the user machine by default
security_group = aws.ec2.SecurityGroup(resource_name=SECURITY_GROUP_NAME,
                                       description="Allow inbound SSH traffic",
                                       vpc_id=vpc.id,
                                       ingress=[aws.ec2.SecurityGroupIngressArgs(
                                           from_port=22,
                                           to_port=22,
                                           protocol="TCP",
                                           cidr_blocks=[CURRENT_MACHINE_IP+"/32"]
                                           # Only the current user machine is allowed to login by default
                                       )],
                                       tags=TAGS,
                                       opts=pulumi.ResourceOptions(depends_on=[subnet]))


gate_way = aws.ec2.InternetGateway(GATEWAY_NAME, vpc_id=vpc.id, tags=TAGS)
route_table = aws.ec2.RouteTable(ROUTE_TABLE_NAME,
                                 tags=TAGS,
                                 vpc_id=vpc.id,
                                 routes=[aws.ec2.RouteTableRouteArgs(
                                     cidr_block="0.0.0.0/0",
                                     gateway_id=gate_way.id,
                                 )])

main_route_table_association = aws.ec2.MainRouteTableAssociation(MAIN_ROUTE_TABLE_ASSOCIATION_NAME,
                                                                 vpc_id=vpc.id,
                                                                 route_table_id=route_table.id)

# Cluster
# ----------------------------------------------------------------------------------------------------------------------

aws_emr_cluster = aws.emr.Cluster(CLUSTER_NAME,
                                  release_label=EMR_RELEASE_LABEL,
                                  applications=["Spark", "Hadoop", "Ganglia", "Zeppelin"],
                                  ec2_attributes=aws.emr.ClusterEc2AttributesArgs(
                                      instance_profile="EMR_EC2_DefaultRole",
                                      subnet_id=subnet.id,
                                      key_name=PEM_FILE_KEY_NAME),
                                  master_instance_group=aws.emr.ClusterMasterInstanceGroupArgs(
                                      instance_type=MASTER_NODE_TYPE,
                                      name=MASTER_NODE_NAME),
                                  core_instance_group=aws.emr.ClusterCoreInstanceGroupArgs(
                                      name=CORE_NODE_NAME,
                                      instance_type=CORE_NODE_TYPE,
                                      instance_count=CORE_NODE_INSTANCE_COUNT,
                                      ebs_configs=[{
                                          "size": EBS_VOLUME,
                                          "type": "gp2",
                                          "volumesPerInstance": 1,
                                      }],
                                  ),
                                  service_role='EMR_DefaultRole',
                                  log_uri=LOG_URI,
                                  tags=TAGS,
                                  steps=None)

task = aws.emr.InstanceGroup(TASK_NODE_NAME,
                             name=TASK_NODE_NAME,
                             cluster_id=aws_emr_cluster.id,
                             instance_count=TASK_NODE_INSTANCE_COUNT,
                             instance_type=TASK_NODE_TYPE)
