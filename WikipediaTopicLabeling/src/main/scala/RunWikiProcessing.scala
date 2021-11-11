import org.apache.spark.ml.feature.{CountVectorizerModel, Word2VecModel}
import org.apache.spark.sql.{SparkSession,Row}
import org.apache.spark.sql.functions._

object RunWikiProcessing {

    val CLOUD_ENV = false // false: run in local, true: run in cloud cluster

    object TopicType extends Enumeration {
        type ClusteringType = Value
        val Occurence, Word2Vec, LDA, Classification = Value
    }

    def main(args: Array[String]) {

        //        println("load dataset...")
        //        // val dataset = spark.sqlContext.read.json("s3a://bda-wiki-bucket/wiki.json")
        //        // val dataset = spark.sqlContext.read.json("s3a://bda-wiki-bucket/wiki_min.json")
        //        //        val dataset = spark.sqlContext.read.json("data/wiki_small.json")


        val spark = SparkSession.builder
          .appName("Wikipedia Topic Labeling")
          .master("local[*]")
          .getOrCreate()

        spark.sparkContext.setLogLevel("ERROR")

        val topicType = TopicType.LDA
        val train = true

        if (topicType != TopicType.Classification) {
            val path = if (CLOUD_ENV) "s3a://bda-wiki-bucket/abstract.json" else "data/abstract.json"

            val dataset = spark.sqlContext
                .read.json(path)
                .filter(row => row(0) == null) // remove corrupted records
                .drop("_corrupt_record")

            val wikiProcessing = new WikiUnsupervisedProcessing(spark, train)

            println("preprocess dataset...")
            val (preprocessingModel, preprocessedData) = wikiProcessing.preprocessing(dataset)

            topicType match {
                case TopicType.Occurence =>
                    println("train kmeans...")
                    val (kMeansModel, kMeansData) = wikiProcessing.kMeans(preprocessedData,20)

                    println("compute results...")
                    wikiProcessing.showKMeansTopicByOcucrence(kMeansData, 20)

                case TopicType.Word2Vec =>
                    println("train kmeans...")
                    val (kMeansModel, kMeansData) = wikiProcessing.kMeans(preprocessedData, 15)

                    println("compute results...")
                    val word2VecModel = preprocessingModel.stages(3).asInstanceOf[Word2VecModel]
                    wikiProcessing.showKMeansTopicLabeling(word2VecModel.getVectors, kMeansModel.clusterCenters, kMeansData)

                case TopicType.LDA =>
                    print("LDA clustering...")
                    val vocabulary = preprocessingModel.stages(2).asInstanceOf[CountVectorizerModel].vocabulary
                    val ldaResult = wikiProcessing.lda(spark, preprocessedData, vocabulary)

                    for (i <- 0 until 15) {
                        println(ldaResult.filter(s"cluster == $i").show(false))
                    }
            }
        } else {
            println("Classification...")
            val path = if (CLOUD_ENV) "s3a://bda-wiki-bucket/categories.json" else "data/categories.json"

            val dataset = spark.sqlContext
                .read.json(path)

            val wikiSupervisedProcessing = new WikiSupervisedProcessing(spark, train)

            val categoriesDF = wikiSupervisedProcessing.extractCategories(dataset)

            println(categoriesDF.show(false))
            println(categoriesDF.select("categories").distinct().count())
            println(dataset.select("categories").count())
        }

        spark.stop()
    }
}
