/**
 * Centralized API Service for Production Dashboard
 * All production metrics and records originate from the Spring Boot + MySQL backend.
 * Zero random data generated on the frontend.
 */

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

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

export async function getDashboard() {
  return apiCall('/dashboard');
}

export async function getDailyProduction() {
  return apiCall('/daily');
}

export async function getWeeklyProduction() {
  return apiCall('/weekly');
}

export async function getMonthlyProduction() {
  return apiCall('/monthly');
}

export async function getRecentProduction(limit = 15) {
  return apiCall(`/recent?limit=${limit}`);
}

export async function getSettings() {
  return apiCall('/settings');
}

export async function updateSettings(settings) {
  return apiCall('/settings', {
    method: 'PUT',
    body: JSON.stringify(settings),
  });
}

export async function getAlerts() {
  return apiCall('/alerts');
}

export async function getDowntime() {
  return apiCall('/downtime');
}

export async function getProductionStatus() {
  return apiCall('/status');
}

// eslint-disable-next-line import/no-anonymous-default-export
export default {
  getDashboard,
  getDailyProduction,
  getWeeklyProduction,
  getMonthlyProduction,
  getRecentProduction,
  getSettings,
  updateSettings,
  getAlerts,
  getDowntime,
  getProductionStatus,
};