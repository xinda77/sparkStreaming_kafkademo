package com.demo.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class ProducerDemo {

    public static void main(String[] args){

        //Assign topicName to string variable
        String topicName = "demo";

        // create instance for properties to access producer configs
        Properties props = new Properties();

        //Assign localhost id
        props.put("bootstrap.servers", "172.16.48.133:9092");

        //If the request fails, the producer can automatically retry,
        props.put("retries", 0);

        //Specify buffer size in config
        props.put("batch.size", 16384);

        //Reduce the no of requests less than 0
        props.put("linger.ms", 1);

        props.put("num.partitions",3);

        //The buffer.memory controls the total amount of memory available to the producer for buffering.
        props.put("buffer.memory", 33554432);

        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");

        Producer<String, String> producer = new KafkaProducer<String, String>(props);

        int i=0;

        while(true){
            try{
                producer.send(new ProducerRecord<String, String>(topicName,Integer.toString(i),"Message sent successfully"+Integer.toString(i)));
                Thread.sleep(10);
                i++;
            }
            catch (Exception e){
               break;
            }

        }
        System.out.println("Message sent successfully");
        producer.close();
    }

}
