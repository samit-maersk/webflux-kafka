package com.example.webfluxkafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    private final Producer producer;

    @Autowired
    public TestController(Producer producer) {
        this.producer = producer;
    }
    @PostMapping("/publish")
    public void messageToTopic(@RequestParam("message") String message){

        this.producer.sendMessage(message);

    }
    @PostMapping("/publish/json")
    public void jsonMessageToTopic(@RequestBody MessageRequest messageRequest){

        this.producer.sendJsonMessage(messageRequest);

    }

    @PostMapping("/publish/avro")
    public void avroMessageToTopic(@RequestBody MessageRequest messageRequest){

        this.producer.sendAvroMessage(messageRequest);

    }
}
