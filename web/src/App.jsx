import React, { useState } from 'react'
import { BrowserRouter as Router, Routes, Route, Navigate, useLocation, useNavigate } from 'react-router-dom'
import Login from './pages/Login'
import Register from './pages/Register'
import Dashboard from './pages/Dashboard'
import Pantry from './pages/Pantry'
import Expenses from './pages/Expenses'
import OAuth2Callback from './pages/OAuth2Callback'
import './App.css'

function Sidebar({ user, onLogout }) {
  const location = useLocation()
  const navigate = useNavigate()

  const navItems = [
    { path: '/dashboard', label: 'Dashboard', icon: '📊' },
    { path: '/pantry', label: 'Pantry', icon: '🛒' },
    { path: '/expenses', label: 'Expenses', icon: '💰' },
    { path: '/profile', label: 'Profile', icon: '👤' },
  ]

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <span className="brand-icon">🏠</span>
        <h2>DormShare</h2>
      </div>
      <nav className="sidebar-nav">
        {navItems.map(item => (
          <a
            key={item.path}
            href="#"
            className={`nav-item ${location.pathname === item.path ? 'active' : ''}`}
            onClick={(e) => { e.preventDefault(); navigate(item.path) }}
          >
            <span className="nav-icon">{item.icon}</span>
            {item.label}
          </a>
        ))}
      </nav>
      <button className="btn-logout" onClick={onLogout}>
        <span className="nav-icon">🚪</span>
        Logout
      </button>
    </aside>
  )
}

function AppLayout({ user, token, onLogout }) {
  return (
    <div className="dashboard-container">
      <Sidebar user={user} onLogout={onLogout} />
      <main className="main-content">
        <Routes>
          <Route path="/dashboard" element={<Dashboard user={user} />} />
          <Route path="/pantry" element={<Pantry user={user} />} />
          <Route path="/expenses" element={<Expenses user={user} />} />
          <Route path="/profile" element={
            <div style={{ padding: '2rem', color: '#94a3b8' }}>
              <h1 style={{ color: '#f1f5f9', marginBottom: '0.5rem' }}>Profile</h1>
              <p>Welcome, {user?.firstName} {user?.lastName}</p>
              <p>Email: {user?.email}</p>
            </div>
          } />
          <Route path="*" element={<Navigate to="/dashboard" />} />
        </Routes>
      </main>
    </div>
  )
}

function App() {
  const [token, setToken] = useState(localStorage.getItem('token'))
  const [user, setUser] = useState(() => {
    try {
      const savedUser = localStorage.getItem('user')
      return savedUser ? JSON.parse(savedUser) : null
    } catch (e) {
      console.error('Failed to parse user from localStorage', e)
      localStorage.removeItem('user')
      return null
    }
  })

  const handleLogin = (tokenValue, userData) => {
    localStorage.setItem('token', tokenValue)
    localStorage.setItem('user', JSON.stringify(userData))
    setToken(tokenValue)
    setUser(userData)
  }

  const handleLogout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setToken(null)
    setUser(null)
  }

  return (
    <Router>
      <Routes>
        <Route path="/login" element={
          token ? <Navigate to="/dashboard" /> : <Login onLogin={handleLogin} />
        } />
        <Route path="/register" element={
          token ? <Navigate to="/dashboard" /> : <Register />
        } />
        <Route path="/oauth2/callback" element={
          <OAuth2Callback onLogin={handleLogin} />
        } />
        <Route path="/*" element={
          token
            ? <AppLayout user={user} token={token} onLogout={handleLogout} />
            : <Navigate to="/login" />
        } />
      </Routes>
    </Router>
  )
}

export default App
