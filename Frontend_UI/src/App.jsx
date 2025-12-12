import React, { useEffect, useState, useRef } from 'react';
import io from 'socket.io-client';
import {
  Activity, Server, AlertTriangle, CheckCircle2,
  ArrowRight, Clock, RefreshCw, Play, Pause, RotateCcw,
  Settings, Box
} from 'lucide-react';
import {
  XAxis, YAxis, Tooltip, ResponsiveContainer, AreaChart, Area, CartesianGrid
} from 'recharts';

const socket = io('http://localhost:3000');

const App = () => {
  // --- STATE ---
  const [logs, setLogs] = useState([]);
  const [windowState, setWindowState] = useState({ start: 0, end: 4 });
  const [packetStatus, setPacketStatus] = useState({});
  const [animations, setAnimations] = useState([]);
  const [metrics, setMetrics] = useState({ sent: 0, acked: 0, timeouts: 0, retransmits: 0 });
  const [throughputData, setThroughputData] = useState([]);

  // Progress Bar + File Metadata + Preview
  const [progress, setProgress] = useState({ received: 0, total: 0, percent: 0 });
  const [fileInfo, setFileInfo] = useState({ name: "", size: 0 });
  const [previewUrl, setPreviewUrl] = useState(null);

  const [isPaused, setIsPaused] = useState(false);
  const [simSettings, setSimSettings] = useState({ windowSize: 5, lossChance: 0 });

  const logsEndRef = useRef(null);

  // --- ANIMATION ENGINE ---
  const triggerAnim = (seq, type) => {
    if (isPaused) return;
    const id = Math.random().toString(36).substr(2, 9);
    setAnimations(prev => [...prev, { id, seq, type }]);
    setTimeout(() => {
      setAnimations(prev => prev.filter(a => a.id !== id));
    }, 700);
  };

  // --- PACKET EVENTS ---
  useEffect(() => {
    socket.on('packet_event', (data) => {
      if (isPaused) return;

      const now = new Date().toLocaleTimeString();
      setLogs(prev => [...prev.slice(-49), { ...data, time: now }]);

      // Status updates
      setPacketStatus(prev => {
        const next = { ...prev };
        if (data.event === "PACKET_SENT") {
          next[data.seq] = "SENT";
          triggerAnim(data.seq, "fly");
        }
        if (data.event === "ACK_RECEIVED") {
          next[data.ack] = "ACKED";
          triggerAnim(data.ack, "blink");
        }
        if (data.event === "TIMEOUT") {
          next[data.seq] = "TIMEOUT";
          triggerAnim(data.seq, "shake");
        }
        if (data.event === "RETRANSMIT") {
          next[data.seq] = "RETRANSMIT";
          triggerAnim(data.seq, "bounce");
        }
        return next;
      });

      // Window movement
      if (data.event === "WINDOW_MOVED") {
        setWindowState({ start: data.newStart, end: data.newEnd });
      } else if (data.event === "PACKET_SENT" && data.windowStart !== undefined) {
        setWindowState({ start: data.windowStart, end: data.windowEnd });
      }

      // Metrics
      if (data.event === "PACKET_SENT") setMetrics(m => ({ ...m, sent: m.sent + 1 }));
      if (data.event === "ACK_RECEIVED") {
        setMetrics(m => ({ ...m, acked: m.acked + 1 }));
        setThroughputData(prev => [...prev.slice(-20), { time: now, seq: data.ack }]);
      }
      if (data.event === "TIMEOUT") setMetrics(m => ({ ...m, timeouts: m.timeouts + 1 }));
      if (data.event === "RETRANSMIT") setMetrics(m => ({ ...m, retransmits: m.retransmits + 1 }));
    });

    return () => socket.off('packet_event');
  }, [isPaused]);

  // --- PROGRESS EVENTS ---
  useEffect(() => {
    socket.on("progress_update", ({ received, total, fileName }) => {
      setProgress({
        received,
        total,
        percent: total > 0 ? Math.floor((received / total) * 100) : 0
      });

      if (fileName && fileName !== fileInfo.name) {
        setFileInfo({ name: fileName, size: total });

        // Show preview if image
        if (fileName.match(/\.(png|jpg|jpeg|gif)$/i)) {
          setPreviewUrl(`http://localhost:3000/received/${fileName}`);
        } else {
          setPreviewUrl(null);
        }
      }
    });

    return () => socket.off("progress_update");
  }, [fileInfo]);

  useEffect(() => {
    if (!isPaused) logsEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [logs, isPaused]);

  useEffect(() => {
    socket.on("file_complete", ({ filename, size }) => {
      console.log("File ready:", filename);

      // Show preview for images
      if (filename.match(/\.(png|jpg|jpeg|gif)$/i)) {
        setPreviewUrl(`http://localhost:3000/received/${filename}`);
      }

      setFileInfo({ name: filename, size });
    });

    return () => socket.off("file_complete");
  }, []);


  // --- CONTROLS ---
  const handleReset = () => {
    setLogs([]);
    setPacketStatus({});
    setMetrics({ sent: 0, acked: 0, timeouts: 0, retransmits: 0 });
    setThroughputData([]);
    setWindowState({ start: 0, end: 4 });
    setProgress({ received: 0, total: 0, percent: 0 });
  };

  const updateSim = (key, val) => {
    setSimSettings(prev => ({ ...prev, [key]: val }));
  };

  // --- RENDER PACKET ---
  const renderPacket = (seq) => {
    const status = packetStatus[seq] || 'PENDING';
    const isInsideWindow = seq >= windowState.start && seq <= windowState.end;

    const anim = animations.find(a => a.seq === seq);
    let animClass = "";
    if (anim) {
      if (anim.type === "fly") animClass = "animate-fly z-20";
      if (anim.type === "blink") animClass = "animate-blink";
      if (anim.type === "shake") animClass = "animate-shake";
      if (anim.type === "bounce") animClass = "animate-bounce";
    }

    let statusStyle = "border-slate-700 bg-slate-800/50 text-slate-500 scale-90 opacity-60";
    if (status === 'SENT') statusStyle = "border-yellow-500 bg-yellow-500/10 text-yellow-400";
    if (status === 'ACKED') statusStyle = "border-emerald-500 bg-emerald-500/10 text-emerald-400";
    if (status === 'TIMEOUT') statusStyle = "border-red-500 bg-red-500/10 text-red-400";
    if (status === 'RETRANSMIT') statusStyle = "border-orange-500 bg-orange-500/10 text-orange-400";

    const windowHighlight = isInsideWindow ? "ring-2 ring-blue-500 ring-offset-2 ring-offset-slate-900" : "";

    return (
      <div key={seq} className={`relative w-14 h-16 flex flex-col items-center justify-center rounded-md border transition-all duration-300 ${statusStyle} ${windowHighlight} ${animClass}`}>
        <span className="text-[9px] opacity-70">SEQ</span>
        <span className="text-xl font-bold">{seq}</span>
        {status === 'ACKED' && <CheckCircle2 size={12} className="absolute -top-1 -right-1 text-emerald-500 bg-slate-900 rounded-full" />}
        {status === 'TIMEOUT' && <AlertTriangle size={12} className="absolute -top-1 -right-1 text-red-500 bg-slate-900 rounded-full" />}
      </div>
    );
  };

  return (
    <div className="min-h-screen bg-[#0B1120] text-slate-300 font-sans overflow-hidden">

      {/* NAVBAR */}
      <nav className="border-b border-slate-800 bg-slate-900/50 backdrop-blur-md h-16 flex items-center justify-between px-6 sticky top-0 z-50">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center shadow-lg shadow-blue-500/20">
            <Server size={18} className="text-white" />
          </div>
          <div>
            <h1 className="text-white font-bold tracking-tight">NetViz <span className="text-blue-500">Pro</span></h1>
            <p className="text-[10px] uppercase text-slate-500">Reliable UDP Simulator</p>
          </div>
        </div>

        <div className="flex items-center gap-2 bg-slate-800/50 p-1 rounded-lg border border-slate-700/50">
          <button onClick={() => setIsPaused(!isPaused)} className={`p-2 rounded hover:bg-slate-700 transition ${isPaused ? 'text-yellow-400' : 'text-slate-400'}`}>
            {isPaused ? <Play size={16} /> : <Pause size={16} />}
          </button>
          <button onClick={handleReset} className="p-2 rounded hover:bg-slate-700 text-slate-400 hover:text-red-400">
            <RotateCcw size={16} />
          </button>
        </div>
      </nav>

      <main className="max-w-7xl mx-auto p-6 grid grid-cols-12 gap-6">

        {/* LEFT COLUMN */}
        <div className="col-span-8 space-y-6">

          {/* TRANSMISSION TAPE */}
          <section className="bg-slate-900 border border-slate-800 rounded-xl p-6 relative overflow-hidden">
            <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
              <Activity className="text-blue-500" size={18} /> Transmission Tape
            </h2>

            <div className="w-full overflow-hidden relative h-24 flex items-center mask-gradient">
              <div
                className="flex gap-3 absolute left-0 tape-container pl-10"
                style={{ transform: `translateX(-${(windowState.start * 64) - 100}px)` }}
              >
                {Array.from({ length: Math.max(50, windowState.end + 10) }, (_, i) => renderPacket(i))}
              </div>
            </div>
          </section>

          {/* FILE INFO + PROGRESS BAR (NEW) */}
          {progress.total > 0 && (
            <section className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-xl space-y-4">

              <h3 className="text-sm font-bold text-blue-400 uppercase tracking-wider">
                File Transfer Status
              </h3>

              {/* File Metadata */}
              <div className="text-xs text-slate-400 font-mono">
                <p><span className="text-slate-500">File:</span> {fileInfo.name}</p>
                <p><span className="text-slate-500">Size:</span> {Math.round(fileInfo.size / 1024)} KB</p>
              </div>

              {/* Progress Bar */}
              <div className="w-full bg-slate-800 rounded-lg h-3 overflow-hidden border border-slate-700">
                <div
                  className="h-full bg-blue-500 transition-all duration-300"
                  style={{ width: `${progress.percent}%` }}
                ></div>
              </div>

              <p className="text-xs font-mono text-blue-400">
                {progress.percent}% • {progress.received}/{progress.total} bytes
              </p>

              {/* Preview */}
              {previewUrl && (
                <div className="mt-4">
                  <p className="text-xs text-slate-400 mb-1">Preview:</p>
                  <img
                    src={previewUrl}
                    alt="preview"
                    className="w-48 h-auto rounded border border-slate-700 shadow-lg"
                  />
                </div>
              )}
            </section>
          )}

          {/* CONTROLS + METRICS */}
          <div className="grid grid-cols-2 gap-6">
            <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
              <h3 className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-4">
                <Settings size={14} /> Sim Settings
              </h3>

              <div className="space-y-4">
                <div>
                  <div className="flex justify-between text-xs mb-1">
                    <span>Window Size</span>
                    <span className="text-blue-400">{simSettings.windowSize}</span>
                  </div>
                  <input type="range" min="1" max="500" value={simSettings.windowSize}
                    onChange={(e) => {
                      const newSize = Number(e.target.value);
                      // update UI state
                      updateSim('windowSize', newSize);
                      // send to Node.js → Java
                      socket.emit("set_window_size", { size: newSize });
                    }}
                    className="w-full h-1 bg-slate-700 rounded cursor-pointer"
                  />
                </div>

                <div>
                  <div className="flex justify-between text-xs mb-1">
                    <span>Simulated Packet Loss</span>
                    <span className="text-red-400">{simSettings.lossChance}%</span>
                  </div>
                  <input type="range" min="0" max="50" value={simSettings.lossChance}
                    onChange={(e) => updateSim('lossChance', e.target.value)}
                    className="w-full h-1 bg-slate-700 rounded cursor-pointer accent-red-500"
                  />
                </div>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <MetricBox label="Packets Sent" value={metrics.sent} color="text-blue-400" />
              <MetricBox label="ACKs Recv" value={metrics.acked} color="text-emerald-400" />
              <MetricBox label="Timeouts" value={metrics.timeouts} color="text-red-400" />
              <MetricBox label="Retries" value={metrics.retransmits} color="text-orange-400" />
            </div>
          </div>

          {/* THROUGHPUT CHART */}
          <section className="bg-slate-900 border border-slate-800 rounded-xl p-6 h-64">
            <h3 className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-4">Throughput</h3>
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={throughputData}>
                <defs>
                  <linearGradient id="colorSeq" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                <XAxis dataKey="time" hide />
                <YAxis stroke="#475569" fontSize={10} />
                <Tooltip contentStyle={{ backgroundColor: '#0f172a', border: '1px solid #1e293b' }} />
                <Area type="monotone" dataKey="seq" stroke="#3b82f6" fill="url(#colorSeq)" isAnimationActive={false} />
              </AreaChart>
            </ResponsiveContainer>
          </section>
        </div>

        {/* RIGHT COLUMN — EVENT LOG */}
        <aside className="col-span-4 bg-slate-900 border border-slate-800 rounded-xl flex flex-col h-[calc(100vh-8rem)] sticky top-24">
          <div className="p-4 border-b border-slate-800 bg-slate-800/50 backdrop-blur">
            <h3 className="font-semibold text-white flex items-center gap-2 text-sm">
              <Clock size={16} className="text-slate-400" /> System Events
            </h3>
          </div>

          <div className="flex-1 overflow-y-auto p-2 space-y-1 font-mono text-xs custom-scrollbar">
            {logs.length === 0 && (
              <div className="flex flex-col items-center justify-center h-40 text-slate-600 space-y-2 mt-10">
                <Box size={24} className="opacity-20" />
                <span className="italic">Waiting for traffic...</span>
              </div>
            )}
            {logs.map((log, i) => <LogItem key={i} log={log} />)}
            <div ref={logsEndRef} />
          </div>
        </aside>
      </main>
    </div>
  );
};

// --- SUBCOMPONENTS ---
const MetricBox = ({ label, value, color }) => (
  <div className="bg-slate-900 border border-slate-800 p-3 rounded-lg">
    <p className="text-[10px] uppercase text-slate-500 font-bold">{label}</p>
    <p className={`text-xl font-bold mt-1 ${color}`}>{value}</p>
  </div>
);

const LogItem = ({ log }) => {
  let typeColor = "text-slate-500";
  let icon = <Activity size={10} />;

  if (log.event === "PACKET_SENT") { typeColor = "text-blue-400"; icon = <ArrowRight size={10} /> }
  if (log.event === "ACK_RECEIVED") { typeColor = "text-emerald-400"; icon = <CheckCircle2 size={10} /> }
  if (log.event === "TIMEOUT") { typeColor = "text-red-400"; icon = <AlertTriangle size={10} /> }
  if (log.event === "RETRANSMIT") { typeColor = "text-orange-400"; icon = <RefreshCw size={10} /> }

  return (
    <div className="flex gap-3 p-2 hover:bg-slate-800/50 rounded transition">
      <span className="text-slate-600 w-14">{log.time.split(' ')[0]}</span>
      <div className="flex-1">
        <div className={`flex items-center gap-1 font-bold ${typeColor}`}>
          {icon} {log.event.replace('PACKET_', '').replace('_RECEIVED', '')}
        </div>
        <div className="text-slate-400 pl-4 border-l border-slate-800 mt-1">
          {log.seq !== undefined && <span>SEQ {log.seq}</span>}
          {log.ack !== undefined && <span>ACK {log.ack}</span>}
        </div>
      </div>
    </div>
  );
};

export default App;
