package com.example.webfluxkafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.Map;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

@SpringBootApplication
@Slf4j
@RequiredArgsConstructor
public class WebfluxKafkaApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebfluxKafkaApplication.class, args);
	}

	@Value("${spring.kafka.topic}")
	public String topicName;
	final KafkaTemplate<String, String> stringKafkaTemplate;
	final MessageRepository messageRepository;


	//Router
	@Bean
	RouterFunction<ServerResponse> routes() {
		return RouterFunctions
				.route()
				.POST("/send-message", accept(MediaType.TEXT_PLAIN), request -> {
					log.info("Sending Text message to kafka topic {}", topicName);
					return request.bodyToMono(String.class)
							.doOnNext(message -> stringKafkaTemplate.send(topicName, message))
							.doOnSuccess(s -> log.info("Text Message sent to kafka"))
							.doOnError(e -> log.error("error while sending Text message to kafka", e))
							.then(ServerResponse.ok().bodyValue(Map.of("message", "SUCCESS")));
				})
				.GET("/db", request -> ServerResponse.ok().body(messageRepository.findAll(), Message.class))
				.after((request, response) -> {
					log.info("{} {} {}",request.method(), request.path(), response.statusCode());
					request.headers().asHttpHeaders().forEach((k, v) -> log.info("{}: {}", k, v));
					return response;
				})
				.build();
	}

}

@Configuration
class KafkaConfigurations {

	// Note - Topic creation in real time project is not recommended as each organisation has its own topic creation strategy in a automated way.
	@Bean
	public NewTopic createTopic(@Value("${spring.kafka.topic}") String textTopicName) {
		return TopicBuilder.name(textTopicName).partitions(1).replicas(1).build();
	}
}

@Component
@Slf4j
@RequiredArgsConstructor
class KafkaConsumer {
	final MessageRepository messageRepository;

	@KafkaListener(topics = "${spring.kafka.topic}")
	public void processTextMessage(@Payload(required = false) String message/*, @Header(KafkaHeaders.RECEIVED_KEY) String key*/) {
		//log.info("message consumed from kafka , message : {}, header : {} ", content, key);
		log.info("[*] Received Text Message {}", message);
		messageRepository.save(new Message(null, message))
				.doOnSuccess(m -> log.info("Text message saved to mongo"))
				.doOnError(e -> log.error("error while saving Text message to mongo", e))
				.subscribe();
	}
}

@Repository
interface MessageRepository extends ReactiveCrudRepository<Message, String> {}

@Document
record Message(@Id String id, String message) {}