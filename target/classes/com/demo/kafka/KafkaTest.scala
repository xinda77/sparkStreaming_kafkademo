package com.demo.kafka
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.{Seconds, StreamingContext}

object KafkaTest {
  def main(args: Array[String]): Unit = {
    System.setProperty("hadoop.home.dir", "E:\\software\\hadoop-2.7.7")
    Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
    Logger.getLogger("org.apache.eclipse.jetty.server").setLevel(Level.OFF)
    val spark = SparkSession
      .builder
      .appName("sparkApp")
      .master("spark://172.16.48.133:7077")
      .config("sparkDriver", "172.16.48.133")
      .getOrCreate()
    val sql_select="select offset_ ,topic_,partition_ from offset_test where topic_='demo'"
    val conn = ConnectionPool.getConnection
    val data=conn.createStatement().executeQuery(sql_select)
    var fromOffsets:Map[TopicPartition,Long]=Map()
    while (data.next()){
      fromOffsets+=(new TopicPartition(data.getString("topic_"),data.getString("partition_").toInt)->data.getString("offset_").toLong)
    }
    if(fromOffsets.isEmpty){
      fromOffsets=Map(new TopicPartition("demo",0)->1234l)
    }
    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> "172.16.48.133:9092",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "group1",
      "auto.offset.reset" -> "latest",
      "enable.auto.commit" -> (false: java.lang.Boolean)
    )
    val sc = new StreamingContext(spark.sparkContext, Seconds(5))
    val line= KafkaUtils.createDirectStream[String, String](
      sc,
      PreferConsistent,
      ConsumerStrategies.Assign[String, String](fromOffsets.keys.toList, kafkaParams, fromOffsets))
      line.foreachRDD{
        rdd =>
          val offsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
          @transient val conn = ConnectionPool.getConnection
              offsetRanges.foreach{
                o=>println(s"${o.topic} ${o.partition} ${o.fromOffset} ${o.untilOffset}")
                  val topic_partition_key = o.topic + "_" + o.partition+"_"+o.untilOffset
                  val sql_update="delete from offset_test where topic_='"+o.topic+"' and partition_='"+o.partition.toString+"'"
                  conn.createStatement.executeUpdate(sql_update)
                  val sql = "insert into offset_test values (" +
                    "'"+topic_partition_key+"'," +
                    "'"+o.topic+"'," +
                    "'"+o.partition.toString+"'," +
                    "'"+o.untilOffset.toString+"'" +
                    ")"
                 conn.createStatement.executeUpdate(sql)
              }
          ConnectionPool.returnConnection(conn)
      }
    sc.start()
    sc.awaitTermination()
    sc.stop()
  }

}
