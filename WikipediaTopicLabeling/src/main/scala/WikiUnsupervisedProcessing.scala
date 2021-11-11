import org.apache.spark.ml.clustering.{KMeans, KMeansModel, LDA, LDAModel, LocalLDAModel}
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.ml.feature.{CountVectorizer, RegexTokenizer, StopWordsRemover, Word2Vec}
import org.apache.spark.ml.linalg.Vector
import org.apache.spark.sql.functions.{count, desc, explode, size}
import org.apache.spark.sql.{DataFrame, Row, SparkSession}

class WikiUnsupervisedProcessing(spark: SparkSession, train: Boolean) {

    import spark.implicits._

    def preprocessing(dataset: DataFrame): (PipelineModel, DataFrame) = {

        val preprocessingModel = if (train) {
            val tokenizer = new RegexTokenizer()
              .setInputCol("text")
              .setOutputCol("token")
              .setPattern("\\W")
              .setMinTokenLength(4)

            val path = if (RunWikiProcessing.CLOUD_ENV) "s3a://bda-wiki-bucket/stopwords.txt" else "data/stopwords.txt"
            val stopwords = spark.read.text(path).collect.map(row => row.getString(0))

            val stopWordRemover = new StopWordsRemover()
              .setInputCol("token")
              .setOutputCol("stop_words")
              .setStopWords(stopwords)

            val countVectorizer = new CountVectorizer()
              .setInputCol("stop_words")
              .setOutputCol("features")

            val word2Vec = new Word2Vec()
              .setInputCol("stop_words")
              .setOutputCol("word2Vec")

            val pipeline = new Pipeline()
              .setStages(Array(
                  tokenizer,
                  stopWordRemover,
                  countVectorizer,
                  word2Vec
              ))

            val preprocessingModel = pipeline.fit(dataset)
            preprocessingModel.write.overwrite.save("models/preprocessing")
            preprocessingModel
        } else {
            PipelineModel.load("models/preprocessing")
        }
        // keep only the articles which have an abstract describing them with more than 20 words
        (preprocessingModel, preprocessingModel.transform(dataset).where(size($"stop_words") > 20))
    }


    def kMeans(dataset: DataFrame, K: Int): (KMeansModel, DataFrame) = {
        val kMeansModel = if (train) {
            val kMeans = new KMeans().setK(K).setSeed(1L).setFeaturesCol("word2Vec")
            val kMeansModel = kMeans.fit(dataset)
            kMeansModel.write.overwrite.save("models/kmeans")
            kMeansModel
        } else KMeansModel.load("models/kmeans")

        (kMeansModel, kMeansModel.transform(dataset).select("title", "stop_words", "word2Vec", "prediction"))
    }

    def showKMeansTopicLabeling(word2VecVectors: DataFrame, clusterCenters: Array[Vector], kMeansData: DataFrame): Unit = {
        val wordClusterSimilarity = computeWordClusterSimilarity(word2VecVectors, clusterCenters)

        println("compute clusters description...")
        val clusterDescriptions = (for (i <- 0 to clusterCenters.length) yield getClusterDescription(i, wordClusterSimilarity)).toArray

        println("compute final result...")
        val result = spark.createDataFrame(kMeansData.select("title", "prediction").collect().map {
            case Row(title: String, prediction: Int) => (title, prediction, clusterDescriptions(prediction))
        })

        for (i <- 0 to clusterCenters.length) {
            println(result.filter(s"_2 == $i").show(false))
        }
    }

    def showKMeansTopicByOcucrence(dataset: DataFrame, K: Int): Unit = {
        for (i <- 0 until (K-1)) {
            println(s"Topic $i:")
            println(dataset.where(s"prediction == $i").withColumn("token_split", explode($"stop_words"))
              .groupBy($"token_split")
              .agg(count($"title") as "count")
              .orderBy($"count".desc)
              .show(false))
            println(dataset.where(s"prediction == $i").select("title").show(false))
        }
    }

    def lda(spark: SparkSession, dataset: DataFrame, vocabulary: Array[String]): DataFrame = {
        import spark.implicits._

        val ldaModel = if (train) {
            val lda = new LDA().setK(15).setMaxIter(100)
            val ldaModel = lda.fit(dataset)
            ldaModel.write.overwrite.save("models/lda")
            ldaModel
        } else {
            println("load lda model...")
            LocalLDAModel.load("models/lda")
        }

        println
        println(s"preplexity: ${ldaModel.logPerplexity(dataset)}")
        println

        val topics = getLDATopics(spark, ldaModel, vocabulary, print = true)

        val ldaDataset = ldaModel.transform(dataset).select("title", "topicDistribution")

        ldaDataset.map {
            case Row(title: String, topicDistribution: Vector) =>
                (title, topicDistribution, topics(topicDistribution.argmax).map(_._1), topicDistribution.argmax)
        }.toDF("title", "topicDistribution", "topicWords", "cluster")
    }


    /** *********************/
    /*  Private            */
    /** *********************/

    private def computeWordClusterSimilarity(word2VecVectors: DataFrame, clusterCenters: Array[Vector]): DataFrame = {
        println("compute word <-> cluster similarity")
        word2VecVectors.map {
            case Row(word: String, v1: Vector) =>
                val clusterSimilarity = clusterCenters.toVector.zipWithIndex.map {
                    case (v2: Vector, i: Int) => (Metrics.cosineSimilarity(v1.toArray, v2.toArray), i)
                }.max
                (word, clusterSimilarity._2, clusterSimilarity._1)
        }.toDF("word", "cluster", "similarity")

    }

    private def getClusterDescription(clusterId: Int, wordClusterSimilarity: DataFrame): Array[String] = {
        wordClusterSimilarity
          .select("word")
          .filter(s"cluster == $clusterId")
          .orderBy(desc("similarity"))
          .limit(5)
          .collect
          .map(_.getString(0))
    }

    private def getLDATopics(spark: SparkSession, ldaModel: LDAModel, vocabulary: Array[String], print: Boolean = false): Array[Seq[(String, Double)]] = {
        // src: https://github.com/karenyyy/Coursera_and_Udemy/blob/2e93a401868ff25900ff83550c08c16588ac2267/ \
        //      Big_Data_coursera/Exercises/notebooks/ScalaMachineLearningProjects_Code/Chapter05/TopicModelling/ \
        //      topicModellingwithLDA.scala

        import spark.implicits._

        //  topics:
        // +------+--------+
        // | term | weight |
        // +------+--------+
        // | [..] |  [..]  |
        // +------+--------+
        val topicIndices = ldaModel.describeTopics(10)
        val topics = topicIndices.map {
            case Row(topic: Int, termIndices: Seq[Int], termWeights: Seq[Double]) =>
                termIndices.zip(termWeights).map {
                    case (termIndice: Int, termWeight: Double) => (vocabulary(termIndice.toInt), termWeight)
                }
        }.collect

        if (print)
            topics.zipWithIndex.foreach {
                case (topic, i) => println(s"topic $i: ${topic.toString()}")
            }
        println

        topics
    }
}
