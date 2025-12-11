# NetViz  — Real-Time Reliable UDP Sliding Window Simulator

> **© M. Jagadeeswar Reddy 2025. All Rights Reserved.**

![NetViz Dashboard](versions_images/dashboard-screenshot_version1.png)

## 🚀 Overview

**NetViz Pro** is a full-stack Reliable UDP Transport Simulator that demonstrates how modern sliding-window ARQ protocols work inside real networks.

Unlike textbook-only Go-Back-N or Selective Repeat, NetViz Pro implements an optimized **hybrid ARQ protocol** inspired by TCP:

* ✔ Out-of-order reception
* ✔ Cumulative ACKs
* ✔ Selective retransmission
* ✔ Continuous sliding window
* ✔ FIN-based termination

The result is a high-performance, easy-to-understand, and beautifully visualized network simulator.

This project integrates:
* **Java** → Core ARQ Engine
* **Node.js** → UDP → WebSocket Bridge
* **React (Vite) + TailwindCSS** → Visualization/UI
* **Recharts + Lucide** → Real-time telemetry

---

## ✨ Key Features

### 1. Hybrid Sliding Window ARQ (Java)
A custom protocol combining the best of Go-Back-N + Selective Repeat:
* **Supports out-of-order packet reception**
* **Uses cumulative ACKs** (like TCP)
* **Retransmits only missing packets**
* **Timeout detection + retry logic**
* **FIN handshake** for session closing
* **Checksum-based integrity validation**
* **Packet segmentation + reconstruction**

This design provides:
✔ High throughput
✔ Simplified ACK logic
✔ More realistic behavior
✔ Easier visualization

### 2. Real-Time Visualization Dashboard (React + Tailwind)
The UI animates every network event:
* **Packet Sent** → Flying animation
* **ACK Received** → Green flash
* **Timeout** → Shake animation
* **Retransmission** → Bounce animation
* **Current sliding window** → Blue highlight box

**Plus graphs:**
* Throughput chart (SEQ vs. Time)
* Metrics dashboard (Sent, ACKs, Timeouts, Retries)
* Live log stream with timestamps

### 3. Node.js UDP → WebSocket Bridge
A lightweight middleware relays Java events to the UI with zero delay:

| Java Event | Purpose |
| :--- | :--- |
| `PACKET_SENT` | Packet transmitted |
| `PACKET_RECEIVED` | Receiver got data |
| `ACK_SENT` | ACK packet sent |
| `ACK_RECEIVED` | ACK processed |
| `TIMEOUT` | Packet assumed lost |
| `RETRANSMIT` | Missing packet resent |
| `WINDOW_MOVED` | Sender's window advanced |
| `FIN` | Transmission terminated |

**Ports:**
* **UDP 5000** → Node receives events
* **WebSocket 3000** → Frontend listens live

### 4. Metrics & Simulation Controls
Interactive dashboard includes:
* Adjustable Window Size
* Adjustable Packet Loss Simulation
* Pause / Resume playback
* Reset Simulation
* Real-time counters for:
    * Packets Sent
    * ACK Received
    * Timeouts
    * Retransmissions

---

## 🧠 System Architecture

```text
┌──────────────────┐       UDP:5000         ┌─────────────────────┐        WS:3000         ┌──────────────────────┐
│   Java ARQ Core  │  ─────────────────►  │      Node Bridge      │  ─────────────────►   │      React Dashboard     │
│ (Sender/Receiver) │                      │ (UDP → WebSocket)     │                      │ (Real-Time Visualizer) │
└──────────────────┘  ◄──────────────────  └─────────────────────┘  ◄──────────────────  └──────────────────────┘
        ▲                                                                                                       
        │  Segmentation                                                                                         
        │  Sliding Window                                                                                       
        │  Cumulative ACKs                                                                                      
        │  Retransmissions                                                                                      
        │  FIN Handshake                                                                                        
🧩 Technology Stack
Frontend
React (Vite)

TailwindCSS

Recharts (graphs)

Lucide Icons

Custom CSS Physics Animations

Middleware
Node.js

Express.js

Socket.IO

UDP Sockets (dgram)

Core Networking Engine
Java (JDK 17+)

Custom ARQ protocol implementation

Manual packet framing + checksum

Byte-level packet handling

📁 Project Structure
Plaintext

NetViz/
│
├── Java_Core/              # Core ARQ Protocol
│   └── src/
│       ├── Sender.java
│       ├── Receiver.java
│       ├── WindowManager.java
│       ├── Logger.java
│       ├── CustomPacket.java
│       ├── Utils.java
│       └── Main.java
│
├── Node_Bridge/            # UDP → WebSocket Bridge
│   ├── server.js
│   └── package.json
│
└── Frontend_UI/            # Visualization Layer (React + Tailwind)
    ├── src/
    │   ├── App.jsx
    │   ├── index.css
    │   └── main.jsx
    ├── index.html
    └── package.json
🛠 How to Run the Project
1️⃣ Start Node Bridge
Bash

cd NetViz/Node_Bridge
node server.js
You should see:

Plaintext

UDP Bridge listening on port 5000
WebSocket server listening on 3000
2️⃣ Start the React Dashboard
Bash

cd NetViz/Frontend_UI
npm install
npm run dev
Open the dashboard at: http://localhost:5173

3️⃣ Run Java ARQ Protocol
Bash

cd NetViz/Java_Core/src
javac *.java
java Main
Example output:

Plaintext

Sender Started Transmission
...
FIN sent
Receiver ACKed FIN
FINAL RECEIVED DATA: <reconstructed message>
Meanwhile, the dashboard visualizes:

Packet movement

ACK waves

Timeout pulses

Window sliding

Real-time logs

📬 Contact
Built by M. Jagadeeswar Reddy.
