# TradingAnalyzer

Low-latency platform for real-time trading analysis.

## Running the project

0. Prerequisites

-   `sbt`
-   `docker` and `docker compose`

1. Compile the data_producer scala project

```
cd data_producer
sbt assembly
cd ..
```

This will compile a .jar file for the data_producer docker container.

2. Download the DEBS 2022 Grand Challenge dataset's csv files ([link](https://zenodo.org/records/6382482)) and place them in a directory `./data`.

3. Run the containers
   `docker compose up`

## Liscense

This project is liscensed under the GPL-3.0 license.
