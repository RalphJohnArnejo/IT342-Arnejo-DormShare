import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { getPantryStats, getExpenseSummary, getExpenseLedger } from '../services/api'
import './Dashboard.css'

function Dashboard({ user }) {
  const navigate = useNavigate()
  const [pantryStats, setPantryStats] = useState({ totalItems: 0, inStock: 0, lowStock: 0, outOfStock: 0 })
  const [expenseSummary, setExpenseSummary] = useState({ owedToYou: 0, youOwe: 0, netBalance: 0 })
  const [recentExpenses, setRecentExpenses] = useState([])

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [pantryRes, summaryRes, ledgerRes] = await Promise.all([
          getPantryStats(),
          getExpenseSummary(),
          getExpenseLedger()
        ])
        
        if (pantryRes.success) setPantryStats(pantryRes.data)
        if (summaryRes.success) setExpenseSummary(summaryRes.data)
        if (ledgerRes.success) setRecentExpenses(ledgerRes.data.slice(0, 3))
      } catch (err) {
        console.error('Dashboard data fetch error:', err)
      }
    }
    fetchData()
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
          <div className="stat-value">₱{expenseSummary.owedToYou.toLocaleString()}</div>
          <div className="stat-change">From roommate splits</div>
        </div>
        <div className="stat-card you-owe">
          <div className="stat-label">You Owe</div>
          <div className="stat-value">₱{expenseSummary.youOwe.toLocaleString()}</div>
          <div className="stat-change">Unsettled debts</div>
        </div>
        <div className="stat-card net-balance">
          <div className="stat-label">Net Balance</div>
          <div className="stat-value">₱{expenseSummary.netBalance.toLocaleString()}</div>
          <div className="stat-change">{expenseSummary.netBalance >= 0 ? 'Surplus' : 'Deficit'}</div>
        </div>
      </div>

      <div className="content-grid">
        <div className="card clickable-card" onClick={() => navigate('/expenses')}>
          <h3>Roommate Ledger</h3>
          {recentExpenses.length > 0 ? (
            <div className="recent-ledger">
              {recentExpenses.map(exp => (
                <div key={exp.id} className="ledger-item-mini">
                  <div className="ledger-info">
                    <span className="ledger-title">{exp.description}</span>
                    <span className="ledger-date">{new Date(exp.date).toLocaleDateString()}</span>
                  </div>
                  <div className="ledger-amt">₱{exp.amount}</div>
                </div>
              ))}
              <div className="overview-total" style={{ marginTop: '1rem' }}>
                View all expenses →
              </div>
            </div>
          ) : (
            <div className="empty-state">
              <span className="empty-icon">📋</span>
              <p>No expenses yet. Start by adding an expense!</p>
            </div>
          )}
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
