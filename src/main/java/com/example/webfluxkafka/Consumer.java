package com.example.webfluxkafka;

import com.example.avro.Person;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
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

        System.out.println("message received" + " " + message.getMessage());
    }

    //avro not working here its in spring-kafka-registry

 /*   @KafkaListener(topics = "topic-1", groupId = "sample-group")
    public void consume(Person person) {
        System.out.println("Consumed message: " + person.getName() + ", " + person.getAge());
    }*/

/*    @KafkaListener(topics = "topic-1", groupId = "sample-group", containerFactory = "kafkaListenerContainerFactory")
    public void listen( @Payload Person person) {
        System.out.println("Consumed message: " + person.getName() + ", " + person.getAge());
    }*/
}
