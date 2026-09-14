/* eslint-disable react-hooks/exhaustive-deps */
/* eslint-disable no-unused-vars */
import React, { useEffect, useState, useRef } from "react";
import "./App.css";
import Chart from "chart.js/auto";

function App() {
  // State management
  const [activeTab, setActiveTab] = useState('dashboard');
  const [clock, setClock] = useState('--:-- -- | --- --, ----');
  const [countdown] = useState('15:00');
  const [intervalMins, setIntervalMins] = useState(15);
  const [lineSelect, setLineSelect] = useState('all');
  const [shiftSelect, setShiftSelect] = useState('current');
  
  // Data state
  const [kpiData, setKpiData] = useState({
    total: 0,
    spring: 0,
    hypnos: 0,
    targetPct: 0,
    targetSub: '0 / 0 units',
    cycle: '--',
    eff: '--%'
  });
  
  const [hmiLog, setHmiLog] = useState([]);
  const [prodLog, setProdLog] = useState([]);
  const [sizeBreakdown, setSizeBreakdown] = useState([]);
  const [efficiencyMeters, setEfficiencyMeters] = useState([]);
  const [progressRings, setProgressRings] = useState([]);
  const [shiftTime, setShiftTime] = useState({ endTime: '--:--', remaining: '--' });
  
  // Chart refs
  const hourlyChartRef = useRef(null);
  const chartsRef = useRef({});
  
  // Utility functions
  const rand = (min, max) => Math.floor(Math.random() * (max - min + 1)) + min;
  const randF = (min, max) => +(Math.random() * (max - min) + min).toFixed(1);
  
  const SIZES = ['King', 'Queen', 'Double', 'Single'];
  const VARIETIES_SPRING = ['Bonnell', 'Pocket', 'Offset', 'Continuous'];
  const VARIETIES_HYPNOS = ['Comfort', 'Ortho', 'Pillow Top', 'Euro Top'];
  // eslint-disable-next-line no-unused-vars
  const SIZE_COLORS = ['#2E7AAB', '#1B4F6A', '#3E9AD0', '#7ABCD5'];
  
  const TARGETS = {
    hourlySpring: 72,
    hourlyHypnos: 48,
    hourlyTotal: 120,
    daily: 960,
    weekly: 4800,
    monthly: 20000,
    shift: 960,
    cycleTime: 4.5
  };
  
  // Generate hourly data
  const generateHourlyData = () => {
    const now = new Date();
    const hour = now.getHours();
    let data = [];
    
    for (let h = 6; h <= hour; h++) {
      const spring = rand(55, 80);
      const hypnos = rand(35, 55);
      data.push({
        hour: h,
        spring: spring,
        hypnos: hypnos,
        total: spring + hypnos,
        king: rand(10, 20),
        queen: rand(20, 35),
        double: rand(15, 25),
        single: rand(5, 15),
        efficiency: randF(82, 97)
      });
    }
    return data;
  };
  
  // Generate HMI record
  const generateHMIRecord = () => {
    const type = Math.random() > 0.45 ? 'Spring' : 'Hypnos';
    const size = SIZES[rand(0, 3)];
    const variety = type === 'Spring' ? VARIETIES_SPRING[rand(0, 3)] : VARIETIES_HYPNOS[rand(0, 3)];
    const count = rand(8, 24);
    const cycle = randF(3.8, 5.6);
    
    const now = new Date();
    const hh = String(now.getHours()).padStart(2, '0');
    const mm = String(now.getMinutes()).padStart(2, '0');
    
    const statuses = ['Complete', 'Complete', 'Complete', 'In Progress', 'Delayed'];
    const status = statuses[rand(0, 4)];
    
    return {
      time: `${hh}:${mm}`,
      type,
      variety,
      size,
      count,
      cycle,
      status,
      line: rand(1, 4)
    };
  };
  
  // Initialize dashboard data
  const initDashboard = () => {
    const hourlyData = generateHourlyData();
    
    // Calculate totals
    const springTotal = hourlyData.reduce((sum, row) => sum + row.spring, 0);
    const hypnosTotal = hourlyData.reduce((sum, row) => sum + row.hypnos, 0);
    const total = springTotal + hypnosTotal;
    const springShift = Math.round(springTotal * 0.6);
    const hypnosShift = Math.round(hypnosTotal * 0.6);
    const totalShift = springShift + hypnosShift;
    const targetPct = Math.round(totalShift / TARGETS.shift * 100);
    
    // Update KPI data
    setKpiData({
      total: total.toLocaleString(),
      spring: springTotal.toLocaleString(),
      hypnos: hypnosTotal.toLocaleString(),
      targetPct: targetPct + '%',
      targetSub: `${totalShift} / ${TARGETS.shift} units`,
      cycle: randF(4.1, 5.0),
      eff: randF(85, 93) + '%'
    });
    
    // Generate size breakdown
    const sizes = { King: 0, Queen: 0, Double: 0, Single: 0 };
    hourlyData.forEach(row => {
      sizes.King += row.king;
      sizes.Queen += row.queen;
      sizes.Double += row.double;
      sizes.Single += row.single;
    });
    
    const sTotal = Object.values(sizes).reduce((a, b) => a + b, 0);
    const sizeHTML = Object.entries(sizes).map(([name, val]) => {
      const pct = Math.round(val / sTotal * 100);
      return {
        name,
        val,
        pct,
        color: { King: '#2E7AAB', Queen: '#1B4F6A', Double: '#3E9AD0', Single: '#7ABCD5' }[name]
      };
    });
    setSizeBreakdown(sizeHTML);
    
    // Initialize HMI log
    const hmiRecords = Array.from({ length: 8 }, () => generateHMIRecord());
    setHmiLog(hmiRecords);
    
    // Generate progress rings
    const springPct = Math.round(springShift / (TARGETS.shift * 0.6) * 100);
    const hypnosPct = Math.round(hypnosShift / (TARGETS.shift * 0.4) * 100);
    setProgressRings([
      { label: 'Spring', pct: springPct, color: '#2E7AAB' },
      { label: 'Hypnos', pct: hypnosPct, color: '#1B4F6A' },
      { label: 'Total', pct: targetPct, color: '#3E9AD0' }
    ]);
    
    // Calculate shift time
    const now = new Date();
    const shiftEnd = new Date();
    shiftEnd.setHours(22, 0, 0, 0);
    const remain = Math.max(0, shiftEnd - now);
    const rh = Math.floor(remain / 3600000);
    const rm = Math.floor((remain % 3600000) / 60000);
    setShiftTime({
      endTime: '22:00',
      remaining: `${rh}h ${rm}m`
    });
    
    // Generate efficiency meters
    const effMetrics = [
      { label: 'Availability', val: randF(88, 96), color: '#2E7AAB' },
      { label: 'Performance', val: randF(84, 94), color: '#1B4F6A' },
      { label: 'Quality', val: randF(91, 99), color: '#3E9AD0' },
      { label: 'OEE', val: randF(83, 92), color: '#2A7D5B' }
    ];
    setEfficiencyMeters(effMetrics);
    
    // Generate production log
    const prodRecords = Array.from({ length: 10 }, () => generateHMIRecord());
    setProdLog(prodRecords);
    
    // Create hourly chart
    createHourlyChart(hourlyData);
  };
  
  // Create hourly chart
  const createHourlyChart = (hourlyData) => {
    const ctx = hourlyChartRef.current;
    if (!ctx) return;
    
    if (chartsRef.current.hourlyChart) {
      chartsRef.current.hourlyChart.destroy();
    }
    
    const hours = hourlyData.map(r => r.hour + ':00');
    const target = hourlyData.map(() => TARGETS.hourlyTotal);
    
    Chart.defaults.font.family = "'Lora', Georgia, serif";
    Chart.defaults.color = '#4A6A7D';
    
    chartsRef.current.hourlyChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: hours,
        datasets: [
          {
            label: 'Spring',
            data: hourlyData.map(r => r.spring),
            backgroundColor: 'rgba(46,122,171,0.7)',
            borderColor: '#2E7AAB',
            borderWidth: 1
          },
          {
            label: 'Hypnos',
            data: hourlyData.map(r => r.hypnos),
            backgroundColor: 'rgba(27,79,106,0.7)',
            borderColor: '#1B4F6A',
            borderWidth: 1
          },
          {
            label: 'Target',
            data: target,
            type: 'line',
            borderColor: '#C9952E',
            borderDash: [4, 4],
            borderWidth: 1.5,
            pointRadius: 0,
            backgroundColor: 'transparent',
            fill: false
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'top',
            labels: { boxWidth: 10, padding: 14, font: { size: 10 } }
          },
          tooltip: { mode: 'index' }
        },
        scales: {
          x: {
            grid: { color: '#EEF5FA' },
            ticks: { font: { size: 10 } },
            stacked: true
          },
          y: {
            grid: { color: '#EEF5FA' },
            ticks: { font: { size: 10 } },
            stacked: true,
            suggestedMin: 0
          }
        }
      }
    });
  };
  
  // Update clock
  const updateClock = () => {
    const now = new Date();
    const days = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
    const months = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
    
    let h = now.getHours();
    const m = now.getMinutes();
    const s = now.getSeconds();
    const ampm = h >= 12 ? 'PM' : 'AM';
    h = h % 12 || 12;
    
    setClock(`${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')} ${ampm}  |  ${days[now.getDay()]}, ${months[now.getMonth()]} ${now.getDate()}, ${now.getFullYear()}`);
  };
  
  // Simulate HMI push
  const simulateHMI = () => {
    const newRecord = generateHMIRecord();
    setHmiLog(prev => {
      const updated = [...prev, newRecord];
      if (updated.length > 20) updated.shift();
      return updated;
    });
  };
  
  // Refresh data
  const refreshData = () => {
    initDashboard();
  };
  
  // Handle interval change
  const handleIntervalChange = (e) => {
    const mins = parseInt(e.target.value);
    setIntervalMins(mins);
    // Reset countdown would go here
  };
  
  // Make ring SVG
  const makeRing = (label, pct, color) => {
    pct = Math.min(100, pct);
    const r = 40;
    const circ = 2 * Math.PI * r;
    const dash = (pct / 100) * circ;
    
    return (
      <div className="ring-item">
        <svg className="ring-svg" width="100" height="100" viewBox="0 0 100 100">
          <circle cx="50" cy="50" r={r} fill="none" stroke="#DDEEF8" strokeWidth="8"/>
          <circle cx="50" cy="50" r={r} fill="none" stroke={color} strokeWidth="8"
            strokeDasharray={`${dash.toFixed(1)} ${circ.toFixed(1)}`}
            strokeDashoffset={(circ / 4).toFixed(1)}
            strokeLinecap="round" transform="rotate(-90 50 50)"
            style={{transition: 'stroke-dasharray 1s ease'}}/>
          <text x="50" y="46" textAnchor="middle" fontFamily="Playfair Display,serif" fontSize="16" fontWeight="700" fill="#1A2E3B">{pct}%</text>
          <text x="50" y="60" textAnchor="middle" fontFamily="Lora,serif" fontSize="9" fill="#7A9DB5">{label}</text>
        </svg>
      </div>
    );
  };
  
  // Initialize on mount
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => {
    initDashboard();
    updateClock();
    const clockInterval = setInterval(updateClock, 1000);
    
    return () => clearInterval(clockInterval);
  }, []);
  
  // Handle tab switching
  const handleTabChange = (tabName) => {
    setActiveTab(tabName);
  };
  
  const getStatusClass = (status) => {
    switch (status) {
      case 'Complete': return 'status-complete';
      case 'Delayed': return 'status-delayed';
      default: return 'status-progress';
    }
  };
  
  const getBadgeClass = (type) => {
    return type === 'Spring' ? 'badge-spring' : 'badge-hypnos';
  };
  
  return (
    <div>
      {/* HEADER */}
      <header className="header">
        <div className="header-left">
          <div className="brand">Peps Mattress</div>
          <div className="brand-sub">Production Display System</div>
        </div>
        <div className="header-right">
          <div className="live-badge">
            <div className="live-dot"></div>
            Live Feed
          </div>
          <div className="clock">{clock}</div>
        </div>
      </header>

      {/* NAV */}
      <nav className="nav">
        <button className={`nav-tab ${activeTab === 'dashboard' ? 'active' : ''}`} onClick={() => handleTabChange('dashboard')}>Dashboard</button>
        <button className={`nav-tab ${activeTab === 'hourly' ? 'active' : ''}`} onClick={() => handleTabChange('hourly')}>Hourly</button>
        <button className={`nav-tab ${activeTab === 'daily' ? 'active' : ''}`} onClick={() => handleTabChange('daily')}>Daily</button>
        <button className={`nav-tab ${activeTab === 'weekly' ? 'active' : ''}`} onClick={() => handleTabChange('weekly')}>Weekly</button>
        <button className={`nav-tab ${activeTab === 'monthly' ? 'active' : ''}`} onClick={() => handleTabChange('monthly')}>Monthly</button>
        <button className={`nav-tab ${activeTab === 'crm' ? 'active' : ''}`} onClick={() => handleTabChange('crm')}>CRM Metrics</button>
        <button className={`nav-tab ${activeTab === 'settings' ? 'active' : ''}`} onClick={() => handleTabChange('settings')}>Settings</button>
      </nav>

      {/* CONTROLS BAR */}
      <div className="controls-bar">
        <div className="control-group">
          <span className="control-label">Update Interval</span>
          <select className="control-select" value={intervalMins} onChange={handleIntervalChange}>
            <option value={15}>Every 15 min</option>
            <option value={30}>Every 30 min</option>
            <option value={60}>Every 60 min</option>
            <option value={5}>Every 5 min (demo)</option>
          </select>
        </div>
        <div className="control-group">
          <span className="control-label">Line</span>
          <select className="control-select" value={lineSelect} onChange={(e) => setLineSelect(e.target.value)}>
            <option value="all">All Lines</option>
            <option value="spring">Spring Line</option>
            <option value="hypnos">Hypnos Line</option>
          </select>
        </div>
        <div className="control-group">
          <span className="control-label">Shift</span>
          <select className="control-select" value={shiftSelect} onChange={(e) => setShiftSelect(e.target.value)}>
            <option value="current">Current Shift</option>
            <option value="A">Shift A (06:00 – 14:00)</option>
            <option value="B">Shift B (14:00 – 22:00)</option>
            <option value="C">Shift C (22:00 – 06:00)</option>
          </select>
        </div>
        <button className="control-btn secondary" onClick={refreshData}>Refresh Now</button>
        <button className="control-btn" onClick={simulateHMI}>Simulate HMI Push</button>
        <div className="next-update">Next auto-update in <span>{countdown}</span></div>
      </div>

      {/* DASHBOARD TAB */}
      <div className={`main ${activeTab === 'dashboard' ? 'active' : ''}`} id="tab-dashboard">
        {/* KPI STRIP */}
        <div className="kpi-strip">
          <div className="kpi-card spring-accent">
            <div className="kpi-label">Total Produced Today</div>
            <div className="kpi-value">{kpiData.total}</div>
            <div className="kpi-sub">units across all lines</div>
            <div className="kpi-delta up">+{rand(3, 8)}% vs yesterday</div>
          </div>
          <div className="kpi-card spring-accent">
            <div className="kpi-label">Spring Mattresses</div>
            <div className="kpi-value">{kpiData.spring}</div>
            <div className="kpi-sub">units today</div>
            <div className="kpi-delta up">+{rand(2, 6)}% vs yesterday</div>
          </div>
          <div className="kpi-card hypnos-accent">
            <div className="kpi-label">Hypnos Mattresses</div>
            <div className="kpi-value">{kpiData.hypnos}</div>
            <div className="kpi-sub">units today</div>
            <div className="kpi-delta up">+{rand(1, 5)}% vs yesterday</div>
          </div>
          <div className="kpi-card success-accent">
            <div className="kpi-label">Shift Target</div>
            <div className="kpi-value">{kpiData.targetPct}</div>
            <div className="kpi-sub">{kpiData.targetSub}</div>
            <div className="kpi-delta neutral">Current Shift</div>
          </div>
          <div className="kpi-card warn-accent">
            <div className="kpi-label">Avg Cycle Time</div>
            <div className="kpi-value">{kpiData.cycle}</div>
            <div className="kpi-sub">minutes per unit</div>
            <div className="kpi-delta neutral">Target: 4.5 min</div>
          </div>
          <div className="kpi-card">
            <div className="kpi-label">Line Efficiency</div>
            <div className="kpi-value">{kpiData.eff}</div>
            <div className="kpi-sub">OEE this shift</div>
            <div className="kpi-delta up">+{randF(0.5, 2.5)}% vs last shift</div>
          </div>
        </div>

        {/* ROW 1: Hourly Trend + Size Breakdown */}
        <div className="grid-2-1">
          <div className="card">
            <div className="card-header">
              <div>
                <div className="card-title">Hourly Production — Today</div>
                <div className="card-subtitle">Spring vs Hypnos, units per hour</div>
              </div>
              <span className="badge badge-success">Live</span>
            </div>
            <div className="card-body">
              <div className="chart-wrap" style={{height: '200px'}}>
                <canvas ref={hourlyChartRef}></canvas>
              </div>
            </div>
          </div>

          <div className="card">
            <div className="card-header">
              <div>
                <div className="card-title">Size Distribution</div>
                <div className="card-subtitle">Today's production</div>
              </div>
            </div>
            <div className="card-body">
              {sizeBreakdown.map((item, idx) => (
                <div className="size-row" key={idx}>
                  <div className="size-name">{item.name}</div>
                  <div className="size-bar-wrap">
                    <div className="size-bar" style={{width: item.pct + '%', background: item.color}}></div>
                  </div>
                  <div className="size-count">{item.val}</div>
                  <div className="size-pct">{item.pct}%</div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* ROW 2: HMI Feed + Target Progress + Efficiency */}
        <div className="grid-3">
          <div className="card">
            <div className="card-header">
              <div>
                <div className="card-title">HMI / PLC Live Feed</div>
                <div className="card-subtitle">Incoming production records</div>
              </div>
              <span className="badge badge-spring">Auto</span>
            </div>
            <div className="card-body" style={{padding: '0.75rem 1rem'}}>
              <div className="hmi-feed">
                {hmiLog.slice(-10).reverse().map((row, idx) => (
                  <div className="hmi-row" key={idx}>
                    <div className="hmi-time">{row.time}</div>
                    <div className="hmi-type">
                      <span className={`badge ${getBadgeClass(row.type)}`} style={{fontSize: '0.62rem'}}>{row.type}</span>
                    </div>
                    <div className="hmi-size">{row.size} / {row.variety}</div>
                    <div className="hmi-count">{row.count}</div>
                    <div className="hmi-status">
                      <span className={`status-pill ${getStatusClass(row.status)}`}>{row.status}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>

          <div className="card">
            <div className="card-header">
              <div>
                <div className="card-title">Shift Progress</div>
                <div className="card-subtitle">Target attainment by line</div>
              </div>
            </div>
            <div className="card-body">
              <div className="ring-wrap">
                {progressRings.map((ring, idx) => makeRing(ring.label, ring.pct, ring.color))}
              </div>
              <div style={{marginTop: '1rem', fontSize: '0.78rem', color: 'var(--text-muted)', textAlign: 'center'}}>
                Shift ends at <strong>{shiftTime.endTime}</strong> &nbsp;&middot;&nbsp; <span>{shiftTime.remaining}</span> remaining
              </div>
            </div>
          </div>

          <div className="card">
            <div className="card-header">
              <div>
                <div className="card-title">Line Efficiency</div>
                <div className="card-subtitle">OEE components this shift</div>
              </div>
            </div>
            <div className="card-body">
              <div className="eff-meter">
                {efficiencyMeters.map((eff, idx) => (
                  <div className="eff-row" key={idx}>
                    <div className="eff-label">{eff.label}</div>
                    <div className="eff-bar-wrap">
                      <div className="eff-bar" style={{width: eff.val + '%', background: eff.color}}></div>
                    </div>
                    <div className="eff-val" style={{color: eff.color}}>{eff.val}%</div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* ROW 3: Recent Completion Log */}
        <div className="card" style={{marginBottom: '1rem'}}>
          <div className="card-header">
            <div>
              <div className="card-title">Recent Production Log</div>
              <div className="card-subtitle">Last 10 completed batches from HMI</div>
            </div>
            <span className="badge badge-hypnos">PLC Source</span>
          </div>
          <div className="card-body" style={{padding: 0}}>
            <table className="prod-table">
              <thead>
                <tr>
                  <th>Time</th>
                  <th>Product Type</th>
                  <th>Variety</th>
                  <th>Size</th>
                  <th>Count</th>
                  <th>Cycle (min)</th>
                  <th>Line</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {prodLog.map((row, idx) => (
                  <tr key={idx}>
                    <td>{row.time}</td>
                    <td><span className={`badge ${getBadgeClass(row.type)}`}>{row.type}</span></td>
                    <td>{row.variety}</td>
                    <td>{row.size}</td>
                    <td className="num">{row.count}</td>
                    <td>{row.cycle}</td>
                    <td>Line {row.line}</td>
                    <td><span className={`status-pill ${getStatusClass(row.status)}`}>{row.status}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* OTHER TABS (Placeholder for now) */}
      {['hourly', 'daily', 'weekly', 'monthly', 'crm', 'settings'].map(tab => (
        <div key={tab} className={`main ${activeTab === tab ? 'active' : ''}`} id={`tab-${tab}`}>
          <div className="card">
            <div className="card-body">
              <div className="card-title" style={{textAlign: 'center', padding: '2rem'}}>
                {tab.charAt(0).toUpperCase() + tab.slice(1)} Tab - Coming Soon
              </div>
            </div>
          </div>
        </div>
      ))}

      <footer className="footer">
        Peps Mattress Automated Production Display System &nbsp;&middot;&nbsp; HMI/PLC Integration Layer &nbsp;&middot;&nbsp; Data refreshes per configured interval
      </footer>
    </div>
  );
}

export default App;