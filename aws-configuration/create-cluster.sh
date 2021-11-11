#!/bin/bash

# Get parameters
CLUSTER_NAME=$1

# Create cluster using flintrock_config.yaml config file
# Then download required jars inside cluster
flintrock --config flintrock-config.yaml launch $CLUSTER_NAME && \
    flintrock run-command bda-wiki-cluster "wget -o spark/jars/hadoop-aws-2.7.7.jar http://central.maven.org/maven2/org/apache/hadoop/hadoop-aws/2.7.7/hadoop-aws-2.7.7.jar" && \
    flintrock run-command bda-wiki-cluster "wget -o spark/jars/aws-java-sdk-1.7.4.jar http://central.maven.org/maven2/com/amazonaws/aws-java-sdk/1.7.4/aws-java-sdk-1.7.4.jar" && \
    flintrock describe && \
    echo "Cluster started"
