#!/bin/sh

sbt package
spark-submit --packages JohnSnowLabs:spark-nlp:2.0.4 --class "WikipediaTopicLabeling" --master local[*] target/scala-2.11/wikipedia-topic-project_2.11-1.0.jar
