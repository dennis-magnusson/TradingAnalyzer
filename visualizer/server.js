const WebSocket = require("ws");
const kafka = require("kafka-node");
const express = require("express");
const path = require("path");

const port = 8888;
const app = express();

app.use(express.static(path.join(__dirname, "public")));

// Kafka Consumer
const Consumer = kafka.Consumer;
const client = new kafka.KafkaClient({ kafkaHost: "kafka:9092" });
const consumerEma = new Consumer(client, [{ topic: "ema", partition: 0 }], {
    autoCommit: true,
});
const consumerAdvisory = new Consumer(
    client,
    [{ topic: "advisory", partition: 0 }],
    {
        autoCommit: true,
    }
);

// WebSocket Server
const server = app.listen(port, () => {
    console.log(`Server started on http://localhost:${port}`);
});

const wss = new WebSocket.Server({ server });

wss.on("connection", (ws) => {
    console.log("WebSocket client connected");

    consumerEma.on("message", (message) => {
        const key = message.key;
        const value = message.value.split(",");

        const data = {
            topic: "ema",
            symbol: key,
            time: value[6],
            ema38: parseFloat(value[0]),
            ema100: parseFloat(value[2]),
        };

        console.log(data);

        ws.send(JSON.stringify(data));
    });

    consumerAdvisory.on("message", (message) => {
        const key = message.key;
        const value = message.value;

        const data = {
            topic: "advisory",
            symbol: key,
            advisory: value,
            timestamp: message.timestamp,
        };

        ws.send(JSON.stringify(data));
    });

    ws.on("close", () => {
        console.log("WebSocket client disconnected");
    });
});

consumerEma.on("error", (err) => {
    console.error("Kafka ema consumer error:", err);
});

consumerAdvisory.on("error", (err) => {
    console.error("Kafka advisory consumer error:", err);
});
