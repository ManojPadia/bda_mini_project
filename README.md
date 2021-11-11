### Requirements

You need the following tools installed on your local machine.

- awscli (`pip install --user awscli`)
- Flintrock (`pip install --user flintrock`)

This tools need your AWS access key to work. So create a file at `~/.aws/credentials` and put the following inside:

```sh
[default]
aws_access_key_id=your_aws_access_key
aws_secret_access_key=associated_secret_access_key
```

### Upload dataset on S3

Use aws-cli to upload the dataset and the stopwords file into a S3 bucket.

```bash
aws s3 mb s3://bda-wiki-bucket
aws s3 cp /path/to/stopwords.txt s3://bda-wiki-bucket
aws s3 cp /path/to/abstract.json s3://bda-wiki-bucket
aws s3 cp /path/to/categories.json s3://bda-wiki-bucket
```

### Configure Flintrock

Copy `flintrock-config-example.yaml` to `flintrock-config.yaml` and update fields corresponding to your account.

```yml
services:
  spark:
    version: 2.4.3
    download-source: "https://archive.apache.org/dist/spark/spark-{v}/spark-{v}-bin-hadoop2.7.tgz"

provider: ec2

providers:
  ec2:
    key-name: aws-bda-project # key pair name in aws
    identity-file: "/path/to/private_key.pem"
    instance-type: m3.medium # Choose EC2 flavor
    region: us-east-1
    ami: ami-0b8d0d6ac70e5750c # Amazon Linux 2, us-east-1
    user: ec2-user
    vpc-id: vpc-dc7caea6 # your VPC id
    subnet-id: subnet-42fe2b7c # one of your subnet id
    security-groups:
      - only-ssh-from-anywhere # security-group name that allow ssh (22) from anywhere (0.0.0.0/0)
    instance-profile-name: Spark_S3 # IAM Role with AmazonS3FullAccess policy
    tenancy: default
    ebs-optimized: no
    instance-initiated-shutdown-behavior: terminate

launch:
  num-slaves: 1 # Choose number of slaves

debug: false
```

This config file will be read by the script `create-cluster.sh`.

### Launch and configure the cluster

Launch your cluster using the script `create-cluster.sh`

```bash
cd aws-configuration
./create-cluster.sh bda-wiki-cluster
```

This script will create and configure the cluster. Then it will download some required jars directly in the cluster instances.
This jars are required to access your dataset stored on S3 from Spark.

### Package the program

First, you need to package the program in a jar file that you will upload on the cluster. You can use sbt in command line or compile your project from your IDE.

```bash
cd /path/to/WikipediaTopicLabeling
sbt package
```

This will generate a jar file in `./target/scala-2.11/wikipedia-topic-project_2.11-1.0.jar`

### Submit a job to the cluster

To submit a job, you need to know the DNS name of the master node of your cluster. It has been shown by the `create-cluster.sh` script. But you can also get it with:

```bash
$ flintrock describe
Found 1 cluster in region us-east-1.
---
bda-wiki-cluster:
  state: running
  node-count: 2
  master: ec2-100-25-152-130.compute-1.amazonaws.com
  slaves:
    - ec2-54-152-129-154.compute-1.amazonaws.com
```

Here it is `ec2-100-25-152-130.compute-1.amazonaws.com`

Then you can use the script `run-app-in-cluster.sh` to upload the packaged app and submit a new job. It requires 2 arguments which are the cluster name and the master DNS name.

:warning: Spark bin folder needs to be in your PATH. You can update your PATH variable by adding `export PATH=$PATH:/path/to/spark-2.4.3-bin-hadoop2.7/bin` in `.env` file in the `aws-configuration` folder, which will be sourced automatically by running the following script.

```bash
$ cd aws-configuration
$ ./run-app-in-cluster.sh bda-wiki-cluster ec2-100-25-152-130.compute-1.amazonaws.com
[...]
Go to http://ec2-100-25-152-130.compute-1.amazonaws.com:8080 to view the app running.
```

As the script say, you can then follow your app execution via spark web interface at the given address.