const express = require('express');
const dgram = require('dgram');
const http = require('http');
const { Server } = require("socket.io");

// 1. Setup Express + Socket.io
const app = express();
const server = http.createServer(app);

const io = new Server(server, {
    cors: {
        origin: "*",
        methods: ["GET", "POST"]
    }
});

// 2. Setup UDP Listener (from Java Logger)
const udpSocket = dgram.createSocket('udp4');

udpSocket.on('error', (err) => {
    console.log(`UDP Bridge Error:\n${err.stack}`);
    udpSocket.close();
});

// When Java sends a log event
udpSocket.on('message', (msg, rinfo) => {
    const jsonString = msg.toString();

    // Print to Node console
    console.log(`[Java -> Node]: ${jsonString}`);

    try {
        const parsed = JSON.parse(jsonString);
        io.emit('packet_event', parsed); // send to React
    } catch (err) {
        console.error("❌ JSON Parse Error:", err.message);
    }
});

// Bind UDP listener (must match Logger.java port)
udpSocket.bind(5000, () => {
    console.log('✅ UDP Bridge listening for Java logs on udp://localhost:5000');
});

// 3. Start WebSocket server for React UI
io.on('connection', (socket) => {
    console.log('🌐 React Dashboard connected:', socket.id);
});

server.listen(3000, () => {
    console.log('✅ WebSocket server running at ws://localhost:3000');
});
