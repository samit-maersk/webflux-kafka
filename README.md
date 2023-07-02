# webflux-kafka

Kafka debug:

```shell

      sh -c "
        kafka-topics --bootstrap-server localhost:9092 --list

        echo -e 'Creating kafka topics'
        kafka-topics --bootstrap-server localhost:9092 --create --if-not-exists --topic topic-1 --replication-factor 1 --partitions 1
        kafka-topics --bootstrap-server localhost:9092 --create --if-not-exists --topic topic-2 --replication-factor 1 --partitions 1

        echo -e 'Successfully created the following topics:'
        kafka-topics --bootstrap-server localhost:9092 --list
      "
```
An example of using Spring WebFlux and reactor Kafka:

- [producer](https://github.com/reactor/reactor-kafka/blob/main/reactor-kafka-samples/src/main/java/reactor/kafka/samples/SampleProducer.java)
- [consumer](https://github.com/reactor/reactor-kafka/blob/main/reactor-kafka-samples/src/main/java/reactor/kafka/samples/SampleConsumer.java)
- [avro example](https://www.confluent.io/blog/schema-registry-avro-in-spring-boot-application-tutorial/)
- [avro example gitgub](https://github.com/confluentinc/springboot-kafka-avro/tree/master/src)
- [avro example](https://github.com/lydtechconsulting/kafka-schema-registry-avro/blob/v1.0.0/schema-registry-demo-service/src/main/java/demo/kafka/KafkaDemoConfiguration.java)
- To see the messages in Kafka topic:

```shell
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic topic-1 --from-beginning --max-messages 10
```