package com.example.webfluxkafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalField;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@Slf4j
public class WebfluxKafkaApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebfluxKafkaApplication.class, args);
	}

	@Bean
	KafkaSender<Integer, String> kafkaSender(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
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
	ApplicationListener<ApplicationReadyEvent> ready(KafkaSender sender) {
		return event -> {
			Flux<SenderRecord<Integer, String, Integer>> outboundFlux =
					Flux.range(1, 10)
							.map(i -> SenderRecord.create("test-topic", 1, 1l , i, "Message_" + i, i));

			sender.send(outboundFlux)
					.doOnError(e-> log.error("Send failed", e))
					.doOnNext(r -> log.info("Message {} send response: {}"))
					.subscribe();
		};
	}

}
