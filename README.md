# TradingAnalyzer

Low-latency platform for real-time trading analysis.

## Running the project

0. Prerequisites

-   `sbt`
-   `docker` and `docker compose`
-   Download the DEBS 2022 Grand Challenge dataset's csv files ([link](https://zenodo.org/records/6382482)) and place them in a directory `./data`.

1. Compile the data_producer scala project

```
cd data_producer
sbt assembly
cd ..
```

This will compile a .jar file for the data_producer docker container.

2.  Run the containers
    `docker compose up`

### Manual use (temporary)

1. Start Kafka in a Docker container

`docker run -p 9092:9092 apache/kafka:3.8.1`

2. Compile and build the data_producer

```
cd data_producer
sbt assembly
cd ..
java -jar data_producer/target/scala-2.12/dataproducer-assembly-1.0.jar
```

3. Consume the events from the topic with the kafka console consumer. [Download](https://dlcdn.apache.org/kafka/3.8.1/kafka_2.13-3.8.1.tgz) the Kafka release here.

```
tar -xzf kafka_2.13-3.8.1.tgz
cd kafka_2.13-3.8.1
bin/kafka-console-consumer.sh --topic trade-events --bootstrap-server localhost:9092 --from-beginning
```

## Liscense

This project is liscensed under the GPL-3.0 license.
