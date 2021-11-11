import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.ml.feature.RegexTokenizer
import org.apache.spark.sql.functions.count
import org.apache.spark.sql.{DataFrame, Row, SparkSession}


class WikiSupervisedProcessing(spark: SparkSession, train: Boolean) {

    import spark.implicits._

    def extractCategories(dataset: DataFrame): DataFrame = {
        val categories = dataset.flatMap {
            case Row(categories: Seq[String], title: String) =>
                categories
        }.toDF("categories")

        categories
          .groupBy("categories")
          .agg(count($"categories") as "count")
          .orderBy($"count".desc)
    }
}
