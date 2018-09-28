package com.demo.kafka;

import kafka.utils.ShutdownableThread;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.Collections;
import java.util.Map;

public class Consumer extends ShutdownableThread {
    private KafkaConsumer<String, String> consumer;
    private  String topic;
    private Map<String,Object> props;

   public Consumer(String topic,Map<String,Object> props) {
       super("KafkaConsumer", false);
       consumer = new KafkaConsumer<String, String>(props);
       this.topic = topic;
   }
   @Override
    public void doWork() {
        consumer.subscribe(Collections.singletonList(this.topic));
       ConsumerRecords<String, String> records = consumer.poll(100);
       for (ConsumerRecord<String, String> record : records) {
           System.out.println(Thread.currentThread()+"Received message: (" + record.key() + ", " + record.value() + ") at offset " + record.offset());
       }
    }
    @Override
    public boolean isInterruptible() {
        return false;
    }
}
