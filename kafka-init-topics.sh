#!/bin/sh

TRADE_EVENTS_TOPIC_NAME=${TRADE_EVENTS_TOPIC_NAME:-trade-events}
TRADE_EVENTS_PARTITIONS=${TRADE_EVENTS_PARTITIONS:-1}
TRADE_EVENTS_REPLICATION_FACTOR=${TRADE_EVENTS_REPLICATION_FACTOR:-1}

LATENCY_TOPIC_NAME=${LATENCY_TOPIC_NAME:-timestamps}
LATENCY_PARTITIONS=${LATENCY_PARTITIONS:-1}
LATENCY_REPLICATION_FACTOR=${LATENCY_REPLICATION_FACTOR:-1}

BOOTSTRAP_SERVER=${BOOTSTRAP_SERVER:-kafka:9092}

/opt/kafka/bin/kafka-topics.sh --create \
    --topic "$TRADE_EVENTS_TOPIC_NAME" \
    --partitions "$TRADE_EVENTS_PARTITIONS" \
    --replication-factor "$TRADE_EVENTS_REPLICATION_FACTOR" \
    --bootstrap-server "$BOOTSTRAP_SERVER"

/opt/kafka/bin/kafka-topics.sh --create \
    --topic "$LATENCY_TOPIC_NAME" \
    --partitions "$LATENCY_PARTITIONS" \
    --replication-factor "$LATENCY_REPLICATION_FACTOR" \
    --bootstrap-server "$BOOTSTRAP_SERVER"

# TODO: make script succesfully exit even if the topics are already created