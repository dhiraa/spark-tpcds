"""An AWS Python Pulumi program"""

import json
import pulumi
import pulumi_aws as aws
from pulumi_aws import s3
from config import *


# EMR provisioning
#=======================================================================================================================

# Networking
# ----------------------------------------------------------------------------------------------------------------------
main_vpc = aws.ec2.Vpc(VPC_NAME,
                       cidr_block="173.31.0.0/16",
                       enable_dns_hostnames=True,
                       tags=TAGS)

main_subnet = aws.ec2.Subnet(SUBNET_NAME,
                             vpc_id=main_vpc.id,
                             cidr_block="173.31.0.0/20",
                             availability_zone="ap-south-1b",
                             tags=TAGS)

allow_access=aws.ec2.SecurityGroup(resource_name="allowAccessPulumi",
                                   description="Allow inbound traffic",
                                   vpc_id=main_vpc.id,
                                   ingress=[aws.ec2.SecurityGroupIngressArgs(
                                       from_port=22,
                                       to_port=22,
                                       protocol="TCP",
                                       cidr_blocks=[CURRENT_MACHINE_IP+"/32"] #main_vpc.cidr_block,
                                   )],
                                   tags=TAGS,
                                   opts=pulumi.ResourceOptions(depends_on=[main_subnet]))


gw = aws.ec2.InternetGateway(GATEWAY_NAME, vpc_id=main_vpc.id)
route_table = aws.ec2.RouteTable(ROUTE_TABLE_NAME,
                                 vpc_id=main_vpc.id,
                                 routes=[aws.ec2.RouteTableRouteArgs(
                                     cidr_block="0.0.0.0/0",
                                     gateway_id=gw.id,
                                 )])

main_route_table_association = aws.ec2.MainRouteTableAssociation(MAIN_ROUTE_TABLE_ASSOCIATION_NAME,
                                                                 vpc_id=main_vpc.id,
                                                                 route_table_id=route_table.id)

# Cluster
# ----------------------------------------------------------------------------------------------------------------------

aws_emr_cluster = aws.emr.Cluster(CLUSTER_NAME,
                                  release_label=EMR_RELEASE_LABEL,
                                  applications=["Spark", "Hadoop", "Ganglia", "Zeppelin"],
                                  ec2_attributes=aws.emr.ClusterEc2AttributesArgs(
                                      instance_profile="EMR_EC2_DefaultRole",#emr_profile.arn,
                                      subnet_id=main_subnet.id,
                                      key_name=PEM_FILE_KEY_NAME),
                                  master_instance_group=aws.emr.ClusterMasterInstanceGroupArgs(
                                      instance_type=MASTER_NODE_TYPE,
                                      name="master_nodes"),
                                  core_instance_group=aws.emr.ClusterCoreInstanceGroupArgs(
                                      instance_type=CORE_NODE_TYPE,
                                      instance_count=1,
                                      ebs_configs=[{
                                          "size": EBS_VOLUME,
                                          "type": "gp2",
                                          "volumesPerInstance": 1,
                                      }],
                                  ),
                                  service_role='EMR_DefaultRole', #iam_emr_service_role.arn,
                                  log_uri=LOG_URI,
                                  steps=None)

task = aws.emr.InstanceGroup("task",
                             name="mageswaran_test_task_pulumi",
                             cluster_id=aws_emr_cluster.id,
                             instance_count=1,
                             instance_type="m4.large")
