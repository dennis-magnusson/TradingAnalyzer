const WebSocket = require("ws");
const kafka = require("kafka-node");

// Kafka Consumer setup
const Consumer = kafka.Consumer;
const client = new kafka.KafkaClient({ kafkaHost: "kafka:9092" });
const consumer = new Consumer(client, [{ topic: "ema", partition: 0 }], {
    autoCommit: true,
});

// WebSocket Server setup
const wss = new WebSocket.Server({ port: 8888 });

wss.on("connection", (ws) => {
    console.log("WebSocket client connected");

    consumer.on("message", (message) => {
        const key = message.key;
        const value = message.value.split(",");

        const data = {
            symbol: key,
            time: value[6],
            ema38: parseFloat(value[0]),
            ema100: parseFloat(value[2]),
        };

        ws.send(JSON.stringify(data));
    });

    ws.on("close", () => {
        console.log("WebSocket client disconnected");
    });
});

consumer.on("error", (err) => {
    console.error("Kafka consumer error:", err);
});

console.log("WebSocket server is running on ws://localhost:8888");
