import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { getPantryStats } from '../services/api'
import './Dashboard.css'

function Dashboard({ user }) {
  const navigate = useNavigate()
  const [pantryStats, setPantryStats] = useState({ totalItems: 0, inStock: 0, lowStock: 0, outOfStock: 0 })

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const result = await getPantryStats()
        if (result.success) {
          setPantryStats(result.data)
        }
      } catch (err) {
        // silent
      }
    }
    fetchStats()
  }, [])

  return (
    <div className="dashboard-page">
      <header className="dashboard-header">
        <div>
          <h1>Welcome back, {user?.firstName || 'User'}!</h1>
          <p className="header-subtitle">Here's your dorm overview</p>
        </div>
      </header>

      <div className="stats-grid">
        <div className="stat-card owed-to-you">
          <div className="stat-label">Owed to You</div>
          <div className="stat-value">₱0.00</div>
          <div className="stat-change positive">No pending debts</div>
        </div>
        <div className="stat-card you-owe">
          <div className="stat-label">You Owe</div>
          <div className="stat-value">₱0.00</div>
          <div className="stat-change positive">All settled up!</div>
        </div>
        <div className="stat-card net-balance">
          <div className="stat-label">Net Balance</div>
          <div className="stat-value">₱0.00</div>
          <div className="stat-change">You're all even</div>
        </div>
      </div>

      <div className="content-grid">
        <div className="card">
          <h3>Roommate Ledger</h3>
          <div className="empty-state">
            <span className="empty-icon">📋</span>
            <p>No expenses yet. Start by adding an expense!</p>
          </div>
        </div>
        <div className="card clickable-card" onClick={() => navigate('/pantry')}>
          <h3>Pantry Overview</h3>
          {pantryStats.totalItems > 0 ? (
            <div className="pantry-overview-stats">
              <div className="overview-stat">
                <span className="overview-dot green" />
                <span className="overview-label">In Stock</span>
                <span className="overview-count">{pantryStats.inStock}</span>
              </div>
              <div className="overview-stat">
                <span className="overview-dot yellow" />
                <span className="overview-label">Low Stock</span>
                <span className="overview-count">{pantryStats.lowStock}</span>
              </div>
              <div className="overview-stat">
                <span className="overview-dot red" />
                <span className="overview-label">Out of Stock</span>
                <span className="overview-count">{pantryStats.outOfStock}</span>
              </div>
              <div className="overview-total">
                {pantryStats.totalItems} total items →
              </div>
            </div>
          ) : (
            <div className="empty-state">
              <span className="empty-icon">🛒</span>
              <p>No pantry items yet. Click here to manage your shared pantry!</p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

export default Dashboard
