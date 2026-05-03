import React, { useState, useEffect } from 'react'
import {
  getSettlementSummary,
  getSettlementHistory,
  initiatePayment,
  uploadPaymentProof,
  getMyGroups,
  getStripeClientSecret,
  confirmPayment,
} from '../services/api'
import './Settlement.css'

function Settlement() {
  const [settlements, setSettlements] = useState([])
  const [settlementHistory, setSettlementHistory] = useState([])
  const [groups, setGroups] = useState([])
  const [selectedGroupId, setSelectedGroupId] = useState(null)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [toast, setToast] = useState(null)
  const [activeTab, setActiveTab] = useState('pending')
  const [hasGroup, setHasGroup] = useState(true)

  // Payment modal state
  const [showPaymentModal, setShowPaymentModal] = useState(false)
  const [selectedSettlement, setSelectedSettlement] = useState(null)
  const [paymentAmount, setPaymentAmount] = useState('')
  const [paymentMethod, setPaymentMethod] = useState('stripe')
  const [paymentProofFile, setPaymentProofFile] = useState(null)
  const [processingPayment, setProcessingPayment] = useState(false)

  const user = JSON.parse(localStorage.getItem('user') || '{}')

  useEffect(() => {
    checkGroupAndFetch()
  }, [])

  useEffect(() => {
    if (selectedGroupId) {
      fetchSettlementData()
    }
  }, [selectedGroupId])

  const checkGroupAndFetch = async () => {
    try {
      const groupRes = await getMyGroups()
      if (groupRes.success && groupRes.data?.length > 0) {
        setGroups(groupRes.data)
        setSelectedGroupId(groupRes.data[0].id)
        setHasGroup(true)
      } else {
        setHasGroup(false)
        setLoading(false)
      }
    } catch (err) {
      console.error('Error fetching groups:', err)
      showToast('Failed to load groups', 'error')
      setHasGroup(false)
      setLoading(false)
    }
  }

  const fetchSettlementData = async () => {
    setLoading(true)
    try {
      const [summaryRes, historyRes] = await Promise.all([
        getSettlementSummary(selectedGroupId),
        getSettlementHistory(selectedGroupId),
      ])

      if (summaryRes.success) {
        // Handle new ledger summary format
        const summaryData = summaryRes.data
        if (summaryData && summaryData.debts && Array.isArray(summaryData.debts)) {
          // Convert debts array into settlement-like objects
          const settlements = summaryData.debts.map(debt => ({
            id: `${debt.fromUserId}-${debt.toUserId}`,
            payerId: debt.fromUserId,
            payerName: debt.fromUserName,
            payeeId: debt.toUserId,
            payeeName: debt.toUserName,
            amount: debt.amount,
            status: debt.status,
            description: `Settlement between ${debt.fromUserName} and ${debt.toUserName}`
          }))
          setSettlements(settlements)
        } else {
          // Fallback for old format
          setSettlements(summaryData || [])
        }
      }
      if (historyRes.success) {
        setSettlementHistory(historyRes.data || [])
      }
    } catch (err) {
      console.error('Error fetching settlement data:', err)
      showToast('Failed to load settlement data', 'error')
    } finally {
      setLoading(false)
    }
  }

  const showToast = (message, type = 'success') => {
    setToast({ message, type })
    setTimeout(() => setToast(null), 3000)
  }

  const handlePaymentClick = (settlement) => {
    setSelectedSettlement(settlement)
    setPaymentAmount(settlement.amount || '')
    setShowPaymentModal(true)
  }

  const handleStripePayment = async () => {
    if (!selectedSettlement || !paymentAmount) {
      showToast('Please select an amount', 'error')
      return
    }

    setProcessingPayment(true)
    try {
      // Step 1: Initiate payment and get Stripe intent
      const intentRes = await getStripeClientSecret(selectedSettlement.id)
      if (!intentRes.success) {
        showToast('Failed to initiate payment', 'error')
        setProcessingPayment(false)
        return
      }

      const { clientSecret, paymentIntentId } = intentRes.data

      // Step 2: Load Stripe and create payment element
      const stripe = await loadStripe(process.env.REACT_APP_STRIPE_PUBLIC_KEY)
      const elements = stripe.elements({ clientSecret })
      const paymentElement = elements.create('payment')

      // Show payment form in modal
      showPaymentForm(paymentElement, stripe, clientSecret, paymentIntentId)
    } catch (err) {
      console.error('Stripe payment error:', err)
      showToast('Payment processing failed', 'error')
      setProcessingPayment(false)
    }
  }

  const showPaymentForm = (paymentElement, stripe, clientSecret, paymentIntentId) => {
    const formContainer = document.getElementById('stripe-payment-form')
    if (formContainer) {
      formContainer.innerHTML = ''
      paymentElement.mount(formContainer)

      const submitBtn = document.getElementById('stripe-submit-btn')
      submitBtn.addEventListener('click', async () => {
        submitBtn.disabled = true
        const { error } = await stripe.confirmPayment({
          elements: paymentElement,
          confirmParams: {
            return_url: `${window.location.origin}/settlement?status=success`,
          },
        })

        if (error) {
          showToast(error.message, 'error')
          submitBtn.disabled = false
        } else {
          // Confirm payment on backend
          const confirmRes = await confirmPayment(paymentIntentId, selectedSettlement.id)
          if (confirmRes.success) {
            showToast('Payment successful!', 'success')
            setShowPaymentModal(false)
            fetchSettlementData()
          } else {
            showToast('Payment confirmation failed', 'error')
          }
          setProcessingPayment(false)
        }
      })
    }
  }

  const handleProofPayment = async () => {
    if (!selectedSettlement || !paymentAmount || !paymentProofFile) {
      showToast('Please fill all fields and upload proof', 'error')
      return
    }

    setProcessingPayment(true)
    try {
      const uploadRes = await uploadPaymentProof(selectedSettlement.id, paymentProofFile)
      if (uploadRes.success) {
        showToast('Payment proof uploaded. Awaiting verification.', 'success')
        setShowPaymentModal(false)
        setPaymentProofFile(null)
        fetchSettlementData()
      } else {
        showToast('Failed to upload payment proof', 'error')
      }
    } catch (err) {
      console.error('Proof upload error:', err)
      showToast('Upload failed', 'error')
    } finally {
      setProcessingPayment(false)
    }
  }

  const getPendingSettlements = () => {
    return settlements.filter(s => s.status === 'PENDING' && s.payerId === user.userId)
  }

  const getReceivedPayments = () => {
    return settlements.filter(s => s.status === 'PENDING' && s.payeeId === user.userId)
  }

  if (!hasGroup) {
    return (
      <div className="settlement-container">
        <div className="empty-state">
          <div className="empty-icon">💳</div>
          <h2>No Group Yet</h2>
          <p>Join or create a group to start managing settlements</p>
        </div>
      </div>
    )
  }

  if (loading) {
    return (
      <div className="settlement-container">
        <div className="loading">
          <div className="spinner"></div>
          <p>Loading settlements...</p>
        </div>
      </div>
    )
  }

  const pendingPayments = getPendingSettlements()
  const receivedPayments = getReceivedPayments()

  return (
    <div className="settlement-container">
      <div className="settlement-header">
        <h1>💳 Settlements</h1>
        <div className="group-selector">
          <select
            value={selectedGroupId}
            onChange={(e) => setSelectedGroupId(e.target.value)}
            className="group-dropdown"
          >
            {groups.map(group => (
              <option key={group.id} value={group.id}>{group.name}</option>
            ))}
          </select>
        </div>
      </div>

      {toast && (
        <div className={`toast toast-${toast.type}`}>
          {toast.message}
        </div>
      )}

      <div className="settlement-tabs">
        <button
          className={`tab ${activeTab === 'pending' ? 'active' : ''}`}
          onClick={() => setActiveTab('pending')}
        >
          💰 You Owe ({pendingPayments.length})
        </button>
        <button
          className={`tab ${activeTab === 'received' ? 'active' : ''}`}
          onClick={() => setActiveTab('received')}
        >
          🎁 You Receive ({receivedPayments.length})
        </button>
        <button
          className={`tab ${activeTab === 'history' ? 'active' : ''}`}
          onClick={() => setActiveTab('history')}
        >
          📜 History
        </button>
      </div>

      {activeTab === 'pending' && (
        <div className="settlement-section">
          <h2>Your Pending Payments</h2>
          {pendingPayments.length === 0 ? (
            <div className="empty-message">
              <p>✅ No pending payments. You're all set!</p>
            </div>
          ) : (
            <div className="settlement-list">
              {pendingPayments.map(settlement => (
                <div key={settlement.id} className="settlement-card">
                  <div className="settlement-info">
                    <div className="settlement-user">
                      <div className="user-avatar">
                        {settlement.payeeName?.charAt(0)?.toUpperCase() || '?'}
                      </div>
                      <div>
                        <h3>Pay {settlement.payeeName}</h3>
                        <p className="settlement-date">
                          Since {new Date(settlement.createdAt).toLocaleDateString()}
                        </p>
                      </div>
                    </div>
                    <div className="settlement-amount">
                      <span className="amount-value">${settlement.amount?.toFixed(2)}</span>
                      <span className={`status-badge status-${settlement.status?.toLowerCase()}`}>
                        {settlement.status}
                      </span>
                    </div>
                  </div>
                  <button
                    className="btn-pay"
                    onClick={() => handlePaymentClick(settlement)}
                  >
                    💳 Pay Now
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {activeTab === 'received' && (
        <div className="settlement-section">
          <h2>Payments You're Receiving</h2>
          {receivedPayments.length === 0 ? (
            <div className="empty-message">
              <p>No pending payments from others</p>
            </div>
          ) : (
            <div className="settlement-list">
              {receivedPayments.map(settlement => (
                <div key={settlement.id} className="settlement-card">
                  <div className="settlement-info">
                    <div className="settlement-user">
                      <div className="user-avatar">
                        {settlement.payerName?.charAt(0)?.toUpperCase() || '?'}
                      </div>
                      <div>
                        <h3>{settlement.payerName} owes you</h3>
                        <p className="settlement-date">
                          Since {new Date(settlement.createdAt).toLocaleDateString()}
                        </p>
                      </div>
                    </div>
                    <div className="settlement-amount">
                      <span className="amount-value">${settlement.amount?.toFixed(2)}</span>
                      <span className={`status-badge status-${settlement.status?.toLowerCase()}`}>
                        {settlement.status}
                      </span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {activeTab === 'history' && (
        <div className="settlement-section">
          <h2>Settlement History</h2>
          {settlementHistory.length === 0 ? (
            <div className="empty-message">
              <p>No settlement history yet</p>
            </div>
          ) : (
            <div className="history-table">
              <table>
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>From</th>
                    <th>To</th>
                    <th>Amount</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {settlementHistory.map(settlement => (
                    <tr key={settlement.id}>
                      <td>{new Date(settlement.createdAt).toLocaleDateString()}</td>
                      <td>{settlement.payerName}</td>
                      <td>{settlement.payeeName}</td>
                      <td className="amount">${settlement.amount?.toFixed(2)}</td>
                      <td>
                        <span className={`badge status-${settlement.status?.toLowerCase()}`}>
                          {settlement.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Payment Modal */}
      {showPaymentModal && selectedSettlement && (
        <div className="modal-overlay" onClick={() => !processingPayment && setShowPaymentModal(false)}>
          <div className="modal-content settlement-modal" onClick={(e) => e.stopPropagation()}>
            <button
              className="modal-close"
              onClick={() => setShowPaymentModal(false)}
              disabled={processingPayment}
            >
              ✕
            </button>
            <h2>💳 Payment Details</h2>
            <div className="payment-summary">
              <p className="summary-item">
                <span>Paying to:</span>
                <strong>{selectedSettlement.payeeName}</strong>
              </p>
              <p className="summary-item">
                <span>Amount:</span>
                <strong>${paymentAmount}</strong>
              </p>
            </div>

            <div className="payment-method-selector">
              <label>
                <input
                  type="radio"
                  value="stripe"
                  checked={paymentMethod === 'stripe'}
                  onChange={(e) => setPaymentMethod(e.target.value)}
                  disabled={processingPayment}
                />
                💳 Card Payment (Stripe)
              </label>
              <label>
                <input
                  type="radio"
                  value="proof"
                  checked={paymentMethod === 'proof'}
                  onChange={(e) => setPaymentMethod(e.target.value)}
                  disabled={processingPayment}
                />
                📸 Upload Payment Proof
              </label>
            </div>

            {paymentMethod === 'stripe' && (
              <div className="payment-form">
                <div id="stripe-payment-form"></div>
                <button
                  id="stripe-submit-btn"
                  className="btn-payment"
                  onClick={handleStripePayment}
                  disabled={processingPayment}
                >
                  {processingPayment ? 'Processing...' : 'Pay with Card'}
                </button>
              </div>
            )}

            {paymentMethod === 'proof' && (
              <div className="proof-upload">
                <label className="file-input-label">
                  <input
                    type="file"
                    accept="image/*,.pdf"
                    onChange={(e) => setPaymentProofFile(e.target.files?.[0])}
                    disabled={processingPayment}
                  />
                  <span className="file-input-text">
                    {paymentProofFile ? `✓ ${paymentProofFile.name}` : '📎 Choose payment proof'}
                  </span>
                </label>
                <p className="proof-hint">Upload a screenshot of your payment (e.g., bank transfer, PayPal)</p>
                <button
                  className="btn-payment"
                  onClick={handleProofPayment}
                  disabled={processingPayment || !paymentProofFile}
                >
                  {processingPayment ? 'Uploading...' : 'Upload Proof'}
                </button>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

// Placeholder for Stripe loading - will be implemented with actual Stripe key
const loadStripe = async (key) => {
  // This would load the actual Stripe library
  // For now, we'll show a message
  alert('Stripe integration requires backend payment intent setup. Using proof upload instead.')
  return null
}

export default Settlement
