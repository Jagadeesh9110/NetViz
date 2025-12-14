const express = require('express');
const dgram = require('dgram');
const http = require('http');
const path = require('path');
const fs = require('fs');
const { Server } = require("socket.io");

// 1. Express + Socket.io Setup
const app = express();
const server = http.createServer(app);

const io = new Server(server, {
    cors: {
        origin: "http://localhost:5173",
        methods: ["GET", "POST"]
    }
});

// 2. Ensure "received" folder exists
const RECEIVED_DIR = path.join(__dirname, 'received');

if (!fs.existsSync(RECEIVED_DIR)) {
    fs.mkdirSync(RECEIVED_DIR);
    console.log(" Created folder: Node_Bridge/received/");
}

// Serve all received files as static
app.use('/received', express.static(RECEIVED_DIR));

console.log(` Serving received files at: http://localhost:3000/received/<filename>`);

// 3. UDP Listener for Java Logger
const udpSocket = dgram.createSocket('udp4');

udpSocket.on('error', (err) => {
    console.log(`UDP Bridge Error:\n${err.stack}`);
    udpSocket.close();
});

// When Java sends any log event
udpSocket.on('message', (msg, rinfo) => {
    const jsonString = msg.toString();

    console.log(`[Java -> Node]: ${jsonString}`);

    try {
        const parsed = JSON.parse(jsonString);

        // --- Forward all normal events ---
        io.emit('packet_event', parsed);

        // --- Special: Progress Events ---
        if (parsed.event === "PROGRESS_UPDATE") {
            io.emit("progress_update", {
                received: parsed.received,
                total: parsed.total,
                timestamp: parsed.timestamp
            });
        }

        // --- FILE COMPLETE  ---
        if (parsed.event === "FILE_COMPLETE") {
            io.emit("file_complete", {
                filename: parsed.filename,
                size: parsed.size
            });
        }


    } catch (err) {
        console.error("JSON Parse Error:", err.message);
    }
});

// Bind UDP listener (Logger.java must send to port 5000)
udpSocket.bind(5000, () => {
    console.log(' UDP Bridge listening for Java logs on udp://localhost:5000');
});

// 4. Start WebSocket for React UI
io.on("connection", (socket) => {
    console.log("React Dashboard connected:", socket.id);

    socket.on("set_window_size", (data) => {
        console.log("UI → Set window size:", data.size);

        const msg = Buffer.from(
            JSON.stringify({ event: "SET_WINDOW", size: data.size })
        );

        udpSocket.send(msg, 0, msg.length, 5001, "localhost");
    });

    socket.on("set_loss_chance", (data) => {
        console.log("UI → Set Packet Loss:", data.chance + "%");

        const msg = Buffer.from(
            JSON.stringify({ event: "SET_LOSS", chance: data.chance })
        );

        udpSocket.send(msg, 0, msg.length, 5001, "localhost");
    });

});


server.listen(3000, () => {
    console.log(' WebSocket server running at ws://localhost:3000');
});
