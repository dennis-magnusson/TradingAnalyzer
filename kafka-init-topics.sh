#!/bin/sh

TRADE_EVENTS_TOPIC_NAME=${TRADE_EVENTS_TOPIC_NAME:-trade-events}
TRADE_EVENTS_PARTITIONS=${TRADE_EVENTS_PARTITIONS:-1}
TRADE_EVENTS_REPLICATION_FACTOR=${TRADE_EVENTS_REPLICATION_FACTOR:-1}

EMA_TOPIC_NAME=${EMA_TOPIC_NAME:-ema}
EMA_PARTITIONS=${EMA_PARTITIONS:-1}
EMA_REPLICATION_FACTOR=${EMA_REPLICATION_FACTOR:-1}

ADVISORY_TOPIC_NAME=${ADVISORY_TOPIC_NAME:-advisory}
ADVISORY_PARTITIONS=${ADVISORY_PARTITIONS:-1}
ADVISORY_REPLICATION_FACTOR=${ADVISORY_REPLICATION_FACTOR:-1}

BOOTSTRAP_SERVER=${BOOTSTRAP_SERVER:-kafka:9092}

/opt/kafka/bin/kafka-topics.sh --create \
    --topic "$TRADE_EVENTS_TOPIC_NAME" \
    --partitions "$TRADE_EVENTS_PARTITIONS" \
    --replication-factor "$TRADE_EVENTS_REPLICATION_FACTOR" \
    --if-not-exists \
    --bootstrap-server "$BOOTSTRAP_SERVER"

/opt/kafka/bin/kafka-topics.sh --create \
    --topic "$EMA_TOPIC_NAME" \
    --partitions "$EMA_PARTITIONS" \
    --replication-factor "$EMA_REPLICATION_FACTOR" \
    --if-not-exists \
    --bootstrap-server "$BOOTSTRAP_SERVER"

/opt/kafka/bin/kafka-topics.sh --create \
    --topic "$ADVISORY_TOPIC_NAME" \
    --partitions "$ADVISORY_PARTITIONS" \
    --replication-factor "$ADVISORY_REPLICATION_FACTOR" \
    --if-not-exists \
    --bootstrap-server "$BOOTSTRAP_SERVER"
