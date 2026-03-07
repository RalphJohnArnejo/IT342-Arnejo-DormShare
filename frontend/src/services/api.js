import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/auth';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const registerUser = async (firstName, lastName, email, password) => {
  const response = await api.post('/register', {
    firstName,
    lastName,
    email,
    password,
  });
  return response.data;
};

export const loginUser = async (email, password) => {
  const response = await api.post('/login', {
    email,
    password,
  });
  return response.data;
};

export default api;
