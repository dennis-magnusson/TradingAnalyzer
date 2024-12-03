# TradingAnalyzer

Low-latency platform for real-time trading analysis.

## Running the project

0. Prerequisites

    - docker and docker compose
    - DEBS 2022 Grand Challenge dataset's csv files ([link](https://zenodo.org/records/6382482))

1. Place DEBS 2022 Grand Challenge dataset's csv files in a directory `./data`.

2. Run the containers
   `docker compose up -d`

## Testing distributed kafka configuration

If you want to make sure every thing is running here is some examples.

1. You can test your kafka connection with these two scripts. they should see everything you write using the producer terminal in the consumer terminal.
   running kafka producer:

```
docker exec -it kafka-kafka1-1 kafka-console-producer  --bootstrap-server kafka1:29092  --topic test
```

running kafka consumer:

```
docker exec -it kafka-kafka1-1 kafka-console-consumer --bootstrap-server kafka1:29092  --topic test --from-beginning
```

3. Consume the events from the topic with the kafka console consumer. [Download](https://dlcdn.apache.org/kafka/3.8.1/kafka_2.13-3.8.1.tgz) the Kafka release here.

```
tar -xzf kafka_2.13-3.8.1.tgz
cd kafka_2.13-3.8.1
bin/kafka-console-consumer.sh --topic trade-events --bootstrap-server localhost:9092 --from-beginning
```

4. Connecting to kafka in spark

    a. Using spark shell:
    execute this command:

    ```
    docker exec -it spark-master bash
    /spark/bin/spark-shell --packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.1
    ```

    you are now in the spark interactive shell
    connect to kafka using this command:

    ```
    val df = spark.readStream.format("kafka").option("kafka.bootstrap.servers", "kafka:9092").option("subscribe", "trade-events").option("startingOffsets","earliest").load()

    // what ever logic you want here:

    // printing dataframe in console
    val query = df.writeStream
    .outputMode("append")
    .format("console")
    .start()

    ```

5. Consuming manually from the results topic:

```
docker exec -it <kafka_container> /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic timestamps --from-beginning --property print.key=true
```

## Spark

you can visit spark master ui in this [link](http://localhost:8080/). You can see the registered workers and applications here.

## Liscense

This project is liscensed under the GPL-3.0 license.
