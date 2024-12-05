#!/bin/bash

# Define the range of speed factors
start_speed=100
end_speed=800
step=100
export SPEED_FACTOR=100
export LOG_PATH="/logs/latency_100.log"
echo "building the images"
docker compose build
echo "running the services"
docker compose up -d kafka kafka-init-topics data_analyzer
# Loop through the speed factors
for (( speed=$start_speed; speed<=$end_speed; speed+=$step ))
do
    echo "Running data generator and latencylogger with speed factor: $speed"
    export SPEED_FACTOR=$speed
    export LOG_PATH="/logs/latency_$speed.log"
    # Set the SPEED_FACTOR environment variable and run the data generator service
    docker compose up  -d data_producer latencylogger
    echo "running completed sleeping for 30secs"
    # echo "Running latency logger with log path: logs/latency_$speed.log"
    # LOG_PATH="logs/latency_$speed.log" docker compose up -d latency-logger
    
    # Optionally, you can add a delay between runs
    # sleep 5
    sleep 10m
done