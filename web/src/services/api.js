import axios from 'axios';

// Auth API (public endpoints)
const AUTH_BASE_URL = 'http://localhost:8080/auth';

const authApi = axios.create({
  baseURL: AUTH_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Authenticated API (protected endpoints)
const API_BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Attach JWT token to every authenticated request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Handle 401 responses globally
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// ==================== AUTH ENDPOINTS ====================

export const registerUser = async (firstName, lastName, email, password) => {
  const response = await authApi.post('/register', {
    firstName,
    lastName,
    email,
    password,
  });
  return response.data;
};

export const loginUser = async (email, password) => {
  const response = await authApi.post('/login', {
    email,
    password,
  });
  return response.data;
};

// ==================== PANTRY ENDPOINTS ====================

export const getAllPantryItems = async () => {
  const response = await api.get('/pantry');
  return response.data;
};

export const getPantryStats = async () => {
  const response = await api.get('/pantry/stats');
  return response.data;
};

export const getPantryItemById = async (id) => {
  const response = await api.get(`/pantry/${id}`);
  return response.data;
};

export const getPantryItemsByStatus = async (status) => {
  const response = await api.get(`/pantry/status/${status}`);
  return response.data;
};

export const getPantryItemsByCategory = async (category) => {
  const response = await api.get(`/pantry/category/${category}`);
  return response.data;
};

export const searchPantryItems = async (query) => {
  const response = await api.get(`/pantry/search?q=${encodeURIComponent(query)}`);
  return response.data;
};

export const addPantryItem = async (itemData) => {
  const response = await api.post('/pantry', itemData);
  return response.data;
};

export const updatePantryItem = async (itemId, itemData) => {
  const response = await api.patch(`/pantry/${itemId}`, itemData);
  return response.data;
};

export const deletePantryItem = async (itemId) => {
  const response = await api.delete(`/pantry/${itemId}`);
  return response.data;
};

// ==================== EXPENSE ENDPOINTS ====================

export const logExpense = async (expenseData) => {
  const response = await api.post('/expenses', expenseData);
  return response.data;
};

export const getExpenseLedger = async () => {
  const response = await api.get('/expenses/ledger');
  return response.data;
};

export const getExpenseSummary = async () => {
  const response = await api.get('/expenses/summary');
  return response.data;
};

export const getAllRoommates = async () => {
  const response = await api.get('/expenses/users');
  return response.data;
};

export const settleSplit = async (splitId) => {
  const response = await api.patch(`/expenses/settle/${splitId}`);
  return response.data;
};

// ==================== GROUP ENDPOINTS ====================

export const createGroup = async (name) => {
  const response = await api.post('/groups', { name });
  return response.data;
};

export const joinGroup = async (inviteCode) => {
  const response = await api.post('/groups/join', { inviteCode });
  return response.data;
};

export const leaveGroup = async (groupId) => {
  const response = await api.delete(`/groups/leave/${groupId}`);
  return response.data;
};

export const getMyGroups = async () => {
  const response = await api.get('/groups/my');
  return response.data;
};

export const getGroupById = async (groupId) => {
  const response = await api.get(`/groups/${groupId}`);
  return response.data;
};

// ==================== USER PROFILE ENDPOINTS ====================

export const getProfile = async () => {
  const response = await api.get('/users/me');
  return response.data;
};

export const updateProfile = async (data) => {
  const response = await api.patch('/users/me', data);
  return response.data;
};

export const changePassword = async (currentPassword, newPassword) => {
  const response = await api.post('/users/change-password', { currentPassword, newPassword });
  return response.data;
};

// ==================== NOTIFICATION ENDPOINTS ====================

export const getNotifications = async ({ unreadOnly = false, limit = 10 } = {}) => {
  const response = await api.get('/notifications', {
    params: { unreadOnly, limit },
  });
  return response.data;
};

export const getUnreadNotificationCount = async () => {
  const response = await api.get('/notifications/unread-count');
  return response.data;
};

export const markNotificationRead = async (notificationId) => {
  const response = await api.patch(`/notifications/${notificationId}/read`);
  return response.data;
};

export const markAllNotificationsRead = async () => {
  const response = await api.patch('/notifications/read-all');
  return response.data;
};

export { api, authApi };
export default api;
