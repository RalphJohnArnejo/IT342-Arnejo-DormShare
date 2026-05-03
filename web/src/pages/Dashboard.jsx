import React, { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { ClipboardList, ShoppingCart, Bell } from 'lucide-react'
import {
  getPantryStats,
  getExpenseSummary,
  getExpenseLedger,
  getNotifications,
  getUnreadNotificationCount,
  markNotificationRead,
} from '../services/api'
import './Dashboard.css'

function Dashboard({ user }) {
  const navigate = useNavigate()
  const [pantryStats, setPantryStats] = useState({ totalItems: 0, inStock: 0, lowStock: 0, outOfStock: 0 })
  const [expenseSummary, setExpenseSummary] = useState({ owedToYou: 0, youOwe: 0, netBalance: 0 })
  const [recentExpenses, setRecentExpenses] = useState([])
  const [notifications, setNotifications] = useState([])
  const [unreadCount, setUnreadCount] = useState(0)
  const lastSeenNotificationIdRef = useRef(null)

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [pantryRes, summaryRes, ledgerRes, notifRes, unreadRes] = await Promise.all([
          getPantryStats(),
          getExpenseSummary(),
          getExpenseLedger(),
          getNotifications({ limit: 5 }),
          getUnreadNotificationCount(),
        ])
        
        if (pantryRes.success) setPantryStats(pantryRes.data)
        if (summaryRes.success) setExpenseSummary(summaryRes.data)
        if (ledgerRes.success) setRecentExpenses(ledgerRes.data.slice(0, 3))
        if (notifRes.success) setNotifications(notifRes.data)
        if (unreadRes.success) setUnreadCount(unreadRes.data)
      } catch (err) {
        console.error('Dashboard data fetch error:', err)
      }
    }
    fetchData()
  }, [])

  useEffect(() => {
    // Ask once (browser prompt) — no extra UI
    if (typeof window === 'undefined' || typeof Notification === 'undefined') return
    if (Notification.permission === 'default') {
      try { Notification.requestPermission() } catch (e) { /* ignore */ }
    }
  }, [])

  useEffect(() => {
    let intervalId

    const refreshNotifications = async () => {
      try {
        const [notifRes, unreadRes] = await Promise.all([
          getNotifications({ limit: 5 }),
          getUnreadNotificationCount(),
        ])
        if (notifRes.success) setNotifications(notifRes.data)
        if (unreadRes.success) setUnreadCount(unreadRes.data)
      } catch (e) {
        // ignore
      }
    }

    // lightweight polling so "push" can surface while page is open
    intervalId = setInterval(refreshNotifications, 15000)

    return () => {
      if (intervalId) clearInterval(intervalId)
    }
  }, [])

  useEffect(() => {
    if (!notifications || notifications.length === 0) return

    const newest = notifications[0]
    const lastSeenId = lastSeenNotificationIdRef.current

    // First load: just set baseline
    if (lastSeenId == null) {
      lastSeenNotificationIdRef.current = newest.id
      return
    }

    // If nothing new, do nothing
    if (newest.id === lastSeenId) return

    // Update baseline first to avoid duplicate toasts
    lastSeenNotificationIdRef.current = newest.id

    // Minimal "push": only notify if already permitted
    if (typeof window === 'undefined' || typeof Notification === 'undefined') return
    if (Notification.permission !== 'granted') return

    try {
      new Notification(newest.title || 'DormShare Update', {
        body: newest.body || '',
      })
    } catch (e) {
      // ignore
    }
  }, [notifications])

  const handleNotificationClick = async (n) => {
    if (!n?.id || n.isRead) return

    try {
      const res = await markNotificationRead(n.id)
      if (res.success) {
        setNotifications((prev) => prev.map((x) => (x.id === n.id ? { ...x, isRead: true } : x)))
        setUnreadCount((prev) => Math.max(0, prev - 1))
      }
    } catch (err) {
      console.error('Failed to mark notification read:', err)
    }
  }

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
              <span className="empty-icon" style={{display: 'flex', justifyContent: 'center'}}><ClipboardList size={48} strokeWidth={1.5} color="#c49a3c" /></span>
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
              <span className="empty-icon" style={{display: 'flex', justifyContent: 'center'}}><ShoppingCart size={48} strokeWidth={1.5} color="#c49a3c" /></span>
              <p>No pantry items yet. Click here to manage your shared pantry!</p>
            </div>
          )}
        </div>
      </div>

      <div className="card" style={{ marginTop: '1.5rem' }}>
        <h3>
          Recent Updates
          {unreadCount > 0 && <span className="updates-badge">{unreadCount}</span>}
        </h3>

        {notifications.length > 0 ? (
          <div className="updates-list">
            {notifications.map((n) => (
              <button
                key={n.id}
                type="button"
                className={`update-item ${n.isRead ? '' : 'unread'}`}
                onClick={() => handleNotificationClick(n)}
              >
                <div className="update-main">
                  <div className="update-title">{n.title}</div>
                  <div className="update-body">{n.body}</div>
                </div>
                <div className="update-time">
                  {n.createdAt ? new Date(n.createdAt).toLocaleString() : ''}
                </div>
              </button>
            ))}
          </div>
        ) : (
          <div className="empty-state">
            <span className="empty-icon" style={{display: 'flex', justifyContent: 'center'}}><Bell size={48} strokeWidth={1.5} color="#c49a3c" /></span>
            <p>No recent activity yet.</p>
          </div>
        )}
      </div>
    </div>
  )
}

export default Dashboard
