import React, { useState, useEffect } from 'react'
import { getProfile, updateProfile, changePassword, getMyGroups } from '../services/api'
import './Profile.css'

function Profile({ user, onProfileUpdate }) {
  const [profile, setProfile] = useState(null)
  const [groups, setGroups] = useState([])
  const [loading, setLoading] = useState(true)
  const [toast, setToast] = useState(null)
  const [saving, setSaving] = useState(false)

  // Edit profile form
  const [editMode, setEditMode] = useState(false)
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [email, setEmail] = useState('')

  // Change password form
  const [showPasswordForm, setShowPasswordForm] = useState(false)
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')

  useEffect(() => {
    fetchProfile()
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

  const fetchProfile = async () => {
    try {
      setLoading(true)
      const res = await getProfile()
      if (res.success && res.data) {
        setProfile(res.data)
        setFirstName(res.data.firstName || '')
        setLastName(res.data.lastName || '')
        setEmail(res.data.email || '')
      }
    } catch (err) {
      console.error('Failed to fetch profile:', err)
    } finally {
      setLoading(false)
    }
  }

  const fetchGroups = async () => {
    try {
      const res = await getMyGroups()
      if (res.success) {
        setGroups(res.data || [])
      }
    } catch (err) {
      console.error('Failed to fetch groups:', err)
    }
  }

  const handleSaveProfile = async (e) => {
    e.preventDefault()
    if (!firstName.trim() || !lastName.trim() || !email.trim()) {
      showToast('All fields are required', 'error')
      return
    }
    setSaving(true)
    try {
      const res = await updateProfile({
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        email: email.trim()
      })
      if (res.success) {
        setProfile(res.data)
        setEditMode(false)
        showToast('Profile updated successfully! 🎉')
        // Update localStorage so sidebar/header reflect changes
        const stored = JSON.parse(localStorage.getItem('user') || '{}')
        stored.firstName = res.data.firstName
        stored.lastName = res.data.lastName
        stored.email = res.data.email
        localStorage.setItem('user', JSON.stringify(stored))
        if (onProfileUpdate) onProfileUpdate(stored)
      } else {
        showToast(res.error?.details || 'Failed to update profile', 'error')
      }
    } catch (err) {
      showToast('Network error', 'error')
    } finally {
      setSaving(false)
    }
  }

  const handleChangePassword = async (e) => {
    e.preventDefault()
    if (!currentPassword || !newPassword) {
      showToast('All password fields are required', 'error')
      return
    }
    if (newPassword.length < 8) {
      showToast('New password must be at least 8 characters', 'error')
      return
    }
    if (newPassword !== confirmPassword) {
      showToast('New passwords do not match', 'error')
      return
    }
    setSaving(true)
    try {
      const res = await changePassword(currentPassword, newPassword)
      if (res.success) {
        showToast('Password changed successfully! 🔒')
        setShowPasswordForm(false)
        setCurrentPassword('')
        setNewPassword('')
        setConfirmPassword('')
      } else {
        showToast(res.error?.details || 'Failed to change password', 'error')
      }
    } catch (err) {
      const msg = err.response?.data?.error?.details || 'Network error'
      showToast(msg, 'error')
    } finally {
      setSaving(false)
    }
  }

  const cancelEdit = () => {
    setEditMode(false)
    setFirstName(profile?.firstName || '')
    setLastName(profile?.lastName || '')
    setEmail(profile?.email || '')
  }

  const formatDate = (dateStr) => {
    if (!dateStr) return 'N/A'
    return new Date(dateStr).toLocaleDateString('en-US', {
      year: 'numeric', month: 'long', day: 'numeric'
    })
  }

  const getInitials = () => {
    const f = profile?.firstName?.charAt(0)?.toUpperCase() || ''
    const l = profile?.lastName?.charAt(0)?.toUpperCase() || ''
    return f + l
  }

  if (loading) {
    return (
      <div className="profile-page">
        <div className="profile-skeleton">
          <div className="skeleton-avatar" />
          <div className="skeleton-line title" />
          <div className="skeleton-line text" />
          <div className="skeleton-line short" />
        </div>
      </div>
    )
  }

  return (
    <div className="profile-page">
      {/* Header Card */}
      <div className="profile-hero">
        <div className="profile-avatar">
          {getInitials()}
        </div>
        <div className="profile-hero-info">
          <h1>{profile?.firstName} {profile?.lastName}</h1>
          <p className="profile-email">{profile?.email}</p>
          <div className="profile-meta">
            <span className="meta-badge role">{profile?.role}</span>
            <span className="meta-badge date">Member since {formatDate(profile?.createdAt)}</span>
          </div>
        </div>
      </div>

      {/* Two-column layout */}
      <div className="profile-grid">

        {/* Left: Account Details */}
        <div className="profile-card">
          <div className="card-header">
            <h2>Account Details</h2>
            {!editMode && (
              <button className="btn-edit" onClick={() => setEditMode(true)}>
                ✏️ Edit
              </button>
            )}
          </div>

          {editMode ? (
            <form onSubmit={handleSaveProfile} className="profile-form">
              <div className="form-group">
                <label htmlFor="profile-fname">First Name</label>
                <input
                  id="profile-fname"
                  type="text"
                  value={firstName}
                  onChange={e => setFirstName(e.target.value)}
                  required
                />
              </div>
              <div className="form-group">
                <label htmlFor="profile-lname">Last Name</label>
                <input
                  id="profile-lname"
                  type="text"
                  value={lastName}
                  onChange={e => setLastName(e.target.value)}
                  required
                />
              </div>
              <div className="form-group">
                <label htmlFor="profile-email">Email</label>
                <input
                  id="profile-email"
                  type="email"
                  value={email}
                  onChange={e => setEmail(e.target.value)}
                  required
                />
              </div>
              <div className="form-actions">
                <button type="button" className="btn-cancel" onClick={cancelEdit}>Cancel</button>
                <button type="submit" className="btn-save" disabled={saving}>
                  {saving ? 'Saving...' : 'Save Changes'}
                </button>
              </div>
            </form>
          ) : (
            <div className="detail-list">
              <div className="detail-row">
                <span className="detail-label">First Name</span>
                <span className="detail-value">{profile?.firstName}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Last Name</span>
                <span className="detail-value">{profile?.lastName}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Email</span>
                <span className="detail-value">{profile?.email}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Role</span>
                <span className="detail-value">{profile?.role}</span>
              </div>
            </div>
          )}
        </div>

        {/* Right: Security & Groups */}
        <div className="profile-right-col">

          {/* Security Card */}
          <div className="profile-card">
            <div className="card-header">
              <h2>Security</h2>
            </div>
            {!showPasswordForm ? (
              <div className="security-info">
                <div className="detail-row">
                  <span className="detail-label">Password</span>
                  <span className="detail-value">••••••••</span>
                </div>
                <button className="btn-change-pw" onClick={() => setShowPasswordForm(true)}>
                  🔒 Change Password
                </button>
              </div>
            ) : (
              <form onSubmit={handleChangePassword} className="profile-form">
                <div className="form-group">
                  <label htmlFor="current-pw">Current Password</label>
                  <input
                    id="current-pw"
                    type="password"
                    value={currentPassword}
                    onChange={e => setCurrentPassword(e.target.value)}
                    required
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="new-pw">New Password</label>
                  <input
                    id="new-pw"
                    type="password"
                    value={newPassword}
                    onChange={e => setNewPassword(e.target.value)}
                    placeholder="Min 8 characters"
                    required
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="confirm-pw">Confirm New Password</label>
                  <input
                    id="confirm-pw"
                    type="password"
                    value={confirmPassword}
                    onChange={e => setConfirmPassword(e.target.value)}
                    required
                  />
                </div>
                <div className="form-actions">
                  <button type="button" className="btn-cancel" onClick={() => {
                    setShowPasswordForm(false)
                    setCurrentPassword('')
                    setNewPassword('')
                    setConfirmPassword('')
                  }}>Cancel</button>
                  <button type="submit" className="btn-save" disabled={saving}>
                    {saving ? 'Changing...' : 'Update Password'}
                  </button>
                </div>
              </form>
            )}
          </div>

          {/* Groups Summary Card */}
          <div className="profile-card">
            <div className="card-header">
              <h2>My Groups</h2>
              <span className="group-count">{groups.length}</span>
            </div>
            {groups.length > 0 ? (
              <div className="groups-mini-list">
                {groups.map(group => (
                  <div key={group.id} className="group-mini-row">
                    <div className="group-mini-icon">🏠</div>
                    <div className="group-mini-info">
                      <span className="group-mini-name">{group.name}</span>
                      <span className="group-mini-members">
                        {group.members?.length || 0} member{(group.members?.length || 0) !== 1 ? 's' : ''}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="no-groups-msg">
                <p>You're not in any group yet.</p>
                <a href="/groups" className="link-go-groups">→ Create or join a group</a>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Toast */}
      {toast && (
        <div className={`toast ${toast.type}`} key={Date.now()}>
          {toast.type === 'success' ? '✅' : '❌'} {toast.message}
        </div>
      )}
    </div>
  )
}

export default Profile
