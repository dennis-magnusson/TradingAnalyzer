# TradingAnalyzer

Low-latency platform for real-time trading analysis.

## Running the project

0. Prerequisites

    - docker and docker compose

1. Download the dataset
   
   `./download_dataset.sh`

3. Create a `.env` file based on the `.env.template` and modify values if needed
   
   `cp .env.template .env`

5. Build the containers

   `docker compose build`
 
7. Run the containers
   
   `docker compose up -d`

## Consuming buy and sell alerts

To consume signal alerts (Buy and Sell) based on breakout patterns, you can use the Kafka console consumer from the kafka container with the following command:

```
docker exec -it <kafka_container> /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic advisory --from-beginning --property print.key=true
```

## Liscense

This project is liscensed under the GPL-3.0 license.
