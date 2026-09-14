/**
 * Centralized API Service for Production Dashboard
 * All production data comes from the Spring Boot backend
 * No random data generation in the frontend
 */

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

/**
 * Generic API call helper with error handling
 */
async function apiCall(endpoint, options = {}) {
  try {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
      ...options,
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error(`API call failed for ${endpoint}:`, error);
    throw error;
  }
}

/**
 * Get current dashboard data
 * Returns: production totals, size breakdown, hourly data, recent items
 */
export async function getDashboard() {
  return apiCall('/dashboard');
}

/**
 * Get hourly production data for today
 * Returns: detailed hourly breakdown with Spring and Hypnos production
 */
export async function getHourlyProduction() {
  return apiCall('/production/hourly');
}

/**
 * Get daily production data for the last 14 days
 * Returns: daily totals, efficiency, downtime
 */
export async function getDailyProduction() {
  return apiCall('/production/daily');
}

/**
 * Get weekly production data for the last 8 weeks
 * Returns: weekly totals, targets, efficiency
 */
export async function getWeeklyProduction() {
  return apiCall('/production/weekly');
}

/**
 * Get monthly production data for the last 12 months
 * Returns: monthly totals, YTD data, targets
 */
export async function getMonthlyProduction() {
  return apiCall('/production/monthly');
}

/**
 * Get recent production records
 * Returns: last N completed production events
 */
export async function getRecentProduction(limit = 10) {
  return apiCall(`/production/recent?limit=${limit}`);
}

/**
 * Get production system status
 * Returns: simulator status, connection status, last update time
 */
export async function getProductionStatus() {
  return apiCall('/production/status');
}

/**
 * Get application settings
 * Returns: refresh interval, TV mode, auto-rotate settings, simulator mode
 */
export async function getSettings() {
  return apiCall('/settings');
}

/**
 * Update application settings
 * @param {Object} settings - Settings object to update
 */
export async function updateSettings(settings) {
  return apiCall('/settings', {
    method: 'PUT',
    body: JSON.stringify(settings),
  });
}

/**
 * Get simulator status
 * Returns: simulator active status, last event, events generated count
 */
export async function getSimulatorStatus() {
  return apiCall('/simulator/status');
}

/**
 * Trigger a manual simulator event
 * Generates a new production event via the simulator
 */
export async function triggerSimulatorEvent() {
  return apiCall('/simulator/event', {
    method: 'POST',
  });
}

/**
 * Get production history with filtering
 * @param {Object} filters - { line, shift, startDate, endDate }
 */
export async function getProductionHistory(filters = {}) {
  const params = new URLSearchParams(filters);
  return apiCall(`/production/history?${params}`);
}

// eslint-disable-next-line import/no-anonymous-default-export
export default {
  getDashboard,
  getHourlyProduction,
  getDailyProduction,
  getWeeklyProduction,
  getMonthlyProduction,
  getRecentProduction,
  getProductionStatus,
  getSettings,
  updateSettings,
  getSimulatorStatus,
  triggerSimulatorEvent,
  getProductionHistory,
};