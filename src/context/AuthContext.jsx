import React, { createContext, useContext, useState, useEffect } from 'react';
import { loginApi, registerApi, fetchMeApi } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(() => localStorage.getItem('dotfield_token'));
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function initAuth() {
      const storedToken = localStorage.getItem('dotfield_token');
      if (storedToken) {
        try {
          const userData = await fetchMeApi();
          setUser(userData);
        } catch {
          // Token invalid or expired
          localStorage.removeItem('dotfield_token');
          setToken(null);
          setUser(null);
        }
      }
      setLoading(false);
    }

    initAuth();

    function handleAuthExpired() {
      setToken(null);
      setUser(null);
    }

    window.addEventListener('dotfield_auth_expired', handleAuthExpired);
    return () => window.removeEventListener('dotfield_auth_expired', handleAuthExpired);
  }, []);

  const login = async (email, password) => {
    const data = await loginApi({ email, password });
    localStorage.setItem('dotfield_token', data.token);
    setToken(data.token);
    setUser(data.user);
    return data;
  };

  const register = async (email, password, name) => {
    const data = await registerApi({ email, password, name });
    localStorage.setItem('dotfield_token', data.token);
    setToken(data.token);
    setUser(data.user);
    return data;
  };

  const logout = () => {
    localStorage.removeItem('dotfield_token');
    setToken(null);
    setUser(null);
  };

  const value = {
    user,
    token,
    loading,
    isAuthenticated: !!user && !!token,
    isAdmin: user?.role === 'ADMIN',
    login,
    register,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
