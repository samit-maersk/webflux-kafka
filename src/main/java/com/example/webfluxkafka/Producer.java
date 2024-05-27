package com.example.webfluxkafka;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class Producer {
    private static final String TOPIC = "topic-1";
    @Autowired
    private KafkaTemplate<String,String> kafkaTemplate;

/*    @Autowired
    private KafkaTemplate<String,MessageRequest> kafkaTemplateJson;*/

/*    @Autowired
    private KafkaTemplate<String,MessageRequest> kafkaTemplateAvro;*/

    public void sendMessage(String message){

        this.kafkaTemplate.send(TOPIC,message);
        System.out.println("message sent"+ " " + message);
    }
    public void sendJsonMessage(MessageRequest message){

        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        ProducerFactory<String, MessageRequest> producerFactory = new DefaultKafkaProducerFactory<>(configProps);
        KafkaTemplate<String, MessageRequest> kafkaTemplateJson = new KafkaTemplate<>(producerFactory);


        kafkaTemplateJson.send(TOPIC,message);
        System.out.println("message sent"+ " " + message.getMessage());
    }
    public void sendAvroMessage(MessageRequest message){

        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);

        ProducerFactory<String, MessageRequest> producerFactory = new DefaultKafkaProducerFactory<>(configProps);
        KafkaTemplate<String, MessageRequest> kafkaTemplateAvro = new KafkaTemplate<>(producerFactory);


        kafkaTemplateAvro.send(TOPIC,message);
        System.out.println("message sent"+ " " + message.getMessage());
    }

}
