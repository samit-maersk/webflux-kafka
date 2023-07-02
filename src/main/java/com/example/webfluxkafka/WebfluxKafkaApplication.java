package com.example.webfluxkafka;

import io.confluent.developer.User;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@Slf4j
public class WebfluxKafkaApplication {

	@Value("${spring.kafka.bootstrap-servers}")
	private String bootstrapServers;

	@Value("${spring.kafka.schema.registry.url}")
	private String schemaRegistry;

	@Value("${spring.kafka.topic.topic1}")
	private String topicName;

	public static void main(String[] args) {
		SpringApplication.run(WebfluxKafkaApplication.class, args);
	}

	// Both kafkaAdmin and topic1 are Optional If you are working on a real project, you should create a topic manually.
	@Bean
	public KafkaAdmin kafkaAdmin() {
		Map<String, Object> configs = new HashMap<>();
		configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		return new KafkaAdmin(configs);
	}

	@Bean
	public NewTopic topic1() {
		return new NewTopic(topicName, 1, (short) 1);
	}
	@Bean
	public NewTopic bank(@Value("${spring.kafka.topic.bank}") String topicName) {
		return new NewTopic(topicName, 1, (short) 1);
	}
	@Bean
	public NewTopic user(@Value("${spring.kafka.topic.user}") String topicName) {
		return new NewTopic(topicName, 1, (short) 1);
	}

	@Bean
	ApplicationListener<ApplicationReadyEvent> ready(UsersRepository usersRepository) {
		return event -> {
			//Consumer 1
			Map<String, Object> consumerProps = new HashMap<>();
			consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
			consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "sample-group");
			consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
			consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
			consumerProps.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistry);

//			consumerProps.put(AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
//			consumerProps.put(AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG, schemaRegistryUsername + ":" + schemaRegistryPassword);

			ReceiverOptions<String, User> receiverOptions =
					ReceiverOptions.<String, User>create(consumerProps)
							.pollTimeout(Duration.ofMillis(5000))
							.subscription(Collections.singleton(topicName));

			Flux<ReceiverRecord<String, User>> inboundFlux =
					KafkaReceiver.create(receiverOptions)
							.receive();

			inboundFlux
					// store this in the db. If success r.receiverOffset().acknowledge() or else don't clear the offset..rather retry
					.subscribe(r -> {
						log.info("Received message: {}\n", r);
						r.receiverOffset().acknowledge();
					});
			//Consumer 1
		};
	}

	//route
	@Bean
	RouterFunction routes() {
		return RouterFunctions.route()
				.POST("/send-message", request -> {
					return request.bodyToMono(String.class)
							.doOnNext(msg -> sendMessage(topicName))
							.then(ServerResponse.ok().build());
				})
				.build();
	}

	private void sendMessage(String topicName) {

		// This can be a generic bean if you have one producer type
		Map<String, Object> producerProps = new HashMap<>();
		producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
		producerProps.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistry);

//		producerProps.put(AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
//		producerProps.put(AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG, schemaRegistryUsername + ":" + schemaRegistryPassword);
//		producerProps.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);

		SenderOptions<String, User> senderOptions =
				SenderOptions.<String, User>create(producerProps)
						.maxInFlight(1024);

		KafkaSender<String, User> kafkaSender = KafkaSender.create(senderOptions);
		// This can be a generic bean if you have one producer type

		var partition = 0;
		var currentTimeInLong = LocalDateTime.now().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
		Flux<SenderRecord<String, User, Integer>> outboundFlux = Flux.range(1, 5)
				.map(i -> SenderRecord.create(topicName, partition, currentTimeInLong , "Message_" + i, User.newBuilder().setName("Name "+i).setAge(20 + i).build(), i));

		kafkaSender.send(outboundFlux)
				.doOnError(e-> log.error("Send failed", e))
				.doOnNext(r -> log.info("Message Sent" ))
				.subscribe();
	}

}

@Document
record Users(@Id String id, String name, int age){}

interface UsersRepository extends ReactiveMongoRepository<Users,String>{}