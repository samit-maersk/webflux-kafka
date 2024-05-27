package com.example.webfluxkafka;

import org.junit.Ignore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.DockerComposeFiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
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

	static Network network = Network.newNetwork();
	@Container
	@ServiceConnection
	static MongoDBContainer MONGODB = new MongoDBContainer(DockerImageName.parse("mongo"))
			.withNetwork(network)
			.waitingFor(Wait.forListeningPort());

	@Container
	static KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka").asCompatibleSubstituteFor("apache/kafka"))
			.withNetwork(network)
			.withExposedPorts(9093)
			.withEnv("KAFKA_LISTENERS", "PLAINTEXT://0.0.0.0:9093 ,BROKER://0.0.0.0:9092")
			.withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "BROKER:PLAINTEXT,PLAINTEXT:PLAINTEXT")
			.waitingFor(Wait.forListeningPort());
	@Container
	static GenericContainer<?> SCHEMA_REGISTRY = new GenericContainer<>(DockerImageName.parse("confluentinc/cp-schema-registry"))
			.withNetwork(network)
			.dependsOn(KAFKA)
			.withExposedPorts(8081)
			.withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
			.withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
			.withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", KAFKA.getNetworkAliases().get(0)+":9092")
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
						.contentType(MediaType.APPLICATION_JSON)
						.bodyValue("""
								{
									"content": "Hello, Kafka!"
								}
								""")
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
