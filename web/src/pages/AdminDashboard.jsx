import React, { useState, useEffect } from 'react'
import {
  getAllUsers,
  deactivateUser,
  reactivateUser,
  getAllGroups,
  getSystemStats,
  getSystemLogs,
} from '../services/api'
import './AdminDashboard.css'

function AdminDashboard() {
  const [activeTab, setActiveTab] = useState('users')
  const [users, setUsers] = useState([])
  const [groups, setGroups] = useState([])
  const [stats, setStats] = useState({})
  const [logs, setLogs] = useState([])
  const [loading, setLoading] = useState(true)
  const [toast, setToast] = useState(null)
  const [deactivatingUserId, setDeactivatingUserId] = useState(null)
  const [searchQuery, setSearchQuery] = useState('')
  const [userFilter, setUserFilter] = useState('all') // all, active, inactive

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
        setUsers(usersRes.data || [])
      }
      if (groupsRes.success) {
        setGroups(groupsRes.data || [])
      }
      if (statsRes.success) {
        setStats(statsRes.data || {})
      }
      if (logsRes.success) {
        setLogs(logsRes.data || [])
      }
    } catch (err) {
      console.error('Admin data fetch error:', err)
      showToast('Failed to load admin data', 'error')
    } finally {
      setLoading(false)
    }
  }

  const showToast = (message, type = 'success') => {
    setToast({ message, type })
    setTimeout(() => setToast(null), 3000)
  }

  const handleDeactivateUser = async (userId, isActive) => {
    setDeactivatingUserId(userId)
    try {
      const res = isActive ? await deactivateUser(userId) : await reactivateUser(userId)
      if (res.success) {
        showToast(
          isActive ? 'User deactivated successfully' : 'User reactivated successfully',
          'success'
        )
        fetchAdminData()
      } else {
        showToast('Failed to update user status', 'error')
      }
    } catch (err) {
      console.error('Error updating user:', err)
      showToast('Error updating user status', 'error')
    } finally {
      setDeactivatingUserId(null)
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
        <h1>⚙️ Admin Dashboard</h1>
        <p className="admin-subheader">System management and oversight</p>
      </div>

      {toast && (
        <div className={`toast toast-${toast.type}`}>
          {toast.message}
        </div>
      )}

      {/* Stats Cards */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon">👥</div>
          <div className="stat-content">
            <div className="stat-label">Total Users</div>
            <div className="stat-value">{stats.totalUsers || 0}</div>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon">✅</div>
          <div className="stat-content">
            <div className="stat-label">Active Users</div>
            <div className="stat-value">{stats.activeUsers || 0}</div>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon">👥</div>
          <div className="stat-content">
            <div className="stat-label">Total Groups</div>
            <div className="stat-value">{stats.totalGroups || 0}</div>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon">💾</div>
          <div className="stat-content">
            <div className="stat-label">Storage Used</div>
            <div className="stat-value">{stats.storageUsedMB || 0} MB</div>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="admin-tabs">
        <button
          className={`tab ${activeTab === 'users' ? 'active' : ''}`}
          onClick={() => setActiveTab('users')}
        >
          👥 Users ({users.length})
        </button>
        <button
          className={`tab ${activeTab === 'groups' ? 'active' : ''}`}
          onClick={() => setActiveTab('groups')}
        >
          👫 Groups ({groups.length})
        </button>
        <button
          className={`tab ${activeTab === 'logs' ? 'active' : ''}`}
          onClick={() => setActiveTab('logs')}
        >
          📜 System Logs
        </button>
      </div>

      {/* Users Tab */}
      {activeTab === 'users' && (
        <div className="admin-section">
          <div className="section-header">
            <h2>User Management</h2>
            <div className="filter-controls">
              <input
                type="text"
                placeholder="Search users..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="search-input"
              />
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

          {filteredUsers.length === 0 ? (
            <div className="empty-message">
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
                          {u.role}
                        </span>
                      </td>
                      <td>
                        <span className={`status-badge status-${u.isActive ? 'active' : 'inactive'}`}>
                          {u.isActive ? '✓ Active' : '✕ Inactive'}
                        </span>
                      </td>
                      <td>{new Date(u.createdAt).toLocaleDateString()}</td>
                      <td>
                        <button
                          className={`btn-action ${!u.isActive ? 'btn-reactivate' : 'btn-deactivate'}`}
                          onClick={() => handleDeactivateUser(u.id, u.isActive)}
                          disabled={deactivatingUserId === u.id}
                        >
                          {deactivatingUserId === u.id
                            ? 'Processing...'
                            : u.isActive
                            ? '🔒 Deactivate'
                            : '🔓 Reactivate'}
                        </button>
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
              <p>No groups found</p>
            </div>
          ) : (
            <div className="groups-grid">
              {groups.map(group => (
                <div key={group.id} className="group-card">
                  <div className="group-card-header">
                    <h3>{group.name}</h3>
                    <span className="group-size">👥 {group.memberCount || 0}</span>
                  </div>
                  <div className="group-details">
                    <p>
                      <strong>Created:</strong> {new Date(group.createdAt).toLocaleDateString()}
                    </p>
                    <p>
                      <strong>Invite Code:</strong>
                      <code>{group.inviteCode}</code>
                    </p>
                  </div>
                  {group.description && (
                    <p className="group-description">{group.description}</p>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Logs Tab */}
      {activeTab === 'logs' && (
        <div className="admin-section">
          <h2>System Activity Logs</h2>
          {logs.length === 0 ? (
            <div className="empty-message">
              <p>No activity logs yet</p>
            </div>
          ) : (
            <div className="logs-container">
              <div className="logs-list">
                {logs.map((log, idx) => (
                  <div key={idx} className="log-entry">
                    <div className="log-timestamp">
                      {new Date(log.timestamp).toLocaleString()}
                    </div>
                    <div className="log-action">{log.action}</div>
                    <div className="log-details">
                      {log.user && <span>User: <strong>{log.user}</strong></span>}
                      {log.resource && <span>Resource: <code>{log.resource}</code></span>}
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
