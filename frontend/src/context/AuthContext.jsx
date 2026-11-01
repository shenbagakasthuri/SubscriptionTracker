import React, {
  createContext,
  useCallback,
  useEffect,
  useState,
} from 'react';
import {
  fetchCurrentUser,
  login as loginApi,
  register as registerApi,
} from '../api/auth';

export const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const loadUser = useCallback(async () => {
    const token = sessionStorage.getItem('jwtToken');

    if (!token) {
      setUser(null);
      setLoading(false);
      return null;
    }

    try {
      const response = await fetchCurrentUser();

      if (!response.success || !response.data) {
        throw new Error('Unable to retrieve user profile');
      }

      setUser(response.data);
      return response.data;
    } catch (error) {
      sessionStorage.removeItem('jwtToken');
      setUser(null);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadUser();
  }, [loadUser]);

  const saveAuthResponse = (response) => {
    if (!response.success || !response.data?.token) {
      throw new Error(response.message || 'Authentication failed');
    }

    sessionStorage.setItem('jwtToken', response.data.token);

    const authenticatedUser = {
      id: response.data.userId,
      name: response.data.name,
      email: response.data.email,
    };

    setUser(authenticatedUser);
    setLoading(false);

    return authenticatedUser;
  };

  const login = async (email, password) => {
    const response = await loginApi(email, password);
    return saveAuthResponse(response);
  };

  const register = async (name, email, password) => {
    const response = await registerApi(name, email, password);
    return saveAuthResponse(response);
  };

  const logout = () => {
    sessionStorage.removeItem('jwtToken');
    sessionStorage.removeItem('currentUser');
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{ user, loading, login, register, logout, loadUser }}
    >
      {children}
    </AuthContext.Provider>
  );
};