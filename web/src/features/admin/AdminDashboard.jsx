import React, { useState, useEffect } from 'react'
import { Settings, Users, CheckCircle2, BarChart3, FileText, Check, X, Lock, Unlock, Shield, ShieldOff, RefreshCw, Search, Filter, Activity } from 'lucide-react'
import {
  getAllUsers,
  deactivateUser,
  reactivateUser,
  promoteUser,
  demoteUser,
  getAllGroups,
  getSystemStats,
  getSystemLogs,
} from '../../shared/services/api'
import './AdminDashboard.css'

function AdminDashboard() {
  const [activeTab, setActiveTab] = useState('users')
  const [users, setUsers] = useState([])
  const [groups, setGroups] = useState([])
  const [stats, setStats] = useState({})
  const [logs, setLogs] = useState([])
  const [loading, setLoading] = useState(true)
  const [toast, setToast] = useState(null)
  const [actionInProgress, setActionInProgress] = useState(null)
  const [searchQuery, setSearchQuery] = useState('')
  const [userFilter, setUserFilter] = useState('all') // all, active, inactive
  const [refreshing, setRefreshing] = useState(false)

  const user = JSON.parse(localStorage.getItem('user') || '{}')

  useEffect(() => {
    checkAdminAndFetch()
  }, [])

  const checkAdminAndFetch = async () => {
    // Check if user is admin
    if (user.role !== 'ADMIN') {
      showToast('You do not have admin access', 'error')
      window.location.href = '/dashboard'
      return
    }
    await fetchAdminData()
  }

  const fetchAdminData = async () => {
    setLoading(true)
    try {
      const [usersRes, groupsRes, statsRes, logsRes] = await Promise.all([
        getAllUsers(),
        getAllGroups(),
        getSystemStats(),
        getSystemLogs(),
      ])

      if (usersRes.success) {
        // API returns data directly as array
        setUsers(Array.isArray(usersRes.data) ? usersRes.data : (usersRes.data?.users || []))
      }
      if (groupsRes.success) {
        setGroups(Array.isArray(groupsRes.data) ? groupsRes.data : (groupsRes.data?.groups || []))
      }
      if (statsRes.success) {
        setStats(statsRes.data || {})
      }
      if (logsRes.success) {
        const logData = logsRes.data
        setLogs(Array.isArray(logData) ? logData : (logData?.logs || []))
      }
    } catch (err) {
      console.error('Admin data fetch error:', err)
      showToast('Failed to load admin data', 'error')
    } finally {
      setLoading(false)
    }
  }

  const handleRefresh = async () => {
    setRefreshing(true)
    await fetchAdminData()
    setRefreshing(false)
    showToast('Dashboard refreshed', 'success')
  }

  const showToast = (message, type = 'success') => {
    setToast({ message, type })
    setTimeout(() => setToast(null), 3000)
  }

  const handleDeactivateUser = async (userId, isActive) => {
    setActionInProgress(`deactivate-${userId}`)
    try {
      const res = isActive ? await deactivateUser(userId) : await reactivateUser(userId)
      if (res.success) {
        showToast(
          isActive ? 'User deactivated successfully' : 'User reactivated successfully',
          'success'
        )
        fetchAdminData()
      } else {
        showToast(res.message || 'Failed to update user status', 'error')
      }
    } catch (err) {
      console.error('Error updating user:', err)
      showToast(err.response?.data?.message || 'Error updating user status', 'error')
    } finally {
      setActionInProgress(null)
    }
  }

  const handlePromoteDemote = async (userId, currentRole) => {
    setActionInProgress(`role-${userId}`)
    try {
      const res = currentRole === 'ADMIN' ? await demoteUser(userId) : await promoteUser(userId)
      if (res.success) {
        showToast(
          currentRole === 'ADMIN' ? 'User demoted to USER' : 'User promoted to ADMIN',
          'success'
        )
        fetchAdminData()
      } else {
        showToast(res.message || 'Failed to update user role', 'error')
      }
    } catch (err) {
      console.error('Error updating role:', err)
      showToast(err.response?.data?.message || 'Error updating user role', 'error')
    } finally {
      setActionInProgress(null)
    }
  }

  const getFilteredUsers = () => {
    let filtered = users
    if (userFilter !== 'all') {
      filtered = filtered.filter(u =>
        userFilter === 'active' ? u.isActive : !u.isActive
      )
    }
    if (searchQuery) {
      filtered = filtered.filter(u =>
        u.firstName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
        u.lastName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
        u.email?.toLowerCase().includes(searchQuery.toLowerCase())
      )
    }
    return filtered
  }

  if (loading) {
    return (
      <div className="admin-container">
        <div className="loading">
          <div className="spinner"></div>
          <p>Loading admin dashboard...</p>
        </div>
      </div>
    )
  }

  const filteredUsers = getFilteredUsers()

  return (
    <div className="admin-container">
      <div className="admin-header">
        <div className="admin-header-row">
          <div>
            <h1 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}><Settings size={28} /> Admin Dashboard</h1>
            <p className="admin-subheader">System management and oversight</p>
          </div>
          <button
            className="btn-refresh"
            onClick={handleRefresh}
            disabled={refreshing}
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
          >
            <RefreshCw size={18} className={refreshing ? 'spinning' : ''} />
            {refreshing ? 'Refreshing...' : 'Refresh'}
          </button>
        </div>
      </div>

      {toast && (
        <div className={`toast toast-${toast.type}`}>
          {toast.type === 'success' ? <Check size={18} /> : <X size={18} />}
          {toast.message}
        </div>
      )}

      {/* Stats Cards */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon"><Users size={24} /></div>
          <div className="stat-content">
            <div className="stat-label">Total Users</div>
            <div className="stat-value">{stats.totalUsers || 0}</div>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon"><CheckCircle2 size={24} /></div>
          <div className="stat-content">
            <div className="stat-label">Active Users</div>
            <div className="stat-value">{stats.activeUsers || 0}</div>
            {stats.inactiveUsers > 0 && (
              <div className="stat-sub">{stats.inactiveUsers} inactive</div>
            )}
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon"><Users size={24} /></div>
          <div className="stat-content">
            <div className="stat-label">Total Groups</div>
            <div className="stat-value">{stats.totalGroups || 0}</div>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon"><BarChart3 size={24} /></div>
          <div className="stat-content">
            <div className="stat-label">Total Expenses</div>
            <div className="stat-value">{stats.totalExpenses || 0}</div>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="admin-tabs">
        <button
          className={`tab ${activeTab === 'users' ? 'active' : ''}`}
          onClick={() => setActiveTab('users')}
          style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
        >
          <Users size={18} /> Users ({users.length})
        </button>
        <button
          className={`tab ${activeTab === 'groups' ? 'active' : ''}`}
          onClick={() => setActiveTab('groups')}
          style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
        >
          <Users size={18} /> Groups ({groups.length})
        </button>
        <button
          className={`tab ${activeTab === 'logs' ? 'active' : ''}`}
          onClick={() => setActiveTab('logs')}
          style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
        >
          <Activity size={18} /> Audit Logs ({logs.length})
        </button>
      </div>

      {/* Users Tab */}
      {activeTab === 'users' && (
        <div className="admin-section">
          <div className="section-header">
            <h2>User Management</h2>
            <div className="filter-controls">
              <div className="search-wrapper">
                <Search size={16} className="search-icon" />
                <input
                  type="text"
                  placeholder="Search users..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="search-input"
                />
              </div>
              <div className="filter-wrapper">
                <Filter size={16} className="filter-icon-el" />
                <select
                  value={userFilter}
                  onChange={(e) => setUserFilter(e.target.value)}
                  className="filter-select"
                >
                  <option value="all">All Users</option>
                  <option value="active">Active</option>
                  <option value="inactive">Inactive</option>
                </select>
              </div>
            </div>
          </div>

          {filteredUsers.length === 0 ? (
            <div className="empty-message">
              <Users size={48} style={{ opacity: 0.3, marginBottom: '1rem' }} />
              <p>No users found</p>
            </div>
          ) : (
            <div className="users-table-container">
              <table className="users-table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Role</th>
                    <th>Status</th>
                    <th>Joined</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredUsers.map(u => (
                    <tr key={u.id} className={!u.isActive ? 'inactive-row' : ''}>
                      <td>
                        <div className="user-cell">
                          <div className="user-avatar">
                            {u.firstName?.charAt(0)?.toUpperCase() || '?'}
                          </div>
                          <span>{u.firstName} {u.lastName}</span>
                        </div>
                      </td>
                      <td>
                        <code className="email-code">{u.email}</code>
                      </td>
                      <td>
                        <span className={`role-badge role-${u.role?.toLowerCase()}`}>
                          {u.role === 'ADMIN' ? <Shield size={12} /> : null}
                          {u.role}
                        </span>
                      </td>
                      <td>
                        <span className={`status-badge status-${u.isActive ? 'active' : 'inactive'}`} style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                          {u.isActive ? <><Check size={14} /> Active</> : <><X size={14} /> Inactive</>}
                        </span>
                      </td>
                      <td>{u.createdAt ? new Date(u.createdAt).toLocaleDateString() : '—'}</td>
                      <td>
                        <div className="action-buttons">
                          {/* Deactivate / Reactivate */}
                          {u.id !== user.userId && (
                            <button
                              className={`btn-action ${!u.isActive ? 'btn-reactivate' : 'btn-deactivate'}`}
                              onClick={() => handleDeactivateUser(u.id, u.isActive)}
                              disabled={actionInProgress === `deactivate-${u.id}`}
                              title={u.isActive ? 'Deactivate user' : 'Reactivate user'}
                              style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}
                            >
                              {actionInProgress === `deactivate-${u.id}`
                                ? 'Processing...'
                                : u.isActive
                                ? <><Lock size={14} /> Deactivate</>
                                : <><Unlock size={14} /> Reactivate</>}
                            </button>
                          )}

                          {/* Promote / Demote */}
                          {u.id !== user.userId && u.role !== 'ADMIN' && (
                            <button
                              className="btn-action btn-promote"
                              onClick={() => handlePromoteDemote(u.id, u.role)}
                              disabled={actionInProgress === `role-${u.id}`}
                              title="Promote to Admin"
                              style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}
                            >
                              {actionInProgress === `role-${u.id}`
                                ? 'Processing...'
                                : <><Shield size={14} /> Promote</>}
                            </button>
                          )}
                          {u.id !== user.userId && u.role === 'ADMIN' && (
                            <button
                              className="btn-action btn-demote"
                              onClick={() => handlePromoteDemote(u.id, u.role)}
                              disabled={actionInProgress === `role-${u.id}`}
                              title="Demote to User"
                              style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}
                            >
                              {actionInProgress === `role-${u.id}`
                                ? 'Processing...'
                                : <><ShieldOff size={14} /> Demote</>}
                            </button>
                          )}

                          {u.id === user.userId && (
                            <span className="you-badge">You</span>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Groups Tab */}
      {activeTab === 'groups' && (
        <div className="admin-section">
          <h2>Group Oversight</h2>
          {groups.length === 0 ? (
            <div className="empty-message">
              <Users size={48} style={{ opacity: 0.3, marginBottom: '1rem' }} />
              <p>No groups found</p>
            </div>
          ) : (
            <div className="groups-grid">
              {groups.map(group => (
                <div key={group.id} className="group-card">
                  <div className="group-card-header">
                    <h3>{group.name}</h3>
                    <span className="group-size" style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}><Users size={16} /> {group.memberCount || 0}</span>
                  </div>
                  <div className="group-details">
                    <p>
                      <strong>Created:</strong> {group.createdAt ? new Date(group.createdAt).toLocaleDateString() : '—'}
                    </p>
                    <p>
                      <strong>Invite Code:</strong>
                      <code>{group.inviteCode}</code>
                    </p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Logs Tab */}
      {activeTab === 'logs' && (
        <div className="admin-section">
          <h2 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Activity size={22} /> Audit Logs
          </h2>
          {logs.length === 0 ? (
            <div className="empty-message">
              <FileText size={48} style={{ opacity: 0.3, marginBottom: '1rem' }} />
              <p>No audit logs yet</p>
              <p style={{ fontSize: '0.85rem', opacity: 0.6 }}>Admin actions will appear here</p>
            </div>
          ) : (
            <div className="logs-container">
              <div className="logs-list">
                {logs.map((log, idx) => (
                  <div key={log.id || idx} className={`log-entry log-${log.action?.toLowerCase()?.includes('deactivat') ? 'warning' : log.action?.toLowerCase()?.includes('promot') ? 'info' : 'default'}`}>
                    <div className="log-header">
                      <div className="log-action-badge">
                        {log.action?.replace(/_/g, ' ')}
                      </div>
                      <div className="log-timestamp">
                        {log.timestamp ? new Date(log.timestamp).toLocaleString() : '—'}
                      </div>
                    </div>
                    <div className="log-details">
                      {log.user && <span>By: <strong>{log.user}</strong></span>}
                      {log.targetType && <span>Target: <code>{log.targetType} #{log.targetId}</code></span>}
                    </div>
                    {log.details && (
                      <div className="log-description">{log.details}</div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

export default AdminDashboard
