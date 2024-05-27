package com.example.webfluxkafka;

import org.junit.Ignore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@AutoConfigureWebTestClient
@Testcontainers
@Ignore
class WebfluxKafkaApplicationTests {

	@Container
	@ServiceConnection
	static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest").asCompatibleSubstituteFor("apache/kafka:latest"))
			.withExposedPorts(9092)
			.waitingFor(Wait.forListeningPorts(9092));

	@Container
	@ServiceConnection
	static MongoDBContainer mongoDbContainer = new MongoDBContainer(DockerImageName.parse("mongo:latest"))
			.waitingFor(Wait.forListeningPort());


	@Autowired
	WebTestClient webTestClient;

	@Test
	void contextLoads() {
	}

	@Test
	@DisplayName("Test /send-message")
	void e2eTest() {
		assertAll("e2e test",
				() -> webTestClient
						.post()
						.uri("/send-message")
						.bodyValue("Hello Kafka")
						.exchange()
						.expectStatus().isOk(),
				() -> webTestClient
						.get()
						.uri("/db")
						.exchange()
						.expectStatus().isOk()
						.expectBodyList(Message.class)
						.hasSize(1)
		);
	}


}
