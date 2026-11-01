import api from '../api/axiosInstance';

export const login = async (email, password) => {
  const response = await api.post('/api/auth/login', { email, password });
  return response.data; // ApiResponse wrapper
};

export const register = async (name, email, password) => {
  const response = await api.post('/api/auth/register', { name, email, password });
  return response.data;
};

export const fetchCurrentUser = async () => {
  const response = await api.get('/api/auth/me');
  return response.data;
};
