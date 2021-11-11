object Metrics {

    def cosineSimilarity(v1: Array[Double], v2: Array[Double]): Double = {
        val numerator = v1.zip(v2).foldRight(0.0)((elem, acc) => acc + elem._1 * elem._2)
        val denominator =
            math.sqrt(v1.foldRight(0.0)((elem, acc) => acc + elem * elem)) *
              math.sqrt(v2.foldRight(0.0)((elem, acc) => acc + elem * elem))
        numerator / denominator
    }

    def l2Norm(v1: Array[Double], v2: Array[Double]): Double = {
        math.sqrt(v1.zip(v2).foldRight(0.0)(
            (elem, acc) => acc + (elem._1 - elem._2) * (elem._1 - elem._2))
        )
    }
}
