import React, { useState, useEffect } from 'react'
import { Crown, User, Check, Copy, Sparkles, Link, AlertTriangle } from 'lucide-react'
import { createGroup, joinGroup, leaveGroup, getMyGroups } from '../services/api'
import './Groups.css'

function Groups({ user }) {
  const [groups, setGroups] = useState([])
  const [loading, setLoading] = useState(true)
  const [activeTab, setActiveTab] = useState('create')
  const [toast, setToast] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  // Form state
  const [groupName, setGroupName] = useState('')
  const [inviteCode, setInviteCode] = useState('')
  const [copiedId, setCopiedId] = useState(null)
  const [leaveTarget, setLeaveTarget] = useState(null)

  useEffect(() => {
    fetchGroups()
  }, [])

  useEffect(() => {
    if (toast) {
      const timer = setTimeout(() => setToast(null), 3500)
      return () => clearTimeout(timer)
    }
  }, [toast])

  const showToast = (message, type = 'success') => {
    setToast({ message, type })
  }

  const fetchGroups = async () => {
    try {
      setLoading(true)
      const res = await getMyGroups()
      if (res.success) {
        setGroups(res.data || [])
      }
    } catch (err) {
      console.error('Failed to fetch groups:', err)
    } finally {
      setLoading(false)
    }
  }

  const handleCreate = async (e) => {
    e.preventDefault()
    if (!groupName.trim()) {
      showToast('Please enter a group name', 'error')
      return
    }
    setSubmitting(true)
    try {
      const res = await createGroup(groupName.trim())
      if (res.success) {
        showToast(`Group "${groupName}" created!`)
        setGroupName('')
        fetchGroups()
      } else {
        showToast(res.error?.details || 'Failed to create group', 'error')
      }
    } catch (err) {
      showToast('Network error', 'error')
    } finally {
      setSubmitting(false)
    }
  }

  const handleJoin = async (e) => {
    e.preventDefault()
    if (!inviteCode.trim()) {
      showToast('Please enter an invite code', 'error')
      return
    }
    setSubmitting(true)
    try {
      const res = await joinGroup(inviteCode.trim().toUpperCase())
      if (res.success) {
        showToast('Successfully joined the group!')
        setInviteCode('')
        fetchGroups()
      } else {
        showToast(res.error?.details || 'Invalid invite code', 'error')
      }
    } catch (err) {
      showToast('Network error', 'error')
    } finally {
      setSubmitting(false)
    }
  }

  const handleLeave = async () => {
    if (!leaveTarget) return
    try {
      const res = await leaveGroup(leaveTarget.id)
      if (res.success) {
        showToast(`Left "${leaveTarget.name}"`)
        setLeaveTarget(null)
        fetchGroups()
      } else {
        showToast(res.error?.details || 'Failed to leave group', 'error')
      }
    } catch (err) {
      showToast('Network error', 'error')
    }
  }

  const copyInviteCode = (code, groupId) => {
    navigator.clipboard.writeText(code).then(() => {
      setCopiedId(groupId)
      setTimeout(() => setCopiedId(null), 2000)
    })
  }

  const getMemberRole = (group) => {
    const member = group.members?.find(m => m.userId === user?.userId)
    return member?.role || 'MEMBER'
  }

  const formatDate = (dateStr) => {
    if (!dateStr) return ''
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'short', day: 'numeric', year: 'numeric'
    })
  }

  return (
    <div className="groups-page">
      {/* Header */}
      <header className="groups-header">
        <div>
          <h1>My Groups</h1>
          <p className="subtitle">Manage your dorm households</p>
        </div>
      </header>

      {/* Group Cards */}
      {loading ? (
        <div className="skeleton-grid">
          {[1, 2].map(i => (
            <div key={i} className="skeleton-card">
              <div className="skeleton-line title" />
              <div className="skeleton-line text" />
              <div className="skeleton-line short" />
            </div>
          ))}
        </div>
      ) : groups.length > 0 ? (
        <div className="groups-grid">
          {groups.map((group, index) => (
            <div key={group.id} className="group-card" style={{ animationDelay: `${index * 0.08}s` }}>
              <div className="group-card-header">
                <div>
                  <h3 className="group-card-title">{group.name}</h3>
                  <span className={`role-badge ${getMemberRole(group).toLowerCase()}`}>
                    {getMemberRole(group) === 'ADMIN' ? <span style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}><Crown size={14} color="#c49a3c" /> Admin</span> : <span style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}><User size={14} /> Member</span>}
                  </span>
                </div>
                <button
                  className="btn-leave"
                  onClick={() => setLeaveTarget(group)}
                  title="Leave group"
                >
                  Leave
                </button>
              </div>

              <div className="invite-code-box">
                <span className="invite-label">Invite Code</span>
                <div className="invite-code-row">
                  <code className="invite-code">{group.inviteCode}</code>
                  <button
                    className={`btn-copy ${copiedId === group.id ? 'copied' : ''}`}
                    onClick={() => copyInviteCode(group.inviteCode, group.id)}
                    style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}
                  >
                    {copiedId === group.id ? <><Check size={14} /> Copied</> : <><Copy size={14} /> Copy</>}
                  </button>
                </div>
              </div>

              <div className="members-section">
                <h4 className="members-title">
                  Members ({group.members?.length || 0})
                </h4>
                <div className="members-list">
                  {(group.members || []).map(member => (
                    <div key={member.userId} className="member-row">
                      <div className="member-avatar">
                        {member.name?.charAt(0)?.toUpperCase() || '?'}
                      </div>
                      <div className="member-info">
                        <span className="member-name">
                          {member.name}
                          {member.userId === user?.userId && <span className="you-tag"> (You)</span>}
                        </span>
                        <span className="member-email">{member.email}</span>
                      </div>
                      <span className={`member-role ${member.role?.toLowerCase()}`}>
                        {member.role === 'ADMIN' ? <Crown size={16} color="#c49a3c" /> : ''}
                      </span>
                    </div>
                  ))}
                </div>
              </div>

              <div className="group-card-footer">
                Created {formatDate(group.createdAt)}
              </div>
            </div>
          ))}
        </div>
      ) : null}

      {/* Create or Join Section */}
      <div className="action-section">
        <div className="tabs">
          <button
            className={`tab ${activeTab === 'create' ? 'active' : ''}`}
            onClick={() => setActiveTab('create')}
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
          >
            <Sparkles size={16} /> Create Group
          </button>
          <button
            className={`tab ${activeTab === 'join' ? 'active' : ''}`}
            onClick={() => setActiveTab('join')}
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
          >
            <Link size={16} /> Join Group
          </button>
        </div>

        {activeTab === 'create' ? (
          <form className="action-form" onSubmit={handleCreate}>
            <div className="form-group">
              <label htmlFor="group-name">Group Name</label>
              <input
                id="group-name"
                type="text"
                placeholder="e.g. Room 301, Apartment 4B..."
                value={groupName}
                onChange={e => setGroupName(e.target.value)}
                required
              />
            </div>
            <button
              type="submit"
              className="btn-submit"
              disabled={submitting || !groupName.trim()}
            >
              {submitting ? 'Creating...' : 'Create Group'}
            </button>
          </form>
        ) : (
          <form className="action-form" onSubmit={handleJoin}>
            <div className="form-group">
              <label htmlFor="invite-code-input">Invite Code</label>
              <input
                id="invite-code-input"
                type="text"
                placeholder="e.g. A3X7K9"
                value={inviteCode}
                onChange={e => setInviteCode(e.target.value.toUpperCase())}
                maxLength={6}
                style={{ letterSpacing: '0.3em', textAlign: 'center', fontWeight: 700, fontSize: '1.2rem' }}
                required
              />
            </div>
            <button
              type="submit"
              className="btn-submit"
              disabled={submitting || inviteCode.length < 6}
            >
              {submitting ? 'Joining...' : 'Join Group'}
            </button>
          </form>
        )}
      </div>

      {/* Leave Confirmation Modal */}
      {leaveTarget && (
        <div className="modal-overlay" onClick={() => setLeaveTarget(null)}>
          <div className="modal-content" onClick={e => e.stopPropagation()}>
            <div className="delete-confirm">
              <span className="delete-confirm-icon"><AlertTriangle size={32} color="#c49a3c" /></span>
              <h3>Leave "{leaveTarget.name}"?</h3>
              <p>You will no longer have access to this group's pantry and expenses. You can rejoin later with an invite code.</p>
              <div className="delete-confirm-actions">
                <button className="btn-cancel" onClick={() => setLeaveTarget(null)}>
                  Cancel
                </button>
                <button className="btn-delete-confirm" onClick={handleLeave}>
                  Leave Group
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Toast */}
      {toast && (
        <div className={`toast ${toast.type}`} key={Date.now()}>
          {toast.message}
        </div>
      )}
    </div>
  )
}

export default Groups
