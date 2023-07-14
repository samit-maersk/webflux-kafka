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

- To see the messages in Kafka topic:

```shell
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic topic-1 --from-beginning --max-messages 10
```

- To deal with `schema registry`
```
#Key
curl -X POST http://localhost:8081/subjects/TOPIC_NAME-KEY/versions" -d {schema: KEY_FILE_CONTENT} -H "Accept: application/json"

#Value
curl -X POST http://localhost:8081/subjects/TOPIC_NAME-KEY/versions" -d {schema: VALUE_FILE_CONTENT} -H "Accept: application/json"
```

- To deal with Kafka `Topic and Group`
```
# todo
```
