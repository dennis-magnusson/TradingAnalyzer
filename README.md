# TradingAnalyzer

Low-latency platform for real-time trading analysis.

## Running the project

0. Prerequisites
-   `docker` and `docker compose`


1. Download the DEBS 2022 Grand Challenge dataset's csv files ([link](https://zenodo.org/records/6382482)) and place them in a directory `./data`.

2. Run the containers
   `docker compose up -d`

## Testing
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

## Liscense

This project is liscensed under the GPL-3.0 license.
