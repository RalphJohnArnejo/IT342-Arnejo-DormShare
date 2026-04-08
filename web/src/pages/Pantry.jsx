import React, { useState, useEffect, useCallback } from 'react'
import {
  getAllPantryItems,
  getPantryStats,
  addPantryItem,
  updatePantryItem,
  deletePantryItem,
  searchPantryItems
} from '../services/api'
import './Pantry.css'

const CATEGORIES = [
  'Dairy', 'Meat', 'Vegetables', 'Fruits', 'Snacks',
  'Beverages', 'Condiments', 'Grains', 'Frozen', 'Cleaning', 'Other'
]

const STATUS_FILTERS = [
  { label: 'All', value: 'ALL' },
  { label: 'In Stock', value: 'IN' },
  { label: 'Low Stock', value: 'LOW' },
  { label: 'Out of Stock', value: 'OUT' }
]

function Pantry({ user }) {
  const [items, setItems] = useState([])
  const [stats, setStats] = useState({ totalItems: 0, inStock: 0, lowStock: 0, outOfStock: 0 })
  const [loading, setLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState('')
  const [activeFilter, setActiveFilter] = useState('ALL')
  const [showModal, setShowModal] = useState(false)
  const [editingItem, setEditingItem] = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [toast, setToast] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  // Form state
  const [formData, setFormData] = useState({
    itemName: '',
    category: 'Other',
    status: 'IN',
    quantity: 1
  })

  const showToast = (message, type = 'success') => {
    setToast({ message, type })
    setTimeout(() => setToast(null), 3000)
  }

  const fetchItems = useCallback(async () => {
    try {
      setLoading(true)
      const result = await getAllPantryItems()
      if (result.success) {
        setItems(result.data)
      }
    } catch (err) {
      showToast('Failed to load pantry items', 'error')
    } finally {
      setLoading(false)
    }
  }, [])

  const fetchStats = useCallback(async () => {
    try {
      const result = await getPantryStats()
      if (result.success) {
        setStats(result.data)
      }
    } catch (err) {
      // silently fail
    }
  }, [])

  useEffect(() => {
    fetchItems()
    fetchStats()
  }, [fetchItems, fetchStats])

  // Filter and search items
  const filteredItems = items.filter(item => {
    const matchesFilter = activeFilter === 'ALL' || item.status === activeFilter
    const matchesSearch = !searchQuery ||
      item.itemName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.category.toLowerCase().includes(searchQuery.toLowerCase())
    return matchesFilter && matchesSearch
  })

  const openAddModal = () => {
    setEditingItem(null)
    setFormData({ itemName: '', category: 'Other', status: 'IN', quantity: 1 })
    setShowModal(true)
  }

  const openEditModal = (item) => {
    setEditingItem(item)
    setFormData({
      itemName: item.itemName,
      category: item.category,
      status: item.status,
      quantity: item.quantity
    })
    setShowModal(true)
  }

  const closeModal = () => {
    setShowModal(false)
    setEditingItem(null)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!formData.itemName.trim()) {
      showToast('Item name is required', 'error')
      return
    }

    setSubmitting(true)
    try {
      if (editingItem) {
        const result = await updatePantryItem(editingItem.id, formData)
        if (result.success) {
          showToast(`"${formData.itemName}" updated successfully`)
        } else {
          showToast(result.error?.details || 'Update failed', 'error')
        }
      } else {
        const result = await addPantryItem(formData)
        if (result.success) {
          showToast(`"${formData.itemName}" added to pantry`)
        } else {
          showToast(result.error?.details || 'Failed to add item', 'error')
        }
      }
      closeModal()
      fetchItems()
      fetchStats()
    } catch (err) {
      showToast(err.response?.data?.error?.details || 'Something went wrong', 'error')
    } finally {
      setSubmitting(false)
    }
  }

  const handleQuickStatusUpdate = async (item, newStatus) => {
    try {
      const result = await updatePantryItem(item.id, { status: newStatus })
      if (result.success) {
        showToast(`"${item.itemName}" → ${newStatus}`)
        fetchItems()
        fetchStats()
      }
    } catch (err) {
      showToast('Failed to update status', 'error')
    }
  }

  const handleDelete = async () => {
    if (!deleteTarget) return
    try {
      const result = await deletePantryItem(deleteTarget.id)
      if (result.success) {
        showToast(`"${deleteTarget.itemName}" removed from pantry`)
        setDeleteTarget(null)
        fetchItems()
        fetchStats()
      }
    } catch (err) {
      showToast('Failed to delete item', 'error')
    }
  }

  const formatTime = (dateStr) => {
    if (!dateStr) return ''
    const date = new Date(dateStr)
    const now = new Date()
    const diff = now - date
    const mins = Math.floor(diff / 60000)
    if (mins < 1) return 'Just now'
    if (mins < 60) return `${mins}m ago`
    const hrs = Math.floor(mins / 60)
    if (hrs < 24) return `${hrs}h ago`
    const days = Math.floor(hrs / 24)
    return `${days}d ago`
  }

  const getStatusBadgeClass = (status) => {
    switch (status) {
      case 'IN': return 'in-stock'
      case 'LOW': return 'low-stock'
      case 'OUT': return 'out-of-stock'
      default: return ''
    }
  }

  const getStatusLabel = (status) => {
    switch (status) {
      case 'IN': return 'In Stock'
      case 'LOW': return 'Low Stock'
      case 'OUT': return 'Out of Stock'
      default: return status
    }
  }

  return (
    <div className="pantry-page">
      {/* Header */}
      <div className="pantry-header">
        <div>
          <h1>Shared Pantry</h1>
          <p className="pantry-header-subtitle">Track communal household items</p>
        </div>
        <button id="btn-add-pantry-item" className="btn-add-item" onClick={openAddModal}>
          <span className="btn-icon">+</span>
          Add Item
        </button>
      </div>

      {/* Stats */}
      <div className="pantry-stats">
        <div className="pantry-stat-card stat-total">
          <div className="pantry-stat-label">Total Items</div>
          <div className="pantry-stat-value">{stats.totalItems}</div>
        </div>
        <div className="pantry-stat-card stat-in">
          <div className="pantry-stat-label">In Stock</div>
          <div className="pantry-stat-value">{stats.inStock}</div>
        </div>
        <div className="pantry-stat-card stat-low">
          <div className="pantry-stat-label">Low Stock</div>
          <div className="pantry-stat-value">{stats.lowStock}</div>
        </div>
        <div className="pantry-stat-card stat-out">
          <div className="pantry-stat-label">Out of Stock</div>
          <div className="pantry-stat-value">{stats.outOfStock}</div>
        </div>
      </div>

      {/* Search & Filters */}
      <div className="pantry-controls">
        <div className="search-wrapper">
          <span className="search-icon">🔍</span>
          <input
            id="pantry-search"
            type="text"
            className="search-input"
            placeholder="Search pantry items..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
        <div className="filter-pills">
          {STATUS_FILTERS.map(f => (
            <button
              key={f.value}
              className={`filter-pill ${activeFilter === f.value ? 'active' : ''}`}
              onClick={() => setActiveFilter(f.value)}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>

      {/* Items Grid */}
      {loading ? (
        <div className="skeleton-grid">
          {[1, 2, 3, 4, 5, 6].map(i => (
            <div key={i} className="skeleton-card">
              <div className="skeleton-line title" />
              <div className="skeleton-line badge" />
              <div className="skeleton-line text" />
              <div className="skeleton-line short" />
            </div>
          ))}
        </div>
      ) : filteredItems.length === 0 ? (
        <div className="pantry-empty">
          <span className="pantry-empty-icon">🛒</span>
          <h3>{searchQuery || activeFilter !== 'ALL' ? 'No items found' : 'Your pantry is empty'}</h3>
          <p>{searchQuery || activeFilter !== 'ALL'
            ? 'Try adjusting your search or filter'
            : 'Start by adding items your dorm needs!'}</p>
          {!searchQuery && activeFilter === 'ALL' && (
            <button className="btn-add-item" onClick={openAddModal}>
              <span className="btn-icon">+</span>
              Add First Item
            </button>
          )}
        </div>
      ) : (
        <div className="pantry-grid">
          {filteredItems.map((item, index) => (
            <div
              key={item.id}
              className="pantry-card"
              style={{ animationDelay: `${index * 0.05}s` }}
            >
              <div className="pantry-card-header">
                <h3 className="pantry-card-title">{item.itemName}</h3>
                <div className="pantry-card-actions">
                  <button
                    className="btn-card-action"
                    onClick={() => openEditModal(item)}
                    title="Edit item"
                  >
                    ✏️
                  </button>
                  <button
                    className="btn-card-action delete"
                    onClick={() => setDeleteTarget(item)}
                    title="Delete item"
                  >
                    🗑️
                  </button>
                </div>
              </div>

              <div className="pantry-card-meta">
                <span className={`status-badge ${getStatusBadgeClass(item.status)}`}>
                  <span className="status-dot" />
                  {getStatusLabel(item.status)}
                </span>
                <span className={`category-badge ${item.category.toLowerCase()}`}>
                  {item.category}
                </span>
              </div>

              <div className="pantry-card-detail">
                <span className="detail-icon">📦</span>
                Quantity: {item.quantity}
              </div>

              {/* Quick Status Toggle */}
              <div className="status-toggles">
                {['IN', 'LOW', 'OUT'].map(s => (
                  <button
                    key={s}
                    className={`status-toggle-btn toggle-${s.toLowerCase()} ${item.status === s ? 'active' : ''}`}
                    onClick={() => item.status !== s && handleQuickStatusUpdate(item, s)}
                    disabled={item.status === s}
                  >
                    {s === 'IN' ? '✓ In' : s === 'LOW' ? '⚠ Low' : '✕ Out'}
                  </button>
                ))}
              </div>

              <div className="pantry-card-footer">
                <div className="pantry-card-user">
                  Updated by <span>{item.updatedByName || 'Unknown'}</span>
                </div>
                <div className="pantry-card-time">{formatTime(item.updatedAt)}</div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Add/Edit Modal */}
      {showModal && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal-content" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editingItem ? 'Edit Item' : 'Add Pantry Item'}</h2>
              <button className="btn-modal-close" onClick={closeModal}>✕</button>
            </div>

            <form className="modal-form" onSubmit={handleSubmit}>
              <div className="form-group">
                <label htmlFor="modal-item-name">Item Name *</label>
                <input
                  id="modal-item-name"
                  type="text"
                  placeholder="e.g. Milk, Rice, Eggs..."
                  value={formData.itemName}
                  onChange={e => setFormData({ ...formData, itemName: e.target.value })}
                  autoFocus
                  required
                />
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="modal-category">Category</label>
                  <select
                    id="modal-category"
                    value={formData.category}
                    onChange={e => setFormData({ ...formData, category: e.target.value })}
                  >
                    {CATEGORIES.map(c => (
                      <option key={c} value={c}>{c}</option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label htmlFor="modal-quantity">Quantity</label>
                  <input
                    id="modal-quantity"
                    type="number"
                    min="0.1"
                    step="0.1"
                    value={formData.quantity}
                    onChange={e => setFormData({ ...formData, quantity: parseFloat(e.target.value) || 1 })}
                  />
                </div>
              </div>

              <div className="form-group">
                <label>Status</label>
                <div className="status-selector">
                  <button
                    type="button"
                    className={`status-option opt-in ${formData.status === 'IN' ? 'selected' : ''}`}
                    onClick={() => setFormData({ ...formData, status: 'IN' })}
                  >
                    ✓ In Stock
                  </button>
                  <button
                    type="button"
                    className={`status-option opt-low ${formData.status === 'LOW' ? 'selected' : ''}`}
                    onClick={() => setFormData({ ...formData, status: 'LOW' })}
                  >
                    ⚠ Low
                  </button>
                  <button
                    type="button"
                    className={`status-option opt-out ${formData.status === 'OUT' ? 'selected' : ''}`}
                    onClick={() => setFormData({ ...formData, status: 'OUT' })}
                  >
                    ✕ Out
                  </button>
                </div>
              </div>

              <button
                id="btn-submit-pantry"
                type="submit"
                className="btn-submit"
                disabled={submitting}
              >
                {submitting
                  ? (editingItem ? 'Updating...' : 'Adding...')
                  : (editingItem ? 'Update Item' : 'Add to Pantry')}
              </button>
            </form>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {deleteTarget && (
        <div className="modal-overlay" onClick={() => setDeleteTarget(null)}>
          <div className="modal-content" onClick={e => e.stopPropagation()}>
            <div className="delete-confirm">
              <span className="delete-confirm-icon">⚠️</span>
              <h3>Delete "{deleteTarget.itemName}"?</h3>
              <p>This action cannot be undone. The item will be removed from the shared pantry.</p>
              <div className="delete-confirm-actions">
                <button className="btn-cancel" onClick={() => setDeleteTarget(null)}>
                  Cancel
                </button>
                <button className="btn-delete-confirm" onClick={handleDelete}>
                  Delete Item
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Toast Notification */}
      {toast && (
        <div className={`toast ${toast.type}`}>
          {toast.type === 'success' ? '✓' : '✕'} {toast.message}
        </div>
      )}
    </div>
  )
}

export default Pantry
