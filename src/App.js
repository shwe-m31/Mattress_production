/* eslint-disable react-hooks/exhaustive-deps */
import React, { useEffect, useState, useRef } from "react";
import "./App.css";
import Chart from "chart.js/auto";
import productionApi from "./services/productionApi";

function App() {
  // Navigation & Control state
  const [activeTab, setActiveTab] = useState('dashboard');
  const [clock, setClock] = useState('--:-- -- | --- --, ----');
  const [intervalMins, setIntervalMins] = useState(15);
  const [secondsRemaining, setSecondsRemaining] = useState(15 * 60);
  const [lineSelect, setLineSelect] = useState('all');
  const [shiftSelect, setShiftSelect] = useState('current');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [tvMode, setTvMode] = useState(false);

  // Settings State
  const [settingsData, setSettingsData] = useState({
    preferredMode: 'SIMULATED',
    dashboardUpdateInterval: 15,
    dataSourceUrl: 'opc.tcp://192.168.1.100:4840',
    authenticationMethod: 'Anonymous',
    dataSourcePollingInterval: 5,
    connectionStatus: 'DISCONNECTED',
    productionTargets: {
      SPRING_SINGLE: 10,
      SPRING_DOUBLE: 12,
      SPRING_QUEEN: 15,
      SPRING_KING: 8,
      HYPNOS_SINGLE: 8,
      HYPNOS_DOUBLE: 10,
      HYPNOS_QUEEN: 12,
      HYPNOS_KING: 7
    },
    shiftConfigurations: [
      { shiftName: 'Morning Shift', startTime: '06:00', endTime: '14:00', active: true },
      { shiftName: 'Evening Shift', startTime: '14:00', endTime: '22:00', active: true },
      { shiftName: 'Night Shift', startTime: '22:00', endTime: '06:00', active: true }
    ]
  });
  const [settingsSaveMessage, setSettingsSaveMessage] = useState(null);

  // Dashboard Data State
  const [kpiData, setKpiData] = useState({
    total: 0,
    spring: 0,
    hypnos: 0,
    shiftTarget: 0,
    targetPct: '0%',
    targetSub: '0 / 0 units',
    efficiency: 0
  });
  const [sizeBreakdown, setSizeBreakdown] = useState([]);
  const [incomingFeed, setIncomingFeed] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [downtimeMinutes, setDowntimeMinutes] = useState(0);
  const [currentShiftInfo, setCurrentShiftInfo] = useState({ shiftName: '--', startTime: '--:--', endTime: '--:--', durationHours: 8 });

  // Analytics Data State
  const [dailyData, setDailyData] = useState([]);
  const [weeklyData, setWeeklyData] = useState([]);
  const [monthlyData, setMonthlyData] = useState([]);

  // Chart references
  const chartsRef = useRef({});
  const timerRef = useRef(null);

  const SIZE_COLORS = {
    king: '#2E7AAB',
    queen: '#1B4F6A',
    double: '#3E9AD0',
    single: '#7ABCD5'
  };

  // Update Clock
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

  // Format MM:SS for timer
  const formatCountdown = (secs) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  };

  // Load Settings from Backend
  const loadSettings = async () => {
    try {
      const data = await productionApi.getSettings();
      if (data) {
        setSettingsData(prev => ({
          ...prev,
          ...data,
          productionTargets: data.productionTargets || prev.productionTargets,
          shiftConfigurations: data.shiftConfigurations || prev.shiftConfigurations
        }));
        if (data.dashboardUpdateInterval && data.dashboardUpdateInterval !== intervalMins) {
          setIntervalMins(data.dashboardUpdateInterval);
          setSecondsRemaining(data.dashboardUpdateInterval * 60);
        }
      }
    } catch (err) {
      console.error('Failed to load settings:', err);
    }
  };

  // Fetch Dashboard Data from Backend
  const fetchDashboardData = async () => {
    try {
      setError(null);
      const data = await productionApi.getDashboard();
      
      setKpiData({
        total: (data.totalProduction || 0).toLocaleString(),
        spring: (data.springCount || 0).toLocaleString(),
        hypnos: (data.hypnosCount || 0).toLocaleString(),
        shiftTarget: data.shiftTarget || 0,
        targetPct: (data.targetPercentage || 0) + '%',
        targetSub: `${data.totalProduction || 0} / ${data.shiftTarget || 0} units`,
        efficiency: data.efficiency || 0
      });

      // Size breakdown
      const sizeEntries = Object.entries(data.sizeBreakdown || {}).map(([name, counts]) => {
        const total = (counts.spring || 0) + (counts.hypnos || 0);
        const pct = data.totalProduction > 0 ? Math.round((total / data.totalProduction) * 100) : 0;
        return {
          name: name.charAt(0).toUpperCase() + name.slice(1),
          val: total,
          pct: pct,
          color: SIZE_COLORS[name.toLowerCase()] || '#2E7AAB'
        };
      });
      setSizeBreakdown(sizeEntries);

      // Incoming Production Records (1 row = 1 completed unit)
      setIncomingFeed(data.recentItems || []);

      // Operational Information
      setAlerts(data.alerts || []);
      setDowntimeMinutes(data.downtimeMinutes || 0);
      if (data.currentShift) {
        setCurrentShiftInfo(data.currentShift);
      }

      // Render Hourly Chart
      renderHourlyChart(data.hourlyData);

    } catch (err) {
      console.error('Error fetching dashboard data:', err);
      setError('Unable to load production data from backend. Please verify backend service.');
    }
  };

  // Render Dashboard Hourly Chart (Time-Aware)
  const renderHourlyChart = (hourlyData) => {
    const canvas = document.getElementById('hourlyDashboardChart');
    if (!canvas || !hourlyData) return;

    if (chartsRef.current.hourlyDashboard) {
      chartsRef.current.hourlyDashboard.destroy();
    }

    const currentHour = new Date().getHours();
    const hours = [];
    const springValues = [];
    const hypnosValues = [];
    const targetValues = [];

    // Calculate hourly target from settings
    const hourlyTarget = calculateTotalHourlyTarget();

    // From shift start (6 AM or 0) up to current hour only (time-aware)
    const startHour = 6;
    const hoursCount = Math.max(1, (currentHour >= startHour ? (currentHour - startHour + 1) : (24 - startHour + currentHour + 1)));

    for (let i = 0; i < Math.min(24, Math.max(8, hoursCount)); i++) {
      const h = (startHour + i) % 24;
      hours.push(`${String(h).padStart(2, '0')}:00`);
      
      // Only include values if hour is <= currentHour
      const isPastOrCurrent = (startHour <= currentHour) ? 
        (h >= startHour && h <= currentHour) : 
        (h >= startHour || h <= currentHour);

      if (isPastOrCurrent) {
        springValues.push(hourlyData.spring ? (hourlyData.spring[h] || 0) : 0);
        hypnosValues.push(hourlyData.hypnos ? (hourlyData.hypnos[h] || 0) : 0);
      } else {
        springValues.push(0);
        hypnosValues.push(0);
      }
      targetValues.push(hourlyTarget);
    }

    chartsRef.current.hourlyDashboard = new Chart(canvas, {
      type: 'bar',
      data: {
        labels: hours,
        datasets: [
          {
            label: 'Spring',
            data: springValues,
            backgroundColor: 'rgba(46,122,171,0.75)',
            borderColor: '#2E7AAB',
            borderWidth: 1
          },
          {
            label: 'Hypnos',
            data: hypnosValues,
            backgroundColor: 'rgba(27,79,106,0.75)',
            borderColor: '#1B4F6A',
            borderWidth: 1
          },
          {
            label: 'Hourly Target',
            data: targetValues,
            type: 'line',
            borderColor: '#C9952E',
            borderDash: [4, 4],
            borderWidth: 2,
            pointRadius: 0,
            fill: false
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'top', labels: { boxWidth: 10, padding: 12, font: { family: "'Lora', serif", size: 10 } } },
          tooltip: { mode: 'index' }
        },
        scales: {
          x: { grid: { color: '#EEF5FA' }, stacked: true },
          y: { grid: { color: '#EEF5FA' }, stacked: true, suggestedMin: 0 }
        }
      }
    });
  };

  // Fetch Daily Page Details
  const fetchDailyDetails = async () => {
    try {
      const data = await productionApi.getDailyProduction();
      if (data && data.length > 0) {
        setDailyData(data);
        renderDailyChart(data);
        renderDailyVarietyChart(data[0]);
      }
    } catch (err) {
      console.error('Error fetching daily details:', err);
    }
  };

  const renderDailyChart = (data) => {
    const canvas = document.getElementById('dailyChart');
    if (!canvas) return;

    if (chartsRef.current.dailyChart) {
      chartsRef.current.dailyChart.destroy();
    }

    const reversed = [...data].reverse();
    const labels = reversed.map(d => d.date);
    const spring = reversed.map(d => d.spring || 0);
    const hypnos = reversed.map(d => d.hypnos || 0);

    chartsRef.current.dailyChart = new Chart(canvas, {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [
          { label: 'Spring', data: spring, backgroundColor: 'rgba(46,122,171,0.75)', borderColor: '#2E7AAB', borderWidth: 1 },
          { label: 'Hypnos', data: hypnos, backgroundColor: 'rgba(27,79,106,0.75)', borderColor: '#1B4F6A', borderWidth: 1 }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { position: 'top', labels: { boxWidth: 10, padding: 12, font: { family: "'Lora', serif", size: 10 } } } },
        scales: {
          x: { grid: { color: '#EEF5FA' }, stacked: true },
          y: { grid: { color: '#EEF5FA' }, stacked: true, suggestedMin: 0 }
        }
      }
    });
  };

  const renderDailyVarietyChart = (todayData) => {
    const canvas = document.getElementById('dailyVarietyChart');
    if (!canvas || !todayData) return;

    if (chartsRef.current.dailyVarietyChart) {
      chartsRef.current.dailyVarietyChart.destroy();
    }

    chartsRef.current.dailyVarietyChart = new Chart(canvas, {
      type: 'doughnut',
      data: {
        labels: ['King', 'Queen', 'Double', 'Single'],
        datasets: [{
          data: [todayData.king || 0, todayData.queen || 0, todayData.double || 0, todayData.single || 0],
          backgroundColor: ['#2E7AAB', '#1B4F6A', '#3E9AD0', '#7ABCD5'],
          borderWidth: 2,
          borderColor: '#fff'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { position: 'right', labels: { boxWidth: 10, padding: 10, font: { family: "'Lora', serif", size: 10 } } } },
        cutout: '65%'
      }
    });
  };

  // Fetch Weekly Page Details
  const fetchWeeklyDetails = async () => {
    try {
      const res = await productionApi.getWeeklyProduction();
      if (res) {
        const summary = res.weeklySummary || [];
        const fulfilment = res.productFulfilment || [];
        setWeeklyData(summary);
        renderWeeklyChart(summary);
        renderFulfilmentChart(fulfilment);
      }
    } catch (err) {
      console.error('Error fetching weekly details:', err);
    }
  };

  const renderWeeklyChart = (summary) => {
    const canvas = document.getElementById('weeklyChart');
    if (!canvas) return;

    if (chartsRef.current.weeklyChart) {
      chartsRef.current.weeklyChart.destroy();
    }

    const reversed = [...summary].reverse();
    const weeks = reversed.map(w => w.week);
    const spring = reversed.map(w => w.spring || 0);
    const hypnos = reversed.map(w => w.hypnos || 0);
    const target = reversed.map(w => w.target || 3936);

    chartsRef.current.weeklyChart = new Chart(canvas, {
      type: 'bar',
      data: {
        labels: weeks,
        datasets: [
          { label: 'Spring', data: spring, backgroundColor: 'rgba(46,122,171,0.75)', borderColor: '#2E7AAB', borderWidth: 1 },
          { label: 'Hypnos', data: hypnos, backgroundColor: 'rgba(27,79,106,0.75)', borderColor: '#1B4F6A', borderWidth: 1 },
          { label: 'Target', data: target, type: 'line', borderColor: '#3E9AD0', borderWidth: 2, pointRadius: 0, fill: false }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { position: 'top', labels: { boxWidth: 10, padding: 12, font: { family: "'Lora', serif", size: 10 } } } },
        scales: {
          x: { grid: { color: '#EEF5FA' }, stacked: true },
          y: { grid: { color: '#EEF5FA' }, stacked: true, suggestedMin: 0 }
        }
      }
    });
  };

  // Replaced Day-of-Week Pattern with "Order Fulfilment by Product"
  const renderFulfilmentChart = (fulfilment) => {
    const canvas = document.getElementById('fulfilmentChart');
    if (!canvas) return;

    if (chartsRef.current.fulfilmentChart) {
      chartsRef.current.fulfilmentChart.destroy();
    }

    const labels = fulfilment.map(f => f.product);
    const planned = fulfilment.map(f => f.planned);
    const fulfilled = fulfilment.map(f => f.fulfilled);

    chartsRef.current.fulfilmentChart = new Chart(canvas, {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [
          { label: 'Target / Planned', data: planned, backgroundColor: 'rgba(122,157,181,0.5)', borderColor: '#7A9DB5', borderWidth: 1 },
          { label: 'Produced / Fulfilled', data: fulfilled, backgroundColor: 'rgba(42,125,91,0.8)', borderColor: '#2A7D5B', borderWidth: 1 }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'top', labels: { boxWidth: 10, padding: 12, font: { family: "'Lora', serif", size: 10 } } },
          tooltip: { mode: 'index' }
        },
        scales: {
          x: { grid: { color: '#EEF5FA' }, ticks: { font: { size: 9 } } },
          y: { grid: { color: '#EEF5FA' }, suggestedMin: 0 }
        }
      }
    });
  };

  // Fetch Monthly Page Details
  const fetchMonthlyDetails = async () => {
    try {
      const data = await productionApi.getMonthlyProduction();
      if (data && data.length > 0) {
        setMonthlyData(data);
        renderMonthlyChart(data);
        renderMonthlySizeChart(data);
      }
    } catch (err) {
      console.error('Error fetching monthly details:', err);
    }
  };

  const renderMonthlyChart = (data) => {
    const canvas = document.getElementById('monthlyChart');
    if (!canvas) return;

    if (chartsRef.current.monthlyChart) {
      chartsRef.current.monthlyChart.destroy();
    }

    const reversed = [...data].reverse();
    const months = reversed.map(m => m.month);
    const spring = reversed.map(m => m.spring || 0);
    const hypnos = reversed.map(m => m.hypnos || 0);
    const target = reversed.map(m => m.target || 16400);

    chartsRef.current.monthlyChart = new Chart(canvas, {
      type: 'bar',
      data: {
        labels: months,
        datasets: [
          { label: 'Spring', data: spring, backgroundColor: 'rgba(46,122,171,0.75)', borderColor: '#2E7AAB', borderWidth: 1 },
          { label: 'Hypnos', data: hypnos, backgroundColor: 'rgba(27,79,106,0.75)', borderColor: '#1B4F6A', borderWidth: 1 },
          { label: 'Target', data: target, type: 'line', borderColor: '#3E9AD0', borderWidth: 2, pointRadius: 0, fill: false }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { position: 'top', labels: { boxWidth: 10, padding: 12, font: { family: "'Lora', serif", size: 10 } } } },
        scales: {
          x: { grid: { color: '#EEF5FA' }, stacked: true },
          y: { grid: { color: '#EEF5FA' }, stacked: true, suggestedMin: 0 }
        }
      }
    });
  };

  const renderMonthlySizeChart = (data) => {
    const canvas = document.getElementById('monthlySizeChart');
    if (!canvas || !data) return;

    if (chartsRef.current.monthlySizeChart) {
      chartsRef.current.monthlySizeChart.destroy();
    }

    const totalKing = data.reduce((s, m) => s + (m.king || 0), 0);
    const totalQueen = data.reduce((s, m) => s + (m.queen || 0), 0);
    const totalDouble = data.reduce((s, m) => s + (m.double || 0), 0);
    const totalSingle = data.reduce((s, m) => s + (m.single || 0), 0);

    chartsRef.current.monthlySizeChart = new Chart(canvas, {
      type: 'doughnut',
      data: {
        labels: ['King', 'Queen', 'Double', 'Single'],
        datasets: [{
          data: [totalKing, totalQueen, totalDouble, totalSingle],
          backgroundColor: ['#2E7AAB', '#1B4F6A', '#3E9AD0', '#7ABCD5'],
          borderWidth: 2,
          borderColor: '#fff'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { position: 'right', labels: { boxWidth: 10, padding: 10, font: { family: "'Lora', serif", size: 10 } } } },
        cutout: '65%'
      }
    });
  };

  // Helper calculations for 8 targets
  const calculateSpringHourlyTarget = () => {
    const t = settingsData.productionTargets || {};
    return (t.SPRING_SINGLE || 0) + (t.SPRING_DOUBLE || 0) + (t.SPRING_QUEEN || 0) + (t.SPRING_KING || 0);
  };

  const calculateHypnosHourlyTarget = () => {
    const t = settingsData.productionTargets || {};
    return (t.HYPNOS_SINGLE || 0) + (t.HYPNOS_DOUBLE || 0) + (t.HYPNOS_QUEEN || 0) + (t.HYPNOS_KING || 0);
  };

  const calculateTotalHourlyTarget = () => {
    return calculateSpringHourlyTarget() + calculateHypnosHourlyTarget();
  };

  // Handle Tab Change
  const handleTabChange = (tab) => {
    setActiveTab(tab);
    if (tab === 'dashboard') {
      fetchDashboardData();
    } else if (tab === 'daily') {
      fetchDailyDetails();
    } else if (tab === 'weekly') {
      fetchWeeklyDetails();
    } else if (tab === 'monthly') {
      fetchMonthlyDetails();
    } else if (tab === 'settings') {
      loadSettings();
    }
  };

  // Handle Interval Dropdown Change
  const handleIntervalChange = (e) => {
    const mins = parseInt(e.target.value, 10);
    setIntervalMins(mins);
    setSecondsRemaining(mins * 60);
  };

  // Handle Target Input Change
  const handleTargetChange = (key, value) => {
    const num = Math.max(0, parseInt(value, 10) || 0);
    setSettingsData(prev => ({
      ...prev,
      productionTargets: {
        ...prev.productionTargets,
        [key]: num
      }
    }));
  };

  // Handle Shift Time Edit
  const handleShiftChange = (index, field, value) => {
    setSettingsData(prev => {
      const updated = [...prev.shiftConfigurations];
      updated[index] = { ...updated[index], [field]: value };
      return { ...prev, shiftConfigurations: updated };
    });
  };

  // Save Settings to Backend
  const handleSaveSettings = async (e) => {
    if (e) e.preventDefault();
    setIsLoading(true);
    setSettingsSaveMessage(null);
    try {
      const saved = await productionApi.updateSettings({
        ...settingsData,
        dashboardUpdateInterval: intervalMins
      });
      setSettingsData(saved);
      setSettingsSaveMessage({ type: 'success', text: 'Settings saved successfully to database.' });
      
      // Update local timer and refetch dashboard
      setSecondsRemaining(saved.dashboardUpdateInterval * 60);
      fetchDashboardData();
    } catch (err) {
      console.error('Error saving settings:', err);
      setSettingsSaveMessage({ type: 'error', text: 'Failed to save settings to backend.' });
    } finally {
      setIsLoading(false);
    }
  };

  // Countdown timer effect
  useEffect(() => {
    if (timerRef.current) clearInterval(timerRef.current);

    timerRef.current = setInterval(() => {
      setSecondsRemaining(prev => {
        if (prev <= 1) {
          // Trigger auto-update
          fetchDashboardData();
          if (activeTab === 'daily') fetchDailyDetails();
          if (activeTab === 'weekly') fetchWeeklyDetails();
          if (activeTab === 'monthly') fetchMonthlyDetails();
          return intervalMins * 60;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timerRef.current);
  }, [intervalMins, activeTab]);

  // Initial mount effect
  useEffect(() => {
    loadSettings();
    fetchDashboardData();
    updateClock();

    const clockTimer = setInterval(updateClock, 1000);
    return () => clearInterval(clockTimer);
  }, []);

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

      {/* NAVIGATION: Strictly Dashboard | Daily | Weekly | Monthly | Settings */}
      <nav className="nav" style={{ display: tvMode ? 'none' : 'flex' }}>
        <button className={`nav-tab ${activeTab === 'dashboard' ? 'active' : ''}`} onClick={() => handleTabChange('dashboard')}>Dashboard</button>
        <button className={`nav-tab ${activeTab === 'daily' ? 'active' : ''}`} onClick={() => handleTabChange('daily')}>Daily</button>
        <button className={`nav-tab ${activeTab === 'weekly' ? 'active' : ''}`} onClick={() => handleTabChange('weekly')}>Weekly</button>
        <button className={`nav-tab ${activeTab === 'monthly' ? 'active' : ''}`} onClick={() => handleTabChange('monthly')}>Monthly</button>
        <button className={`nav-tab ${activeTab === 'settings' ? 'active' : ''}`} onClick={() => handleTabChange('settings')}>Settings</button>
      </nav>

      {/* CONTROLS BAR: No Simulate HMI Push button */}
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
            <option value="current">Current Shift ({currentShiftInfo.shiftName})</option>
            {(settingsData.shiftConfigurations || []).map((s, idx) => (
              <option key={idx} value={s.shiftName}>{s.shiftName} ({s.startTime}–{s.endTime})</option>
            ))}
          </select>
        </div>

        <button className="control-btn secondary" onClick={() => { fetchDashboardData(); setSecondsRemaining(intervalMins * 60); }} disabled={isLoading}>
          {isLoading ? 'Refreshing...' : 'Refresh Now'}
        </button>

        <div className="next-update">Next auto-update in <span>{formatCountdown(secondsRemaining)}</span></div>
      </div>

      {/* ERROR DISPLAY */}
      {error && (
        <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--danger)' }}>
          <div style={{ fontSize: '1.2rem', marginBottom: '0.5rem' }}>⚠️ {error}</div>
          <button className="control-btn" onClick={fetchDashboardData} style={{ marginTop: '0.5rem' }}>Retry Connection</button>
        </div>
      )}

      {/* 1. DASHBOARD TAB */}
      {!error && (
        <div className={`main ${activeTab === 'dashboard' ? 'active' : ''}`}>
          {/* KPI STRIP: 5 Cards (No Average Cycle Time, No Line Efficiency OEE card) */}
          <div className="kpi-strip">
            <div className="kpi-card spring-accent">
              <div className="kpi-label">Total Produced Today</div>
              <div className="kpi-value">{kpiData.total}</div>
              <div className="kpi-sub">units across all lines</div>
              <div className="kpi-delta up">● Live from MySQL</div>
            </div>

            <div className="kpi-card spring-accent">
              <div className="kpi-label">Spring Mattresses</div>
              <div className="kpi-value">{kpiData.spring}</div>
              <div className="kpi-sub">units completed today</div>
              <div className="kpi-delta up">Spring Production</div>
            </div>

            <div className="kpi-card hypnos-accent">
              <div className="kpi-label">Hypnos Mattresses</div>
              <div className="kpi-value">{kpiData.hypnos}</div>
              <div className="kpi-sub">units completed today</div>
              <div className="kpi-delta up">Hypnos Production</div>
            </div>

            <div className="kpi-card success-accent">
              <div className="kpi-label">Shift Target</div>
              <div className="kpi-value">{kpiData.targetPct}</div>
              <div className="kpi-sub">{kpiData.targetSub}</div>
              <div className="kpi-delta neutral">{currentShiftInfo.shiftName}</div>
            </div>

            <div className="kpi-card warn-accent">
              <div className="kpi-label">Production Efficiency</div>
              <div className="kpi-value">{kpiData.efficiency}%</div>
              <div className="kpi-sub">Actual vs Scheduled Pace</div>
              <div className="kpi-delta up">Deterministic Calculation</div>
            </div>
          </div>

          {/* ROW 1: Time-Aware Hourly Graph + Size Distribution */}
          <div className="grid-2-1">
            <div className="card">
              <div className="card-header">
                <div>
                  <div className="card-title">Hourly Production — Today</div>
                  <div className="card-subtitle">Spring vs Hypnos, units per elapsed hour</div>
                </div>
                <span className="badge badge-success">Time-Aware</span>
              </div>
              <div className="card-body">
                <div className="chart-wrap" style={{ height: '220px' }}>
                  <canvas id="hourlyDashboardChart"></canvas>
                </div>
              </div>
            </div>

            <div className="card">
              <div className="card-header">
                <div>
                  <div className="card-title">Size Distribution</div>
                  <div className="card-subtitle">Today's completed production</div>
                </div>
              </div>
              <div className="card-body">
                {sizeBreakdown.length > 0 ? sizeBreakdown.map((item, idx) => (
                  <div className="size-row" key={idx}>
                    <div className="size-name">{item.name}</div>
                    <div className="size-bar-wrap">
                      <div className="size-bar" style={{ width: `${item.pct}%`, background: item.color }}></div>
                    </div>
                    <div className="size-count">{item.val}</div>
                    <div className="size-pct">{item.pct}%</div>
                  </div>
                )) : <div style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '2rem' }}>No size data available</div>}
              </div>
            </div>
          </div>

          {/* ROW 2: Incoming Production Records (Live Feed) + Operational Status */}
          <div className="grid-2-1">
            <div className="card">
              <div className="card-header">
                <div>
                  <div className="card-title">Incoming Production Records</div>
                  <div className="card-subtitle">Live arrival feed (1 row = 1 completed mattress)</div>
                </div>
                <span className="badge badge-spring">{settingsData.preferredMode}</span>
              </div>
              <div className="card-body" style={{ padding: '0.75rem 1.25rem' }}>
                <div className="hmi-feed">
                  {incomingFeed.length > 0 ? incomingFeed.map((row, idx) => (
                    <div className="hmi-row" key={idx}>
                      <div className="hmi-time">
                        {new Date(row.completionTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                      </div>
                      <div className="hmi-type">
                        <span className={`badge ${row.productType === 'SPRING' ? 'badge-spring' : 'badge-hypnos'}`} style={{ fontSize: '0.62rem' }}>
                          {row.productType}
                        </span>
                      </div>
                      <div className="hmi-size"><strong>{row.size}</strong> &middot; {row.variety}</div>
                      <div className="hmi-line">{row.productionLine}</div>
                      <div className="hmi-status">
                        <span className="status-pill status-complete">{row.status}</span>
                      </div>
                    </div>
                  )) : <div style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '2rem' }}>Awaiting incoming production events...</div>}
                </div>
              </div>
            </div>

            <div className="card">
              <div className="card-header">
                <div>
                  <div className="card-title">Operational Status</div>
                  <div className="card-subtitle">Downtime & Low Production Alerts</div>
                </div>
              </div>
              <div className="card-body">
                <div style={{ marginBottom: '1rem', padding: '0.75rem', background: 'var(--container-alt)', borderRadius: 'var(--radius)', border: '1px solid var(--border)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ fontSize: '0.85rem', fontWeight: 600 }}>Downtime Today:</span>
                    <span style={{ fontFamily: 'var(--font-display)', fontWeight: 700, fontSize: '1.1rem', color: downtimeMinutes > 0 ? 'var(--warn)' : 'var(--success)' }}>
                      {downtimeMinutes} min
                    </span>
                  </div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
                    Calculated strictly from stoppage intervals
                  </div>
                </div>

                <div>
                  <div style={{ fontSize: '0.8rem', fontWeight: 600, marginBottom: '0.5rem', color: 'var(--text-secondary)' }}>
                    Active Alerts ({alerts.length})
                  </div>
                  {alerts.length > 0 ? alerts.map((a, idx) => (
                    <div key={idx} style={{ padding: '0.5rem 0.75rem', background: '#FDE8E8', border: '1px solid #F9C5C5', borderRadius: '4px', marginBottom: '0.4rem', fontSize: '0.75rem' }}>
                      <strong style={{ color: 'var(--danger)' }}>[{a.severity}] {a.alertType}:</strong> {a.message}
                    </div>
                  )) : (
                    <div style={{ padding: '0.5rem', fontSize: '0.75rem', color: 'var(--success)' }}>
                      ✓ All lines operating normally. No active low production alerts.
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 2. DAILY TAB */}
      {!error && (
        <div className={`main ${activeTab === 'daily' ? 'active' : ''}`}>
          <div className="kpi-strip six-cards">
            <div className="kpi-card spring-accent">
              <div className="kpi-label">Today Total</div>
              <div className="kpi-value">{dailyData[0] ? dailyData[0].total : '--'}</div>
              <div className="kpi-sub">units</div>
            </div>
            <div className="kpi-card">
              <div className="kpi-label">Yesterday</div>
              <div className="kpi-value">{dailyData[1] ? dailyData[1].total : '--'}</div>
              <div className="kpi-sub">units</div>
            </div>
            <div className="kpi-card success-accent">
              <div className="kpi-label">Daily Target</div>
              <div className="kpi-value">{calculateTotalHourlyTarget() * 8}</div>
              <div className="kpi-sub">units (8h shift)</div>
            </div>
            <div className="kpi-card">
              <div className="kpi-label">Best Day (14d)</div>
              <div className="kpi-value">{dailyData.length > 0 ? Math.max(...dailyData.map(d => d.total || 0)) : '--'}</div>
              <div className="kpi-sub">units</div>
            </div>
            <div className="kpi-card">
              <div className="kpi-label">Avg Daily (14d)</div>
              <div className="kpi-value">{dailyData.length > 0 ? Math.round(dailyData.reduce((s, d) => s + (d.total || 0), 0) / dailyData.length) : '--'}</div>
              <div className="kpi-sub">units</div>
            </div>
            <div className="kpi-card warn-accent">
              <div className="kpi-label">Downtime Today</div>
              <div className="kpi-value">{dailyData[0] ? dailyData[0].downtime : '0'}</div>
              <div className="kpi-sub">minutes</div>
            </div>
          </div>

          <div className="grid-2-1">
            <div className="card">
              <div className="card-header">
                <div>
                  <div className="card-title">Daily Production — Last 14 Days</div>
                  <div className="card-subtitle">Spring vs Hypnos completed volume</div>
                </div>
              </div>
              <div className="card-body">
                <div className="chart-wrap" style={{ height: '240px' }}>
                  <canvas id="dailyChart"></canvas>
                </div>
              </div>
            </div>

            <div className="card">
              <div className="card-header">
                <div>
                  <div className="card-title">Today by Variety Mix</div>
                </div>
              </div>
              <div className="card-body">
                <div className="chart-wrap" style={{ height: '240px' }}>
                  <canvas id="dailyVarietyChart"></canvas>
                </div>
              </div>
            </div>
          </div>

          <div className="card" style={{ marginTop: '1rem' }}>
            <div className="card-header">
              <div className="card-title">Daily Production Table — Last 14 Days</div>
            </div>
            <div className="card-body" style={{ padding: 0 }}>
              <table className="prod-table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Spring</th>
                    <th>Hypnos</th>
                    <th>Total</th>
                    <th>King</th>
                    <th>Queen</th>
                    <th>Double</th>
                    <th>Single</th>
                    <th>Efficiency</th>
                    <th>Downtime</th>
                  </tr>
                </thead>
                <tbody>
                  {dailyData.map((d, idx) => (
                    <tr key={idx}>
                      <td><strong>{d.date}</strong></td>
                      <td className="num">{d.spring}</td>
                      <td className="num">{d.hypnos}</td>
                      <td className="num">{d.total}</td>
                      <td className="num">{d.king}</td>
                      <td className="num">{d.queen}</td>
                      <td className="num">{d.double}</td>
                      <td className="num">{d.single}</td>
                      <td className="num">{d.efficiency}%</td>
                      <td className="num">{d.downtime} min</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* 3. WEEKLY TAB: Replaced Day-of-Week with Order Fulfilment by Product */}
      {!error && (
        <div className={`main ${activeTab === 'weekly' ? 'active' : ''}`}>
          <div className="kpi-strip six-cards">
            <div className="kpi-card spring-accent">
              <div className="kpi-label">This Week Total</div>
              <div className="kpi-value">{weeklyData[0] ? weeklyData[0].total : '--'}</div>
              <div className="kpi-sub">units</div>
            </div>
            <div className="kpi-card">
              <div className="kpi-label">Last Week</div>
              <div className="kpi-value">{weeklyData[1] ? weeklyData[1].total : '--'}</div>
              <div className="kpi-sub">units</div>
            </div>
            <div className="kpi-card success-accent">
              <div className="kpi-label">Weekly Target</div>
              <div className="kpi-value">{calculateTotalHourlyTarget() * 48}</div>
              <div className="kpi-sub">units (6 days)</div>
            </div>
            <div className="kpi-card spring-accent">
              <div className="kpi-label">Spring (This Week)</div>
              <div className="kpi-value">{weeklyData[0] ? weeklyData[0].spring : '--'}</div>
              <div className="kpi-sub">units</div>
            </div>
            <div className="kpi-card hypnos-accent">
              <div className="kpi-label">Hypnos (This Week)</div>
              <div className="kpi-value">{weeklyData[0] ? weeklyData[0].hypnos : '--'}</div>
              <div className="kpi-sub">units</div>
            </div>
            <div className="kpi-card warn-accent">
              <div className="kpi-label">Week Attainment</div>
              <div className="kpi-value">{weeklyData[0] ? weeklyData[0].efficiency : 0}%</div>
              <div className="kpi-sub">Target Attainment</div>
            </div>
          </div>

          <div className="grid-1-1">
            <div className="card">
              <div className="card-header">
                <div>
                  <div className="card-title">Weekly Output — Last 8 Weeks</div>
                  <div className="card-subtitle">Spring vs Hypnos production volume</div>
                </div>
              </div>
              <div className="card-body">
                <div className="chart-wrap" style={{ height: '250px' }}>
                  <canvas id="weeklyChart"></canvas>
                </div>
              </div>
            </div>

            {/* ORDER FULFILMENT BY PRODUCT (All 8 Product / Size combinations) */}
            <div className="card">
              <div className="card-header">
                <div>
                  <div className="card-title">Order Fulfilment by Product</div>
                  <div className="card-subtitle">Target Planned vs Fulfilled (8 Mattress Lines)</div>
                </div>
                <span className="badge badge-success">Production Domain</span>
              </div>
              <div className="card-body">
                <div className="chart-wrap" style={{ height: '250px' }}>
                  <canvas id="fulfilmentChart"></canvas>
                </div>
              </div>
            </div>
          </div>

          <div className="card" style={{ marginTop: '1rem' }}>
            <div className="card-header">
              <div className="card-title">Weekly Summary Table</div>
            </div>
            <div className="card-body" style={{ padding: 0 }}>
              <table className="prod-table">
                <thead>
                  <tr>
                    <th>Week</th>
                    <th>Spring</th>
                    <th>Hypnos</th>
                    <th>Total</th>
                    <th>Target</th>
                    <th>Attainment</th>
                  </tr>
                </thead>
                <tbody>
                  {weeklyData.map((w, idx) => (
                    <tr key={idx}>
                      <td><strong>{w.week}</strong></td>
                      <td className="num">{w.spring}</td>
                      <td className="num">{w.hypnos}</td>
                      <td className="num">{w.total}</td>
                      <td className="num">{w.target}</td>
                      <td className="num">{w.efficiency}%</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* 4. MONTHLY TAB */}
      {!error && (
        <div className={`main ${activeTab === 'monthly' ? 'active' : ''}`}>
          <div className="kpi-strip six-cards">
            <div className="kpi-card spring-accent">
              <div className="kpi-label">This Month Total</div>
              <div className="kpi-value">{monthlyData[0] ? monthlyData[0].total : '--'}</div>
              <div className="kpi-sub">units</div>
            </div>
            <div className="kpi-card">
              <div className="kpi-label">Last Month</div>
              <div className="kpi-value">{monthlyData[1] ? monthlyData[1].total : '--'}</div>
              <div className="kpi-sub">units</div>
            </div>
            <div className="kpi-card success-accent">
              <div className="kpi-label">Monthly Target</div>
              <div className="kpi-value">{calculateTotalHourlyTarget() * 200}</div>
              <div className="kpi-sub">units (25 days)</div>
            </div>
            <div className="kpi-card spring-accent">
              <div className="kpi-label">Spring YTD</div>
              <div className="kpi-value">{monthlyData.reduce((s, m) => s + (m.spring || 0), 0)}</div>
              <div className="kpi-sub">units</div>
            </div>
            <div className="kpi-card hypnos-accent">
              <div className="kpi-label">Hypnos YTD</div>
              <div className="kpi-value">{monthlyData.reduce((s, m) => s + (m.hypnos || 0), 0)}</div>
              <div className="kpi-sub">units</div>
            </div>
            <div className="kpi-card">
              <div className="kpi-label">YTD Total</div>
              <div className="kpi-value">{monthlyData.reduce((s, m) => s + (m.total || 0), 0)}</div>
              <div className="kpi-sub">units completed</div>
            </div>
          </div>

          <div className="grid-2-1">
            <div className="card">
              <div className="card-header">
                <div>
                  <div className="card-title">Monthly Production — 12-Month View</div>
                </div>
              </div>
              <div className="card-body">
                <div className="chart-wrap" style={{ height: '240px' }}>
                  <canvas id="monthlyChart"></canvas>
                </div>
              </div>
            </div>

            <div className="card">
              <div className="card-header">
                <div>
                  <div className="card-title">YTD Size Mix</div>
                </div>
              </div>
              <div className="card-body">
                <div className="chart-wrap" style={{ height: '240px' }}>
                  <canvas id="monthlySizeChart"></canvas>
                </div>
              </div>
            </div>
          </div>

          <div className="card" style={{ marginTop: '1rem' }}>
            <div className="card-header">
              <div className="card-title">Monthly Summary Table</div>
            </div>
            <div className="card-body" style={{ padding: 0 }}>
              <table className="prod-table">
                <thead>
                  <tr>
                    <th>Month</th>
                    <th>Spring</th>
                    <th>Hypnos</th>
                    <th>Total</th>
                    <th>Target</th>
                    <th>King</th>
                    <th>Queen</th>
                    <th>Double</th>
                    <th>Single</th>
                  </tr>
                </thead>
                <tbody>
                  {monthlyData.map((m, idx) => (
                    <tr key={idx}>
                      <td><strong>{m.month}</strong></td>
                      <td className="num">{m.spring}</td>
                      <td className="num">{m.hypnos}</td>
                      <td className="num">{m.total}</td>
                      <td className="num">{m.target}</td>
                      <td className="num">{m.king}</td>
                      <td className="num">{m.queen}</td>
                      <td className="num">{m.double}</td>
                      <td className="num">{m.single}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* 5. SETTINGS TAB */}
      <div className={`main ${activeTab === 'settings' ? 'active' : ''}`}>
        <div className="settings-panel">
          {settingsSaveMessage && (
            <div className={`alert-box ${settingsSaveMessage.type === 'success' ? 'alert-success' : 'alert-error'}`}>
              {settingsSaveMessage.type === 'success' ? '✓ ' : '⚠️ '}{settingsSaveMessage.text}
            </div>
          )}

          {/* 5.1 PREFERRED MODE */}
          <div className="settings-section">
            <div className="settings-title">Preferred Mode</div>
            <div className="mode-selection-group">
              <label className="mode-radio-label">
                <input
                  type="radio"
                  name="preferredMode"
                  value="SIMULATED"
                  checked={settingsData.preferredMode === 'SIMULATED'}
                  onChange={() => setSettingsData(p => ({ ...p, preferredMode: 'SIMULATED' }))}
                />
                Simulated Mode
              </label>

              <label className="mode-radio-label">
                <input
                  type="radio"
                  name="preferredMode"
                  value="DATA_SOURCE"
                  checked={settingsData.preferredMode === 'DATA_SOURCE'}
                  onChange={() => setSettingsData(p => ({ ...p, preferredMode: 'DATA_SOURCE' }))}
                />
                Data Source Mode
              </label>
            </div>

            {/* DATA SOURCE FIELDS: Only shown in DATA_SOURCE mode */}
            {settingsData.preferredMode === 'DATA_SOURCE' ? (
              <div style={{ marginTop: '1rem', paddingTop: '1rem', borderTop: '1px solid var(--border)' }}>
                <div className="settings-row">
                  <div>
                    <div className="settings-row-label">Data Source / PLC URL</div>
                    <div className="settings-row-desc">OPC-UA or Modbus TCP address of controller</div>
                  </div>
                  <input
                    type="text"
                    className="control-input"
                    style={{ width: '260px' }}
                    value={settingsData.dataSourceUrl || ''}
                    onChange={(e) => setSettingsData(p => ({ ...p, dataSourceUrl: e.target.value }))}
                  />
                </div>

                <div className="settings-row">
                  <div>
                    <div className="settings-row-label">Authentication Method</div>
                  </div>
                  <input
                    type="text"
                    className="control-input"
                    style={{ width: '200px' }}
                    value={settingsData.authenticationMethod || ''}
                    onChange={(e) => setSettingsData(p => ({ ...p, authenticationMethod: e.target.value }))}
                  />
                </div>

                <div className="settings-row">
                  <div>
                    <div className="settings-row-label">Data Source Polling Interval (seconds)</div>
                    <div className="settings-row-desc">Hardware acquisition frequency</div>
                  </div>
                  <input
                    type="number"
                    className="control-input"
                    style={{ width: '100px' }}
                    value={settingsData.dataSourcePollingInterval || 5}
                    onChange={(e) => setSettingsData(p => ({ ...p, dataSourcePollingInterval: parseInt(e.target.value, 10) }))}
                  />
                </div>

                <div className="settings-row">
                  <div>
                    <div className="settings-row-label">Connection Status</div>
                  </div>
                  <span className="badge badge-success">Ready (Interface Bound)</span>
                </div>
              </div>
            ) : (
              <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '0.5rem' }}>
                Simulated mode active: Internal simulation generates gradual production events. External PLC connection fields are disabled.
              </div>
            )}
          </div>

          {/* 5.2 PRODUCTION TARGETS: 8 PRODUCT TARGETS */}
          <div className="settings-section">
            <div className="settings-title">Production Targets (Hourly Target per Product & Size)</div>
            <div className="targets-grid">
              {/* SPRING TARGETS */}
              <div>
                <div className="target-col-title">Spring Mattress Line</div>
                {['SINGLE', 'DOUBLE', 'QUEEN', 'KING'].map(sz => (
                  <div className="target-input-row" key={`SPRING_${sz}`}>
                    <span className="target-input-label">Spring {sz.charAt(0) + sz.slice(1).toLowerCase()}</span>
                    <input
                      type="number"
                      min="0"
                      className="target-input-field"
                      value={settingsData.productionTargets?.[`SPRING_${sz}`] ?? 0}
                      onChange={(e) => handleTargetChange(`SPRING_${sz}`, e.target.value)}
                    />
                  </div>
                ))}
              </div>

              {/* HYPNOS TARGETS */}
              <div>
                <div className="target-col-title">Hypnos Mattress Line</div>
                {['SINGLE', 'DOUBLE', 'QUEEN', 'KING'].map(sz => (
                  <div className="target-input-row" key={`HYPNOS_${sz}`}>
                    <span className="target-input-label">Hypnos {sz.charAt(0) + sz.slice(1).toLowerCase()}</span>
                    <input
                      type="number"
                      min="0"
                      className="target-input-field"
                      value={settingsData.productionTargets?.[`HYPNOS_${sz}`] ?? 0}
                      onChange={(e) => handleTargetChange(`HYPNOS_${sz}`, e.target.value)}
                    />
                  </div>
                ))}
              </div>
            </div>

            <div className="target-summary-box">
              <span>Spring: <strong>{calculateSpringHourlyTarget()}/hr</strong></span>
              <span>Hypnos: <strong>{calculateHypnosHourlyTarget()}/hr</strong></span>
              <span style={{ color: 'var(--accent-hypnos)' }}>Total Hourly Target: <strong>{calculateTotalHourlyTarget()} units/hr</strong></span>
            </div>
          </div>

          {/* 5.3 SHIFT CONFIGURATION */}
          <div className="settings-section">
            <div className="settings-title">Shift Configuration</div>
            <table className="shift-config-table">
              <thead>
                <tr>
                  <th>Shift Name</th>
                  <th>Start Time</th>
                  <th>End Time</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {(settingsData.shiftConfigurations || []).map((shift, idx) => (
                  <tr key={idx}>
                    <td>
                      <input
                        type="text"
                        className="control-input"
                        value={shift.shiftName}
                        onChange={(e) => handleShiftChange(idx, 'shiftName', e.target.value)}
                      />
                    </td>
                    <td>
                      <input
                        type="time"
                        className="control-input"
                        value={shift.startTime}
                        onChange={(e) => handleShiftChange(idx, 'startTime', e.target.value)}
                      />
                    </td>
                    <td>
                      <input
                        type="time"
                        className="control-input"
                        value={shift.endTime}
                        onChange={(e) => handleShiftChange(idx, 'endTime', e.target.value)}
                      />
                    </td>
                    <td>
                      <span className="badge badge-success">Active</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* 5.4 DASHBOARD PREFERENCES */}
          <div className="settings-section">
            <div className="settings-title">Dashboard Preferences</div>
            <div className="settings-row">
              <div>
                <div className="settings-row-label">Dashboard Auto-Update Interval</div>
                <div className="settings-row-desc">Frequency of React frontend data refresh</div>
              </div>
              <select
                className="control-select"
                style={{ width: '150px' }}
                value={intervalMins}
                onChange={handleIntervalChange}
              >
                <option value={15}>Every 15 min</option>
                <option value={30}>Every 30 min</option>
                <option value={60}>Every 60 min</option>
                <option value={5}>Every 5 min (demo)</option>
              </select>
            </div>

            <div className="settings-row">
              <div>
                <div className="settings-row-label">TV Display Mode</div>
                <div className="settings-row-desc">Full-screen kiosk mode for floor displays</div>
              </div>
              <div className="toggle-wrap" onClick={() => setTvMode(!tvMode)}>
                <div className={`toggle ${tvMode ? 'on' : ''}`}></div>
              </div>
            </div>
          </div>

          {/* SAVE BUTTON */}
          <div className="save-settings-bar">
            <button className="control-btn" onClick={handleSaveSettings} disabled={isLoading} style={{ padding: '0.5rem 2rem', fontSize: '0.9rem' }}>
              {isLoading ? 'Saving...' : 'Save Settings'}
            </button>
          </div>
        </div>
      </div>

      {/* FOOTER */}
      <footer className="footer" style={{ display: tvMode ? 'none' : 'block' }}>
        Peps Mattress Automated Production Display System &nbsp;&middot;&nbsp; Production Data Acquisition Layer &nbsp;&middot;&nbsp; Spring Boot & MySQL Backend
      </footer>
    </div>
  );
}

export default App;