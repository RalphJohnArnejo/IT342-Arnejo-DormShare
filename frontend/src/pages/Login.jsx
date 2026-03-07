import { useState } from 'react'
import { Link } from 'react-router-dom'
import { loginUser } from '../services/api'
import './Auth.css'

function Login({ onLogin }) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      const result = await loginUser(email, password)
      if (result.success) {
        onLogin(result.data.token, result.data)
      } else {
        setError(result.error?.message || 'Login failed')
      }
    } catch (err) {
      const errorData = err.response?.data
      setError(errorData?.error?.message || 'Invalid email or password')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-left">
          <div className="brand">
            <div className="brand-icon">🏠</div>
            <h1>DormShare</h1>
            <p className="tagline">Manage your dorm life, together.</p>
            <p className="description">
              Automated expense splitting and real-time pantry tracking for roommates.
            </p>
          </div>
        </div>
        <div className="auth-right">
          <h2>Welcome Back</h2>
          <p className="subtitle">Log in to your account</p>

          {error && <div className="error-message">{error}</div>}

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label htmlFor="email">Email Address</label>
              <input
                id="email"
                type="email"
                placeholder="Enter your email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="password">Password</label>
              <input
                id="password"
                type="password"
                placeholder="Enter your password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>

            <button type="submit" className="btn-primary" disabled={loading}>
              {loading ? 'Signing In...' : 'Sign In'}
            </button>
          </form>

          <p className="auth-link">
            Don't have an account? <Link to="/register">Sign Up</Link>
          </p>
        </div>
      </div>
    </div>
  )
}

export default Login
