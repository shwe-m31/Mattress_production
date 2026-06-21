import React, { useEffect, useState, useRef } from "react";
import "./App.css";
import Chart from "chart.js/auto";

function App() {
  const [data, setData] = useState({
    totalProduction: 0,
    springCount: 0,
    hypnosCount: 0,
    efficiency: 0,
    sizeBreakdown: { 
      single: { spring: 0, hypnos: 0 }, 
      double: { spring: 0, hypnos: 0 }, 
      queen: { spring: 0, hypnos: 0 }, 
      king: { spring: 0, hypnos: 0 } 
    },
    recentItems: [],
    hourlyData: { spring: [], hypnos: [] },
  });

  const [refreshInterval, setRefreshInterval] = useState(60000);
  const [lastUpdate, setLastUpdate] = useState("--:--:--");
  const [isLoading, setIsLoading] = useState(false);

  const productionChartRef = useRef(null);
  const chartInstance = useRef(null);

  const updateChart = (hourlyData) => {
    if (chartInstance.current && hourlyData) {
      chartInstance.current.data.datasets[0].data = hourlyData.spring || Array(12).fill(0);
      chartInstance.current.data.datasets[1].data = hourlyData.hypnos || Array(12).fill(0);
      chartInstance.current.update();
    }
  };

  const fetchProductionData = async () => {
    if (isLoading) return;
    setIsLoading(true);

    try {
      const res = await fetch("http://localhost:8080/api/dashboard");
      if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
      const json = await res.json();

      const transformed = {
        totalProduction: json.totalProduction || 0,
        springCount: json.springCount || 0,
        hypnosCount: json.hypnosCount || 0,
        efficiency: json.efficiency || 90,
        sizeBreakdown: json.sizeBreakdown || {
          single: { spring: 0, hypnos: 0 },
          double: { spring: 0, hypnos: 0 },
          queen: { spring: 0, hypnos: 0 },
          king: { spring: 0, hypnos: 0 }
        },
        recentItems: (json.recentItems || []).map(item => ({
          type: item.type || "SPRING",
          size: item.size || "SINGLE",
          time: new Date(item.time).toLocaleTimeString([], {
            hour: "2-digit",
            minute: "2-digit",
          }),
        })),
        hourlyData: json.hourlyData || {
          spring: Array(12).fill(0),
          hypnos: Array(12).fill(0)
        },
      };

      setData(transformed);
      setLastUpdate(new Date().toLocaleTimeString());
      updateChart(transformed.hourlyData);
    } catch (err) {
      console.error("Error fetching production data:", err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (!productionChartRef.current) return;
    const ctx = productionChartRef.current.getContext("2d");

    if (chartInstance.current) chartInstance.current.destroy();

    chartInstance.current = new Chart(ctx, {
      type: "bar",
      data: {
        labels: [
          "9AM", "10AM", "11AM", "12PM", "1PM", "2PM",
          "3PM", "4PM", "5PM", "6PM", "7PM", "8PM",
        ],
        datasets: [
          {
            label: "Spring Mattresses",
            data: data.hourlyData.spring,
            backgroundColor: "rgba(52,152,219,0.7)",
            borderColor: "rgba(52,152,219,1)",
            borderWidth: 1,
          },
          {
            label: "Hypnos Mattresses",
            data: data.hourlyData.hypnos,
            backgroundColor: "rgba(46,204,113,0.7)",
            borderColor: "rgba(46,204,113,1)",
            borderWidth: 1,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: { 
            beginAtZero: true,
            title: { display: true, text: 'Number of Mattresses' }
          },
          x: { title: { display: true, text: 'Time of Day' } }
        },
        plugins: { title: { display: true, text: 'Production by Hour' } }
      },
    });

    fetchProductionData();
  }, []);

  useEffect(() => {
    fetchProductionData();
    const interval = setInterval(fetchProductionData, refreshInterval);
    return () => clearInterval(interval);
  }, [refreshInterval]);

  return (
    <div className="dashboard">
      {/* Header */}
      <div className="header">
        <div className="logo">
          <i className="fas fa-bed"></i>
          <h1>Peps Mattress Production</h1>
        </div>
        <div className="controls">
          <div className="refresh-control">
            <button 
              onClick={fetchProductionData}
              disabled={isLoading}
              className="refresh-btn"
            >
              <i className={`fas fa-sync-alt ${isLoading ? 'spinning' : ''}`}></i>
              {isLoading ? 'Refreshing...' : 'Refresh Now'}
            </button>
            <span>Auto Refresh:</span>
            <select
              value={refreshInterval}
              onChange={(e) => setRefreshInterval(parseInt(e.target.value))}
              disabled={isLoading}
            >
              <option value={15000}>15 seconds</option>
              <option value={30000}>30 seconds</option>
              <option value={60000}>1 minute</option>
              <option value={120000}>2 minutes</option>
              <option value={300000}>5 minutes</option>
              <option value={600000}>10 minutes</option>
              <option value={900000}>15 minutes</option>
              <option value={1800000}>30 minutes</option>
            </select>
          </div>
          <div className="status">
            <span className={`status-indicator ${isLoading ? 'status-refreshing' : 'status-active'}`}></span>
            <span>{isLoading ? 'Refreshing...' : 'Live'}</span>
          </div>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-header">
            <div className="stat-title">Total Production Today</div>
            <div className="stat-icon"><i className="fas fa-layer-group"></i></div>
          </div>
          <div className="stat-value">{data.totalProduction}</div>
        </div>
        <div className="stat-card">
          <div className="stat-header">
            <div className="stat-title">Spring Mattresses</div>
            <div className="stat-icon"><i className="fas fa-bed"></i></div>
          </div>
          <div className="stat-value">{data.springCount}</div>
        </div>
        <div className="stat-card">
          <div className="stat-header">
            <div className="stat-title">Hypnos Mattresses</div>
            <div className="stat-icon"><i className="fas fa-moon"></i></div>
          </div>
          <div className="stat-value">{data.hypnosCount}</div>
        </div>
        <div className="stat-card">
          <div className="stat-header">
            <div className="stat-title">Efficiency</div>
            <div className="stat-icon"><i className="fas fa-tachometer-alt"></i></div>
          </div>
          <div className="stat-value">{data.efficiency}%</div>
        </div>
      </div>

      {/* Content */}
      <div className="content-grid">
        <div className="main-content">
          <div className="chart-container">
            <h2>Hourly Production</h2>
            <div className="chart-wrapper">
              <canvas ref={productionChartRef}></canvas>
            </div>
          </div>
          <div className="breakdown-container">
            <h2>Product Breakdown</h2>
            <div className="breakdown-grid">
              {["single","double","queen","king"].map(size => (
                <div className="breakdown-item" key={size}>
                  <i className="fas fa-ruler-combined" style={{ color: "var(--secondary)" }}></i>
                  <div className="breakdown-value">
                    {(data.sizeBreakdown[size]?.spring||0)+(data.sizeBreakdown[size]?.hypnos||0)}
                  </div>
                  <div className="breakdown-label">{size.charAt(0).toUpperCase()+size.slice(1)} Size</div>
                  <div className="breakdown-detail">
                    Spring: {data.sizeBreakdown[size]?.spring||0} | Hypnos: {data.sizeBreakdown[size]?.hypnos||0}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="sidebar">
          <div className="timeline-container">
            <h2>Recent Production</h2>
            <div className="timeline">
              {data.recentItems.length > 0 ? data.recentItems.map((item, idx)=>(
                <div className="timeline-item" key={idx}>
                  <div className={`timeline-icon ${item.type==="SPRING"?"timeline-spring":"timeline-hypnos"}`}>
                    <i className={`fas ${item.type==="SPRING"?"fa-bed":"fa-moon"}`}></i>
                  </div>
                  <div className="timeline-content">
                    <div className="timeline-product">{item.type} Mattress - {item.size}</div>
                    <div className="timeline-time">Completed at {item.time}</div>
                  </div>
                </div>
              )) : <div className="no-data">No production records yet</div>}
            </div>
          </div>
        </div>
      </div>

      <div className="last-update">
        Last updated: <span>{lastUpdate}</span>
        {isLoading && <span className="updating"> (Updating...)</span>}
      </div>
    </div>
  );
}

export default App;
