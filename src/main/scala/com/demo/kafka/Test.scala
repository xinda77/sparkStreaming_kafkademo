package com.demo.kafka

import org.apache.log4j.{Level, Logger}
import org.apache.spark.ml.feature.StringIndexer
import org.apache.spark.sql.SparkSession
import org.apache.spark.mllib.clustering.KMeans
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.sql._
import org.apache.spark.ml.feature.Binarizer
import org.apache.spark.ml.feature.{CountVectorizer, CountVectorizerModel}
import org.apache.spark.ml.feature.Interaction
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.feature.Word2Vec
import org.apache.spark.ml.feature.{RegexTokenizer, Tokenizer}
import org.apache.spark.sql.functions._
import org.apache.spark.ml.linalg.Vector
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.linalg.Vectors
import org.apache.spark.ml.feature.ElementwiseProduct
import org.apache.spark.ml.linalg.Vectors
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.linalg.{Vector, Vectors}
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.sql.Row

object Test {
  
   def main(args:Array[String]) {
     System.setProperty("hadoop.home.dir", "E:\\software\\hadoop-2.7.7")
     Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
     Logger.getLogger("org.apache.eclipse.jetty.server").setLevel(Level.OFF)
     val spark = SparkSession
       .builder
       .appName("sparkApp")
       .master("spark://172.16.48.133:7077")
       .config("sparkDriver", "172.16.48.133")
       .getOrCreate()
     spark.sparkContext.addJar("E:\\git\\kafkaTest\\target\\kafkaTest-1.0-SNAPSHOT-jar-with-dependencies.jar")

     // Prepare training data from a list of (label, features) tuples.
     val training = spark.createDataFrame(Seq(
       (1.0, Vectors.dense(0.0, 1.1, 0.1)),
       (0.0, Vectors.dense(2.0, 1.0, -1.0)),
       (0.0, Vectors.dense(2.0, 1.3, 1.0)),
       (1.0, Vectors.dense(0.0, 1.2, -0.5))
     )).toDF("label", "features")

     // Create a LogisticRegression instance. This instance is an Estimator.
     val lr = new LogisticRegression()
     // Print out the parameters, documentation, and any default values.
     println("LogisticRegression parameters:\n" + lr.explainParams() + "\n")

     // We may set parameters using setter methods.
     lr.setMaxIter(10)
       .setRegParam(0.01)

     // Learn a LogisticRegression model. This uses the parameters stored in lr.
     val model1 = lr.fit(training)
     // Since model1 is a Model (i.e., a Transformer produced by an Estimator),
     // we can view the parameters it used during fit().
     // This prints the parameter (name: value) pairs, where names are unique IDs for this
     // LogisticRegression instance.
     println("Model 1 was fit using parameters: " + model1.parent.extractParamMap)

     // We may alternatively specify parameters using a ParamMap,
     // which supports several methods for specifying parameters.
     val paramMap = ParamMap(lr.maxIter -> 20)
       .put(lr.maxIter, 30)  // Specify 1 Param. This overwrites the original maxIter.
       .put(lr.regParam -> 0.1, lr.threshold -> 0.55)  // Specify multiple Params.

     // One can also combine ParamMaps.
     val paramMap2 = ParamMap(lr.probabilityCol -> "myProbability")  // Change output column name.
     val paramMapCombined = paramMap ++ paramMap2

     // Now learn a new model using the paramMapCombined parameters.
     // paramMapCombined overrides all parameters set earlier via lr.set* methods.
     val model2 = lr.fit(training, paramMapCombined)
     println("Model 2 was fit using parameters: " + model2.parent.extractParamMap)

     // Prepare test data.
     val test = spark.createDataFrame(Seq(
       (1.0, Vectors.dense(-1.0, 1.5, 1.3)),
       (0.0, Vectors.dense(3.0, 2.0, -0.1)),
       (1.0, Vectors.dense(0.0, 2.2, -1.5))
     )).toDF("label", "features")

     // Make predictions on test data using the Transformer.transform() method.
     // LogisticRegression.transform will only use the 'features' column.
     // Note that model2.transform() outputs a 'myProbability' column instead of the usual
     // 'probability' column since we renamed the lr.probabilityCol parameter previously.
     model2.transform(test)
       .select("features", "label", "myProbability", "prediction")
       .collect()
       .foreach { case Row(features: Vector, label: Double, prob: Vector, prediction: Double) =>
         println(s"($features, $label) -> prob=$prob, prediction=$prediction")
       }
      
/*  val dataset = spark.createDataFrame(
  Seq((0, 18, 1.0, Vectors.dense(0.0, 10.0, 0.5), 1.0))
).toDF("id", "hour", "mobile", "userFeatures", "clicked")

val assembler = new VectorAssembler()
  .setInputCols(Array("hour", "mobile", "userFeatures"))
  .setOutputCol("features")

val output = assembler.transform(dataset)
println("Assembled columns 'hour', 'mobile', 'userFeatures' to vector column 'features'")
output.select("features", "clicked").show(false)
*/
      
 /*     val documentDF = spark.createDataFrame(Seq(
  "Hi I heard about Spark".split(" "),
  "I wish Java could use case classes".split(" "),
  "Logistic regression models are neat".split(" ")
).map(Tuple1.apply)).toDF("text")

// Learn a mapping from words to Vectors.
val word2Vec = new Word2Vec()
  .setInputCol("text")
  .setOutputCol("result")
  .setVectorSize(3)
  .setMinCount(0)
val model = word2Vec.fit(documentDF)

val result = model.transform(documentDF)
result.collect().foreach { case Row(text: Seq[_], features: Vector) =>
  println(s"Text: [${text.mkString(", ")}] => \nVector: $features\n") }
      */
      
  /*   val df = spark.createDataFrame(Seq(
      (0, Array("a", "b", "c")),
      (1, Array("a", "b", "b", "c", "a"))
       )).toDF("id", "words")
    
    // fit a CountVectorizerModel from the corpus
    val cvModel: CountVectorizerModel = new CountVectorizer()
      .setInputCol("words")
      .setOutputCol("features")
      //.setVocabSize(3)
      //.setMinDF(2)
      .fit(df)*/
    
    /*// alternatively, define CountVectorizerModel with a-priori vocabulary
    val cvm = new CountVectorizerModel(Array("a", "b", "c"))
      .setInputCol("words")
      .setOutputCol("vector")*/
    
  //  cvModel.transform(df).show(false)
      
      
     /* val data = Array((0, 0.1), (1, 0.8), (2, 0.2))
      val dataFrame = spark.createDataFrame(data).toDF("id", "feature")
      
      val binarizer: Binarizer = new Binarizer()
        .setInputCol("feature")
        .setOutputCol("binarized_feature")
        .setThreshold(0.5)
      
      val binarizedDataFrame = binarizer.transform(dataFrame)
      
      println(s"Binarizer output with Threshold = ${binarizer.getThreshold}")
      binarizedDataFrame.show()
      */
    /* val df = spark.createDataFrame(
      Seq((0, "a"), (1, "b"), (2, "c"), (3, "a"), (4, "a"), (5, "c"))
      ).toDF("id", "category")
      
      val indexer = new StringIndexer()
        .setInputCol("category")
      .setOutputCol("categoryIndex")
      
      val indexed = indexer.fit(df).transform(df)
      indexed.show()*/
 /*     
      val df = spark.createDataFrame(Seq(
  (1, 1, 2, 3, 8, 4, 5),
  (2, 4, 3, 8, 7, 9, 8),
  (3, 6, 1, 9, 2, 3, 6),
  (4, 10, 8, 6, 9, 4, 5),
  (5, 9, 2, 7, 10, 7, 3),
  (6, 1, 1, 4, 2, 8, 4)
)).toDF("id1", "id2", "id3", "id4", "id5", "id6", "id7")

val assembler1 = new VectorAssembler().
  setInputCols(Array("id2", "id3", "id4")).
  setOutputCol("vec1")

val assembled1 = assembler1.transform(df)

val assembler2 = new VectorAssembler().
  setInputCols(Array("id5", "id6", "id7")).
  setOutputCol("vec2")

val assembled2 = assembler2.transform(assembled1).select("id1", "vec1", "vec2")

val interaction = new Interaction()
  .setInputCols(Array("id1", "vec1", "vec2"))
  .setOutputCol("interactedCol")

val interacted = interaction.transform(assembled2)

interacted.show(truncate = false)*/
      
/*   val sentenceDataFrame = spark.createDataFrame(Seq(
  (0, "Hi,I,heard,about,Spark"),
  (1, "I,wish,Java,could,use,case,classes"),
  (2, "Logistic,regression,models,are,neat")
)).toDF("id", "sentence")

val tokenizer = new Tokenizer().setInputCol("sentence").setOutputCol("words")
val regexTokenizer = new RegexTokenizer()
  .setInputCol("sentence")
  .setOutputCol("words")
  .setPattern("\\W") // alternatively .setPattern("\\w+").setGaps(false)

val countTokens = udf { (words: Seq[String]) => words.length }

val tokenized = tokenizer.transform(sentenceDataFrame)
tokenized.select("id", "words").show(false)*/

/*val regexTokenized = regexTokenizer.transform(sentenceDataFrame)
regexTokenized.select("sentence", "words")
    .withColumn("tokens", countTokens(col("words"))).show(false)   */
      
// Create some vector data; also works for sparse vectors
val dataFrame = spark.createDataFrame(Seq(
  ("a", Vectors.dense(1.0, 2.0, 3.0)),
  ("b", Vectors.dense(4.0, 5.0, 6.0)))).toDF("id", "vector")

val transformingVector = Vectors.dense(0.0, 1.0, 2.0)
val transformer = new ElementwiseProduct()
  .setScalingVec(transformingVector)
  .setInputCol("vector")
  .setOutputCol("transformedVector")

// Batch transform the vectors to create new column:
transformer.transform(dataFrame).show()
      
   }
  
}