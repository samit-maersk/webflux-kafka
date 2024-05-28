package com.example.webfluxkafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;

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

	//@Value("${spring.kafka.topic}")
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
	KafkaSender<Integer, String> kafkaSender() {
		Map<String, Object> producerProps = new HashMap<>();
		producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class);
		producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

		SenderOptions<Integer, String> senderOptions =
				SenderOptions.<Integer, String>create(producerProps)
						.maxInFlight(1024);

		return KafkaSender.create(senderOptions);
	}


	@Bean
	ApplicationListener<ApplicationReadyEvent> ready() {
		return event -> {
			Map<String, Object> consumerProps = new HashMap<>();
			consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
			consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "sample-group");
			consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class);
			consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

			ReceiverOptions<Integer, String> receiverOptions =
					ReceiverOptions.<Integer, String>create(consumerProps)
							.subscription(Collections.singleton(topicName));

			Flux<ReceiverRecord<Integer, String>> inboundFlux =
					KafkaReceiver.create(receiverOptions)
							.receive();

			inboundFlux.subscribe(r -> {
				log.info("Received message: {}\n", r);
				r.receiverOffset().acknowledge();
				System.out.printf("Received message: topic-partition=%s offset=%d key=%d value=%s%n",
					r.receiverOffset().topicPartition(),
					r.receiverOffset().offset(),
					r.key(),
					r.value());
			});
		};
	}

	//route
	@Bean
	RouterFunction routes(KafkaSender sender) {
		return RouterFunctions.route()
				.POST("/send-message", request -> request.bodyToMono(String.class)
						.doOnNext(msg -> sendMessage(sender, topicName))
						.then(ServerResponse.ok().build()))
				.build();
	}

	private void sendMessage(KafkaSender sender, String topicName) {
		var partition = 0;
		var currentTimeInLong = LocalDateTime.now().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();

		Flux<SenderRecord<Integer, String, Integer>> outboundFlux = Flux.range(1, 10)
				.map(i -> SenderRecord.create(topicName, partition, currentTimeInLong , i, "Message_" + i, i));

		sender.send(outboundFlux)
				.doOnError(e-> log.error("Send failed", e))
				.doOnNext(r -> log.info("Message Sent" ))
				.subscribe();
	}

}
