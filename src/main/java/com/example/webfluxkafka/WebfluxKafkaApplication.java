package com.example.webfluxkafka;

import com.example.webfluxkafka.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

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

	final KafkaTemplate<String, Message> kafkaTemplate;

	final MessageRepository messageRepository;

	// Note - Topic creation in real time project is not recommended as each organisation has its own topic creation strategy in an automated way.
	@Bean
	public NewTopic createTopic(@Value("${spring.kafka.topic}") String textTopicName) {
		return TopicBuilder.name(textTopicName).partitions(1).replicas(1).build();
	}

	// Note - This is just for demo purpose, in real time project we should not create topic and register schema in schema registry in this way.
	@Bean
	ApplicationListener<ApplicationReadyEvent> ready(@Value("classpath:avro/message-schema.avsc") Resource resource) {

		return event -> {
			log.info("Application started");
			try {
				var r = WebClient
						.builder()
						.baseUrl("http://localhost:8081")
						.build()
						.post()
						.uri("/subjects/" + topicName + "-value/versions")
						.contentType(MediaType.APPLICATION_JSON)
						.bodyValue(Map.of("schema", new String(resource.getContentAsByteArray())))
						.retrieve()
						.bodyToMono(Map.class)
						.block();

				System.out.println(new String(resource.getContentAsByteArray()));
				System.out.println(r);
			} catch (Exception e) {
				System.out.println("Error while reading schema file");
				System.out.println(e.getMessage());
			}
		};
	}


	//Router
	@Bean
	RouterFunction<ServerResponse> routes() {
		return RouterFunctions
				.route()
				.POST("/send-message", accept(MediaType.APPLICATION_JSON), request -> {
					log.info("Sending AVRO message to kafka topic {}", topicName);
					var uuid = UUID.randomUUID().toString();

					return request.bodyToMono(Message.class)
							.map(message -> kafkaTemplate.send(topicName, uuid , message))
							.doOnSuccess(cF -> log.info("AVRO Message sent to kafka {}", cF))
							.doOnError(cF -> log.error("error while sending AVRO message to kafka", cF))
							.map(cF -> Map.of("status", "success", "topic", topicName, "cF", cF))
							.flatMap(ServerResponse.ok()::bodyValue);
				})
				.GET("/db", request -> ServerResponse.ok().body(messageRepository.findAll(), DbMessage.class))
				.after((request, response) -> {
					log.info("{} {} {}",request.method(), request.path(), response.statusCode());
					request.headers().asHttpHeaders().forEach((k, v) -> log.info("{}: {}", k, v));
					return response;
				})
				.build();
	}
}

@Component
@Slf4j
@RequiredArgsConstructor
class KafkaConsumer {
	final MessageRepository messageRepository;

	//assume we have two consumer , do different things with the same message

	//Consumer-1
	@KafkaListener(topics = "${spring.kafka.topic}", groupId = "${spring.kafka.consumer.group-id}")
	public void processAvroMessage(@Payload Message message, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
		log.info("[*] Received Message [processAvroMessage] key: {}, value: {}", key, message);
	}

	//Consumer-2
	@KafkaListener(topics = "${spring.kafka.topic}", groupId = "${spring.kafka.consumer.group-id}-one")
	public void processAvroMessageV2(ConsumerRecord<String, Message> record) {
		var message = record.value();
		log.info("[*] Received Message [processAvroMessageV2] key: {}, value: {}", record.key(), message);

		messageRepository
				.save(new DbMessage(null, record.key(), message.getId(), message.getMessage()))
				.subscribe();
	}
}

@Repository
interface MessageRepository extends ReactiveMongoRepository<DbMessage, String> {}

@Document
record DbMessage(@Id String id, String messageKey, Integer messageId, String message) {}