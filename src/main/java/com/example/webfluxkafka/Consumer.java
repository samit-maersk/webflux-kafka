package com.example.webfluxkafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class Consumer {

    @Value("${spring.kafka.topic}")
    private String topicName;


    @KafkaListener(topics = "topic-1", groupId = "sample-group")
    public void consumeMessage(String message) {

        System.out.println("message received" + " " + message);
    }


    @KafkaListener(topics = "topic-1", groupId = "sample-group", containerFactory = "messageListener")
    public void consumeJsonMessage(MessageRequest message) {

        System.out.println("message received" + " " + message);
    }


}
