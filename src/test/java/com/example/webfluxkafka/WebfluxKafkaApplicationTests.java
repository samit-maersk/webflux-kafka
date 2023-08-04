package com.example.webfluxkafka;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
class WebfluxKafkaApplicationTests {

	@Container
	static final MongoDBContainer MONGO_DB_CONTAINER = new MongoDBContainer(DockerImageName.parse("mongo"))
			.withExposedPorts(27017)
			.waitingFor(Wait.forListeningPort());

	@Container
	static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.3.0"))
			.withExposedPorts(9093)
			.waitingFor(Wait.forListeningPort());

	@DynamicPropertySource
	static void setDynamicProps(DynamicPropertyRegistry registry) {
		registry.add("spring.data.mongodb.uri", () -> MONGO_DB_CONTAINER.getReplicaSetUrl());
		registry.add("spring.kafka.bootstrap-servers", () -> KAFKA_CONTAINER.getBootstrapServers());
	}

	@Test
	void contextLoads() {
	}

}
