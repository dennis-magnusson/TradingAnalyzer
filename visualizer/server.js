import express from "express";
import { Kafka } from "kafkajs";
import path from "path";
import { fileURLToPath } from "url";
import { WebSocketServer } from "ws";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const port = 8888;
const app = express();

app.use(express.static(path.join(__dirname, "public")));

// Kafka Consumer
const kafka = new Kafka({ clientId: "visualizer", brokers: ["kafka:9092"] });
const consumerEma = kafka.consumer({ groupId: "visualizer-ema-grp" });
const consumerAdvisory = kafka.consumer({ groupId: "visualizer-advisory-grp" });

await consumerEma.connect();
await consumerAdvisory.connect();

await consumerEma.subscribe({ topic: "ema", fromBeginning: true });
await consumerAdvisory.subscribe({
    topic: "advisory",
    fromBeginning: true,
});

// WebSocket Server
const server = app.listen(port, () => {
    console.log(`Server started on http://localhost:${port}`);
});

const wss = new WebSocketServer({ server });

const startConsumers = () => {
    consumerEma.run({
        eachMessage: async ({ message }) => {
            const key = message.key.toString();
            const value = message.value.toString().split(",");

            const data = {
                topic: "ema",
                symbol: key,
                ema38: parseFloat(value[0]),
                ema100: parseFloat(value[2]),
                time: parseInt(value[6]),
            };

            wss.clients.forEach((client) => {
                if (client.readyState === WebSocket.OPEN) {
                    client.send(JSON.stringify(data));
                }
            });
        },
    });

    consumerAdvisory.run({
        eachMessage: async ({ message }) => {
            const key = message.key.toString();
            const value = message.value.toString();

            const data = {
                topic: "advisory",
                symbol: key,
                advisory: value,
                timestamp: parseInt(message.timestamp),
            };

            wss.clients.forEach((client) => {
                if (client.readyState === WebSocket.OPEN) {
                    client.send(JSON.stringify(data));
                }
            });
        },
    });
};

startConsumers();

wss.on("connection", (ws) => {
    console.log("WebSocket client connected");
});
