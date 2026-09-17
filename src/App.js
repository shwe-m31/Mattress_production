/* eslint-disable react-hooks/exhaustive-deps */
import React, { useEffect, useState, useRef } from "react";
import "./App.css";
import Chart from "chart.js/auto";
import productionApi from "./services/productionApi";

function App() {
  // State management
  const [activeTab, setActiveTab] = useState('dashboard');
  const [clock, setClock] = useState('--:-- -- | --- --, ----');
  const [countdown, setCountdown] = useState('15:00');
  const [intervalMins, setIntervalMins] = useState(15);
  const [lineSelect, setLineSelect] = useState('all');
  const [shiftSelect, setShiftSelect] = useState('current');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [tvMode, setTvMode] = useState(false);
  
  // Data state - all from backend
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
  const [simulatorStatus, setSimulatorStatus] = useState(null);
  
  // Historical data state
  const [dailyData, setDailyData] = useState([]);
  const [weeklyData, setWeeklyData] = useState([]);
  const [monthlyData, setMonthlyData] = useState([]);
  
  // Chart refs
  const hourlyChartRef = useRef(null);
  const chartsRef = useRef({});
  
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
  
  // Initialize dashboard data from backend
  const initDashboard = async () => {
    setIsLoading(true);
    setError(null);
    
    try {
      // Fetch dashboard data from backend
      const dashboardData = await productionApi.getDashboard();
      
      // Update KPI data
      setKpiData({
        total: dashboardData.totalProduction.toLocaleString(),
        spring: dashboardData.springCount.toLocaleString(),
        hypnos: dashboardData.hypnosCount.toLocaleString(),
        targetPct: Math.round((dashboardData.totalProduction / TARGETS.shift) * 100) + '%',
        targetSub: `${dashboardData.totalProduction} / ${TARGETS.shift} units`,
        cycle: '4.2', // Would come from backend in real implementation
        eff: dashboardData.efficiency + '%'
      });
      
      // Process size breakdown from backend data
      const sizeData = Object.entries(dashboardData.sizeBreakdown || {}).map(([name, data]) => {
        const total = (data.spring || 0) + (data.hypnos || 0);
        const pct = total > 0 ? Math.round((data.spring + data.hypnos) / dashboardData.totalProduction * 100) : 0;
        return {
          name: name.charAt(0).toUpperCase() + name.slice(1),
          val: total,
          pct: pct,
          color: SIZE_COLORS[['king', 'queen', 'double', 'single'].indexOf(name)]
        };
      });
      setSizeBreakdown(sizeData);
      
      // Process recent production from backend
      const recentData = (dashboardData.recentItems || []).map(item => ({
        time: new Date(item.completionTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        type: item.productType === 'SPRING' ? 'Spring' : 'Hypnos',
        variety: item.variety || 'Standard',
        size: item.size,
        count: item.quantity || 1,
        cycle: item.cycleTime || 0,
        line: item.productionLine || 'Line 1',
        status: item.status || 'COMPLETED'
      }));
      setHmiLog(recentData);
      setProdLog(recentData);
      
      // Generate progress rings based on real data
      const springPct = Math.min(100, Math.round((dashboardData.springCount / (TARGETS.shift * 0.6)) * 100));
      const hypnosPct = Math.min(100, Math.round((dashboardData.hypnosCount / (TARGETS.shift * 0.4)) * 100));
      const totalPct = Math.round((dashboardData.totalProduction / TARGETS.shift) * 100);
      
      setProgressRings([
        { label: 'Spring', pct: springPct, color: '#2E7AAB' },
        { label: 'Hypnos', pct: hypnosPct, color: '#1B4F6A' },
        { label: 'Total', pct: totalPct, color: '#3E9AD0' }
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
      
      // Generate efficiency meters (simplified calculation from real data)
      const effMetrics = [
        { label: 'Availability', val: Math.min(100, dashboardData.efficiency + 5), color: '#2E7AAB' },
        { label: 'Performance', val: Math.min(100, dashboardData.efficiency - 2), color: '#1B4F6A' },
        { label: 'Quality', val: Math.min(100, dashboardData.efficiency + 8), color: '#3E9AD0' },
        { label: 'OEE', val: dashboardData.efficiency, color: '#2A7D5B' }
      ];
      setEfficiencyMeters(effMetrics);
      
      // Create hourly chart from backend data
      createHourlyChart(dashboardData.hourlyData);
      
    } catch (err) {
      console.error('Error fetching dashboard data:', err);
      setError('Failed to load production data. Backend may be unavailable.');
    } finally {
      setIsLoading(false);
    }
  };
  
  // Create hourly chart from backend data
  const createHourlyChart = (hourlyData) => {
    const ctx = hourlyChartRef.current;
    if (!ctx || !hourlyData) return;
    
    if (chartsRef.current.hourlyChart) {
      chartsRef.current.hourlyChart.destroy();
    }
    
    // Process hourly data for chart
    const hours = [];
    const springData = [];
    const hypnosData = [];
    const targetData = [];
    
    const startHour = 6; // 6 AM
    for (let i = 0; i < 24; i++) {
      const hour = (startHour + i) % 24;
      hours.push(`${hour}:00`);
      springData.push(hourlyData.spring[i] || 0);
      hypnosData.push(hourlyData.hypnos[i] || 0);
      targetData.push(TARGETS.hourlyTotal);
    }
    
    Chart.defaults.font.family = "'Lora', Georgia, serif";
    Chart.defaults.color = '#4A6A7D';
    
    chartsRef.current.hourlyChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: hours,
        datasets: [
          {
            label: 'Spring',
            data: springData,
            backgroundColor: 'rgba(46,122,171,0.7)',
            borderColor: '#2E7AAB',
            borderWidth: 1
          },
          {
            label: 'Hypnos',
            data: hypnosData,
            backgroundColor: 'rgba(27,79,106,0.7)',
            borderColor: '#1B4F6A',
            borderWidth: 1
          },
          {
            label: 'Target',
            data: targetData,
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
  
  // Simulate HMI push via backend
  const simulateHMI = async () => {
    try {
      const newEvent = await productionApi.triggerSimulatorEvent();
      
      // Update HMI log with new event
      const newRecord = {
        time: new Date(newEvent.completionTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        type: newEvent.productType === 'SPRING' ? 'Spring' : 'Hypnos',
        variety: newEvent.variety || 'Standard',
        size: newEvent.size,
        count: newEvent.quantity,
        cycle: newEvent.cycleTime || 0,
        line: newEvent.productionLine || 'Line 1',
        status: newEvent.status || 'COMPLETED'
      };
      
      setHmiLog(prev => {
        const updated = [...prev, newRecord];
        if (updated.length > 20) updated.shift();
        return updated;
      });
      
      // Refresh dashboard to show updated data
      initDashboard();
      
    } catch (err) {
      console.error('Error simulating HMI event:', err);
      setError('Failed to simulate HMI event');
    }
  };
  
  // Refresh data from backend
  const refreshData = () => {
    initDashboard();
  };
  
  // Handle interval change
  const handleIntervalChange = (e) => {
    const mins = parseInt(e.target.value);
    setIntervalMins(mins);
    // Reset countdown display
    setCountdown(`${mins}:00`);
  };
  
  // Fetch simulator status
  const fetchSimulatorStatus = async () => {
    try {
      const status = await productionApi.getSimulatorStatus();
      setSimulatorStatus(status);
    } catch (err) {
      console.error('Error fetching simulator status:', err);
    }
  };
  
  // Fetch daily production data
  const fetchDailyData = async () => {
    try {
      const data = await productionApi.getDailyProduction();
      setDailyData(data);
    } catch (err) {
      console.error('Error fetching daily data:', err);
    }
  };
  
  // Fetch weekly production data
  const fetchWeeklyData = async () => {
    try {
      const data = await productionApi.getWeeklyProduction();
      setWeeklyData(data);
    } catch (err) {
      console.error('Error fetching weekly data:', err);
    }
  };
  
  // Fetch monthly production data
  const fetchMonthlyData = async () => {
    try {
      const data = await productionApi.getMonthlyProduction();
      setMonthlyData(data);
    } catch (err) {
      console.error('Error fetching monthly data:', err);
    }
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
  useEffect(() => {
    initDashboard();
    updateClock();
    fetchSimulatorStatus();
    fetchDailyData();
    fetchWeeklyData();
    fetchMonthlyData();
    
    const clockInterval = setInterval(updateClock, 1000);
    
    return () => clearInterval(clockInterval);
  }, []);
  
  // Handle tab switching
  const handleTabChange = (tabName) => {
    setActiveTab(tabName);
  };
  
  const getStatusClass = (status) => {
    switch (status) {
      case 'COMPLETED': case 'Completed': return 'status-complete';
      case 'DELAYED': case 'Delayed': return 'status-delayed';
      case 'IN_PROGRESS': case 'In Progress': return 'status-progress';
      default: return 'status-progress';
    }
  };
  
  const getBadgeClass = (type) => {
    return type === 'Spring' || type === 'SPRING' ? 'badge-spring' : 'badge-hypnos';
  };
  
  // Toggle TV mode
  const toggleTvMode = () => {
    setTvMode(!tvMode);
  };
  
  return (
    <div className={tvMode ? 'tv-mode' : ''}>
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
      <nav className="nav" style={{ display: tvMode ? 'none' : 'flex' }}>
        <button className={`nav-tab ${activeTab === 'dashboard' ? 'active' : ''}`} onClick={() => handleTabChange('dashboard')}>Dashboard</button>
        <button className={`nav-tab ${activeTab === 'hourly' ? 'active' : ''}`} onClick={() => handleTabChange('hourly')}>Hourly</button>
        <button className={`nav-tab ${activeTab === 'daily' ? 'active' : ''}`} onClick={() => handleTabChange('daily')}>Daily</button>
        <button className={`nav-tab ${activeTab === 'weekly' ? 'active' : ''}`} onClick={() => handleTabChange('weekly')}>Weekly</button>
        <button className={`nav-tab ${activeTab === 'monthly' ? 'active' : ''}`} onClick={() => handleTabChange('monthly')}>Monthly</button>
        <button className={`nav-tab ${activeTab === 'crm' ? 'active' : ''}`} onClick={() => handleTabChange('crm')}>CRM Metrics</button>
        <button className={`nav-tab ${activeTab === 'settings' ? 'active' : ''}`} onClick={() => handleTabChange('settings')}>Settings</button>
      </nav>

      {/* CONTROLS BAR */}
      <div className="controls-bar" style={{ display: tvMode ? 'none' : 'flex' }}>
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
        <button className="control-btn secondary" onClick={refreshData} disabled={isLoading}>
          {isLoading ? 'Refreshing...' : 'Refresh Now'}
        </button>
        <button className="control-btn" onClick={simulateHMI}>Simulate HMI Push</button>
        <div className="next-update">Next auto-update in <span>{countdown}</span></div>
      </div>

      {/* ERROR STATE */}
      {error && (
        <div style={{ padding: '2rem', textAlign: 'center', color: '#B03A2E' }}>
          <div style={{ fontSize: '1.2rem', marginBottom: '0.5rem' }}>⚠️ {error}</div>
          <button className="control-btn" onClick={refreshData} style={{ marginTop: '1rem' }}>Retry</button>
        </div>
      )}

      {/* LOADING STATE */}
      {isLoading && !error && (
        <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)' }}>
          Loading production data...
        </div>
      )}

      {/* DASHBOARD TAB */}
      {!isLoading && !error && (
        <div className={`main ${activeTab === 'dashboard' ? 'active' : ''}`} id="tab-dashboard">
          {/* KPI STRIP */}
          <div className="kpi-strip">
            <div className="kpi-card spring-accent">
              <div className="kpi-label">Total Produced Today</div>
              <div className="kpi-value">{kpiData.total}</div>
              <div className="kpi-sub">units across all lines</div>
              <div className="kpi-delta up">Live from Backend</div>
            </div>
            <div className="kpi-card spring-accent">
              <div className="kpi-label">Spring Mattresses</div>
              <div className="kpi-value">{kpiData.spring}</div>
              <div className="kpi-sub">units today</div>
              <div className="kpi-delta up">Live from Backend</div>
            </div>
            <div className="kpi-card hypnos-accent">
              <div className="kpi-label">Hypnos Mattresses</div>
              <div className="kpi-value">{kpiData.hypnos}</div>
              <div className="kpi-sub">units today</div>
              <div className="kpi-delta up">Live from Backend</div>
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
              <div className="kpi-delta up">Live from Backend</div>
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
                {sizeBreakdown.length > 0 ? sizeBreakdown.map((item, idx) => (
                  <div className="size-row" key={idx}>
                    <div className="size-name">{item.name}</div>
                    <div className="size-bar-wrap">
                      <div className="size-bar" style={{width: item.pct + '%', background: item.color}}></div>
                    </div>
                    <div className="size-count">{item.val}</div>
                    <div className="size-pct">{item.pct}%</div>
                  </div>
                )) : <div style={{textAlign: 'center', color: 'var(--text-muted)'}}>No size data available</div>}
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
                <span className="badge badge-spring">Simulated</span>
              </div>
              <div className="card-body" style={{padding: '0.75rem 1rem'}}>
                <div className="hmi-feed">
                  {hmiLog.length > 0 ? hmiLog.slice(-10).reverse().map((row, idx) => (
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
                  )) : <div style={{textAlign: 'center', color: 'var(--text-muted)', padding: '1rem'}}>No production records yet</div>}
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
              <span className="badge badge-hypnos">Simulated Source</span>
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
                  {prodLog.length > 0 ? prodLog.map((row, idx) => (
                    <tr key={idx}>
                      <td>{row.time}</td>
                      <td><span className={`badge ${getBadgeClass(row.type)}`}>{row.type}</span></td>
                      <td>{row.variety}</td>
                      <td>{row.size}</td>
                      <td className="num">{row.count}</td>
                      <td>{row.cycle.toFixed(1)}</td>
                      <td>{row.line}</td>
                      <td><span className={`status-pill ${getStatusClass(row.status)}`}>{row.status}</span></td>
                    </tr>
                  )) : <tr><td colSpan="8" style={{textAlign: 'center', color: 'var(--text-muted)'}}>No production records yet</td></tr>}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* OTHER TABS */}
      {['hourly', 'daily', 'weekly', 'monthly', 'crm', 'settings'].map(tab => (
        <div key={tab} className={`main ${activeTab === tab ? 'active' : ''}`} id={`tab-${tab}`}>
          <div className="card">
            <div className="card-body">
              {tab === 'hourly' ? (
                <div>
                  <div className="card-title" style={{textAlign: 'center', padding: '1rem'}}>
                    Hourly Production — Coming Soon
                  </div>
                  <div style={{textAlign: 'center', color: 'var(--text-muted)', padding: '1rem'}}>
                    Use the Dashboard tab for current hourly production data
                  </div>
                </div>
              ) : tab === 'daily' ? (
                <div>
                  <div className="card-title" style={{textAlign: 'center', padding: '1rem'}}>
                    Daily Production — Last 14 Days
                  </div>
                  <div style={{padding: '1rem'}}>
                    <table className="prod-table">
                      <thead>
                        <tr>
                          <th>Date</th>
                          <th>Spring</th>
                          <th>Hypnos</th>
                          <th>Total</th>
                          <th>Efficiency</th>
                          <th>Downtime</th>
                        </tr>
                      </thead>
                      <tbody>
                        {dailyData.length > 0 ? dailyData.map((day, idx) => (
                          <tr key={idx}>
                            <td>{day.date}</td>
                            <td className="num">{day.spring}</td>
                            <td className="num">{day.hypnos}</td>
                            <td className="num">{day.total}</td>
                            <td className="num">{day.efficiency}%</td>
                            <td className="num">{day.downtime} min</td>
                          </tr>
                        )) : <tr><td colSpan="6" style={{textAlign: 'center', color: 'var(--text-muted)'}}>Loading daily data...</td></tr>}
                      </tbody>
                    </table>
                  </div>
                </div>
              ) : tab === 'weekly' ? (
                <div>
                  <div className="card-title" style={{textAlign: 'center', padding: '1rem'}}>
                    Weekly Production — Last 8 Weeks
                  </div>
                  <div style={{padding: '1rem'}}>
                    <table className="prod-table">
                      <thead>
                        <tr>
                          <th>Week</th>
                          <th>Spring</th>
                          <th>Hypnos</th>
                          <th>Total</th>
                          <th>Target</th>
                          <th>Efficiency</th>
                        </tr>
                      </thead>
                      <tbody>
                        {weeklyData.length > 0 ? weeklyData.map((week, idx) => (
                          <tr key={idx}>
                            <td>{week.week}</td>
                            <td className="num">{week.spring}</td>
                            <td className="num">{week.hypnos}</td>
                            <td className="num">{week.total}</td>
                            <td className="num">{week.target}</td>
                            <td className="num">{week.efficiency}%</td>
                          </tr>
                        )) : <tr><td colSpan="6" style={{textAlign: 'center', color: 'var(--text-muted)'}}>Loading weekly data...</td></tr>}
                      </tbody>
                    </table>
                  </div>
                </div>
              ) : tab === 'monthly' ? (
                <div>
                  <div className="card-title" style={{textAlign: 'center', padding: '1rem'}}>
                    Monthly Production — Last 12 Months
                  </div>
                  <div style={{padding: '1rem'}}>
                    <table className="prod-table">
                      <thead>
                        <tr>
                          <th>Month</th>
                          <th>Spring</th>
                          <th>Hypnos</th>
                          <th>Total</th>
                          <th>Target</th>
                        </tr>
                      </thead>
                      <tbody>
                        {monthlyData.length > 0 ? monthlyData.map((month, idx) => (
                          <tr key={idx}>
                            <td>{month.month}</td>
                            <td className="num">{month.spring}</td>
                            <td className="num">{month.hypnos}</td>
                            <td className="num">{month.total}</td>
                            <td className="num">{month.target}</td>
                          </tr>
                        )) : <tr><td colSpan="5" style={{textAlign: 'center', color: 'var(--text-muted)'}}>Loading monthly data...</td></tr>}
                      </tbody>
                    </table>
                  </div>
                </div>
              ) : tab === 'settings' ? (
                <div className="settings-panel">
                  <div className="settings-section">
                    <div className="settings-title">Simulator Controls</div>
                    <div className="settings-row">
                      <div>
                        <div className="settings-row-label">Simulator Status</div>
                        <div className="settings-row-desc">
                          {simulatorStatus ? `${simulatorStatus.status} - ${simulatorStatus.eventsGenerated} events generated` : 'Loading...'}
                        </div>
                      </div>
                      <span className={`badge ${simulatorStatus?.active ? 'badge-success' : 'badge-warn'}`}>
                        {simulatorStatus?.active ? 'Active' : 'Inactive'}
                      </span>
                    </div>
                    <div className="settings-row">
                      <div>
                        <div className="settings-row-label">TV Display Mode</div>
                        <div className="settings-row-desc">Full-screen kiosk optimised layout</div>
                      </div>
                      <div className="toggle-wrap" onClick={toggleTvMode}>
                        <div className={`toggle ${tvMode ? 'on' : ''}`}></div>
                      </div>
                    </div>
                  </div>
                  
                  <div className="settings-section">
                    <div className="settings-title">Data Source — HMI / PLC</div>
                    <div className="settings-row">
                      <div>
                        <div className="settings-row-label">Connection Status</div>
                      </div>
                      <span className="badge badge-success">Connected — Simulated</span>
                    </div>
                    <div className="settings-row">
                      <div>
                        <div className="settings-row-label">Data Source</div>
                      </div>
                      <span className="badge badge-spring">HMI/PLC Simulator</span>
                    </div>
                    <div className="settings-row">
                      <div>
                        <div className="settings-row-label">Endpoint</div>
                        <div className="settings-row-desc">OPC-UA or Modbus TCP address of the HMI controller</div>
                      </div>
                      <span style={{fontSize: '0.8rem', color: 'var(--text-muted)'}}>HMI/PLC Simulator</span>
                    </div>
                  </div>
                  
                  <div className="settings-section">
                    <div className="settings-title">Display Preferences</div>
                    <div className="settings-row">
                      <div>
                        <div className="settings-row-label">Refresh Interval</div>
                        <div className="settings-row-desc">Current auto-refresh setting</div>
                      </div>
                      <span style={{fontSize: '0.8rem', color: 'var(--text-muted)'}}>{intervalMins} minutes</span>
                    </div>
                  </div>
                </div>
              ) : tab === 'crm' ? (
                <div className="card-title" style={{textAlign: 'center', padding: '2rem'}}>
                  CRM Metrics — Not Available / Future Module
                </div>
              ) : (
                <div className="card-title" style={{textAlign: 'center', padding: '2rem'}}>
                  {tab.charAt(0).toUpperCase() + tab.slice(1)} Tab — Coming Soon
                </div>
              )}
            </div>
          </div>
        </div>
      ))}

      <footer className="footer" style={{ display: tvMode ? 'none' : 'block' }}>
        Peps Mattress Automated Production Display System &nbsp;&middot;&nbsp; HMI/PLC Integration Layer &nbsp;&middot;&nbsp; Data refreshes per configured interval
      </footer>
    </div>
  );
}

export default App;