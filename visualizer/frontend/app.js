import Chart from "chart.js/auto";
import "chartjs-adapter-luxon";
import "luxon";

const ctx = document.getElementById("ema-chart").getContext("2d");
const connectionStatus = document.getElementById("connection_status");
const lastUpdated = document.getElementById("lastupdated");
const checkboxContainer = document.getElementById("checkbox-container");

const receivedData = {};
const selectedSymbols = new Set();

const EMA100GraphColor = "rgba(192, 0, 0, 1)";
const EMA38GraphColor = "rgba(7, 110, 141, 1)";

const chart = new Chart(ctx, {
    type: "line",
    data: {
        datasets: [],
    },
    options: {
        animation: false,
        responsive: true,
        maintainAspectRatio: false,
        scales: {
            x: {
                type: "time",
                time: {
                    unit: "minute",
                    displayFormats: {
                        minute: "HH:mm",
                    },
                },
                ticks: {
                    maxTicksLimit: 20,
                    autoSkip: true,
                },
                title: {
                    display: true,
                    text: "Window End Time",
                },
            },
            y: {
                beginAtZero: false,
                title: {
                    display: true,
                    text: "EMA Value",
                },
            },
        },
        plugins: {
            legend: {
                position: "top",
            },
        },
    },
});

const socket = new WebSocket("ws://localhost:8888");

socket.onmessage = function (event) {
    const data = JSON.parse(event.data);
    const symbol = data.symbol;
    const time = new Date(parseInt(data.time, 10));
    const ema38 = data.ema38;
    const ema100 = data.ema100;

    if (!receivedData[symbol]) {
        receivedData[symbol] = [];
        createCheckbox(symbol);
    }
    receivedData[symbol].push({ time, ema38, ema100 });

    updateLastUpdated();
    updateChart();
};

socket.onopen = function () {
    connectionStatus.textContent = "Connected";
    connectionStatus.classList.add("green");
    connectionStatus.classList.remove("red");
    console.log("WebSocket connection established");
};

socket.onclose = function () {
    connectionStatus.classList.add("red");
    connectionStatus.classList.remove("green");
    connectionStatus.textContent = "Disconnected";
    console.log("WebSocket connection closed");
};

socket.onerror = function (error) {
    connectionStatus.textContent = "Websocket error: " + error;
    console.error("WebSocket error:", error);
};

function createCheckbox(symbol) {
    const checkbox = document.createElement("input");
    checkbox.type = "checkbox";
    checkbox.id = symbol;
    checkbox.name = symbol;
    checkbox.value = symbol;
    checkbox.checked = false;
    checkbox.addEventListener("change", updateChart);

    const label = document.createElement("label");
    label.htmlFor = symbol;
    label.appendChild(document.createTextNode(symbol));

    const container = document.createElement("div");
    container.appendChild(checkbox);
    container.appendChild(label);

    checkboxContainer.appendChild(container);

    selectedSymbols.add(symbol);
}

function updateChart() {
    const datasets = [];

    selectedSymbols.forEach((symbol) => {
        if (document.getElementById(symbol).checked) {
            const data = receivedData[symbol].map((point) => ({
                x: point.time,
                y: point.ema38,
            }));
            datasets.push({
                label: `${symbol} EMA38`,
                borderColor: EMA38GraphColor,
                data: data,
                fill: false,
            });

            const data100 = receivedData[symbol].map((point) => ({
                x: point.time,
                y: point.ema100,
            }));
            datasets.push({
                label: `${symbol} EMA100`,
                borderColor: EMA100GraphColor,
                data: data100,
                fill: false,
            });
        }
    });

    chart.data.datasets = datasets;
    chart.update();
}

function updateLastUpdated() {
    const time = new Date();
    lastUpdated.textContent = time.toLocaleString();
}
