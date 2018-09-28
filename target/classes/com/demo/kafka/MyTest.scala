package com.demo.kafka
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.{Seconds, StreamingContext}

import scala.collection.mutable

object MyTest {
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
    val url="jdbc:mysql://172.16.48.133:3306/kafka?user=root&password=123456"
    val jdbc=spark.read.format("jdbc").option("url",url)
    jdbc.option("driver", "com.mysql.jdbc.Driver").option("dbtable", "offset_test").load().createOrReplaceTempView("offset_test")
    import spark.sql
    val data=sql("select offset_ ,topic_,partition_ from offset_test where topic_='demo'").cache()
    var fromOffsets=mutable.Map.empty[TopicPartition,Long]
     val results=spark.sparkContext.broadcast(fromOffsets)
    data.foreachPartition{
      iter=>
      iter.foreach{
        s=> results.value+=(new TopicPartition(s.getAs[String]("topic_"),s.getAs[String]("partition_").toInt)->s.getAs[String]("offset_").toLong)
      }

      /*while(iter.hasNext){
        val s=iter.next()
        results.value+=(new TopicPartition(s.getAs[String]("topic_"),s.getAs[String]("partition_").toInt)->s.getAs[String]("offset_").toLong)
      }*/
    }
 /*   val results=data.collect()
    for(s<-results){
      fromOffsets+=(new TopicPartition(s.getAs[String]("topic_"),s.getAs[String]("partition_").toInt)->s.getAs[String]("offset_").toLong)
    }*/
     fromOffsets=results.value
    if(fromOffsets.isEmpty){
      fromOffsets+=(new TopicPartition("demo",0)->1234l)
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
