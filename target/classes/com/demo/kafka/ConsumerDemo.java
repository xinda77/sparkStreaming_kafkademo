package com.demo.kafka;
import java.util.HashMap;
import java.util.Map;

public class ConsumerDemo {

    public static void main(String[] args){

        String topic = "demo";
        Map<String, Object> props=getPropertis("group1");
        Consumer r1=new Consumer(topic,props);
        Consumer r2=new Consumer(topic,props);
        r1.start();
        r2.start();


    }

    public static Map<String, Object> getPropertis(String group) {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("bootstrap.servers", "172.16.48.133:9092");
        props.put("group.id", group);
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("session.timeout.ms", "30000");
        props.put("key.deserializer",
                "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer",
                "org.apache.kafka.common.serialization.StringDeserializer");
        return props;
    }


}
