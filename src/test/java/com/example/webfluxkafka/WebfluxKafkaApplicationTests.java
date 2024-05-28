package com.example.webfluxkafka;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureWebTestClient
@Testcontainers
class WebfluxKafkaApplicationTests {

	static Network network = Network.newNetwork();

	@Container
	@ServiceConnection
	static MongoDBContainer MONGODB = new MongoDBContainer(DockerImageName.parse("mongo:4.4.2"))
			.withNetwork(network)
			.waitingFor(Wait.forListeningPort());

	@Container
	static org.testcontainers.containers.KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka"))
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
			.withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", KAFKA.getNetworkAliases().getFirst()+":9092")
			.waitingFor(Wait.forListeningPort());

	@Autowired
	WebTestClient webTestClient;

	@Test
	void contextLoads() {
	}
}
