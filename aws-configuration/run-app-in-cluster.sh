#!/bin/bash

# Source env file if it exist
if [ -f .env ]; then
    source .env
fi

# Get parameters
CLUSTER_NAME=$1 # flintrock name of the cluster
CLUSTER_MASTER_DNS_NAME=$2 # DNS name of the master

# Copy packaged app to cluster
flintrock copy-file $CLUSTER_NAME ../WikipediaTopicLabeling/target/scala-2.11/wikipedia-topic-project_2.11-1.0.jar /home/ec2-user/

# Launch app in cluster
spark-submit \
  --class RunWikiProcessing \
  --master spark://$CLUSTER_MASTER_DNS_NAME:7077 \
  --deploy-mode cluster \
  --conf "spark.jars.packages=com.amazonaws:aws-java-sdk:1.7.4,org.apache.hadoop:hadoop-aws:2.7.7" \
  /home/ec2-user/wikipedia-topic-project_2.11-1.0.jar

echo "Go to http://$CLUSTER_MASTER_DNS_NAME:8080 to view the app running."
