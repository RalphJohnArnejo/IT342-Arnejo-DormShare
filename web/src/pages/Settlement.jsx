import React, { useState, useEffect, useRef } from 'react'
import { CreditCard, TrendingDown, TrendingUp, History, CheckCircle2, Camera, Paperclip, X } from 'lucide-react'
import { loadStripe } from '@stripe/stripe-js'
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
  const [stripeLoaded, setStripeLoaded] = useState(false)

  // Refs to hold Stripe instances between load and confirm phases
  const stripeRef = useRef(null)
  const elementsRef = useRef(null)
  const paymentIntentIdRef = useRef(null)

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
            id: debt.splitId ? debt.splitId.toString() : `${debt.fromUserId}-${debt.toUserId}`,
            payerId: debt.fromUserId,
            payerName: debt.fromUserName,
            payeeId: debt.toUserId,
            payeeName: debt.toUserName,
            amount: debt.amount,
            status: debt.status,
            createdAt: debt.createdAt || new Date().toISOString(),
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
    setStripeLoaded(false)
    setShowPaymentModal(true)
  }

  const handleStripePayment = async () => {
    if (!selectedSettlement || !paymentAmount) {
      showToast('Please select an amount', 'error')
      return
    }

    setProcessingPayment(true)
    try {
      // Step 1: Create payment intent on backend
      const intentRes = await getStripeClientSecret({
        payeeId: selectedSettlement.payeeId,
        amount: Number(paymentAmount),
        groupId: selectedGroupId,
        description: selectedSettlement.description || `Payment to ${selectedSettlement.payeeName}`,
      })
      if (!intentRes.success) {
        console.error('Payment intent creation failed:', intentRes)
        showToast('Failed to initiate payment: ' + (intentRes.error?.message || 'Unknown error'), 'error')
        setProcessingPayment(false)
        return
      }

      const { clientSecret, paymentIntentId, publicKey, mockMode } = intentRes.data

      // Validate keys
      if (!publicKey || publicKey.includes('YOUR_') || publicKey === 'pk_test_dummy_key_for_sandbox') {
        showToast('Stripe is not properly configured. Please contact support.', 'error')
        console.error('Invalid Stripe public key:', publicKey)
        setProcessingPayment(false)
        return
      }

      // Store refs for the confirm step
      paymentIntentIdRef.current = paymentIntentId

      // If in mock mode, display a mock payment form
      if (mockMode) {
        console.log('Using mock payment mode - displaying mock form')
        const formContainer = document.getElementById('stripe-payment-form')
        if (formContainer) {
          formContainer.innerHTML = `
            <div style="padding: 0;">
              <!-- Secure Link Section -->
              <div style="padding: 12px 0; display: flex; align-items: center; gap: 8px; margin-bottom: 16px;">
                <span style="color: #1a7f34; font-size: 14px;">🔒</span>
                <span style="font-size: 13px; color: #1a7f34; font-weight: 600;">Secure, fast checkout with Link</span>
                <span style="margin-left: auto; font-size: 12px; color: #666; cursor: pointer;">∨</span>
              </div>

              <!-- Card Number -->
              <div style="margin-bottom: 12px;">
                <div style="position: relative;">
                  <input type="text" placeholder="1234 1234 1234 1234" id="card-number-input"
                         style="width: 100%; padding: 12px; border: 1px solid #ddd; border-radius: 6px; font-family: monospace; font-size: 14px; box-sizing: border-box; background: white; color: black; padding-right: 120px;" 
                         onkeyup="this.value = this.value.replace(/\\s/g, '').replace(/(\\d{4})(?=\\d)/g, '$1 ').slice(0, 19);"
                         oninput="this.value = this.value.replace(/\\s/g, '').replace(/(\\d{4})(?=\\d)/g, '$1 ').slice(0, 19);" />
                  <!-- Card logos -->
                  <div style="position: absolute; right: 8px; top: 50%; transform: translateY(-50%); display: flex; gap: 3px; pointer-events: none;">
                    <!-- Visa -->
                    <svg width="32" height="20" viewBox="0 0 32 20" style="border-radius: 3px;">
                      <rect width="32" height="20" fill="#1A1F71" rx="2"/>
                      <text x="16" y="13" font-size="9" font-weight="900" fill="white" text-anchor="middle" font-family="Arial, sans-serif">VISA</text>
                      <rect x="4" y="15" width="24" height="2" fill="#FFB81C" rx="1"/>
                    </svg>
                    <!-- Mastercard -->
                    <svg width="32" height="20" viewBox="0 0 32 20" style="border-radius: 3px;">
                      <rect width="32" height="20" fill="white" rx="2"/>
                      <circle cx="11" cy="10" r="6.5" fill="#EB001B"/>
                      <circle cx="21" cy="10" r="6.5" fill="#F79E1B"/>
                    </svg>
                    <!-- American Express -->
                    <svg width="32" height="20" viewBox="0 0 32 20" style="border-radius: 3px;">
                      <rect width="32" height="20" fill="#006FCF" rx="2"/>
                      <text x="16" y="13" font-size="8" font-weight="900" fill="white" text-anchor="middle" font-family="Arial, sans-serif">AMEX</text>
                    </svg>
                    <!-- Discover -->
                    <svg width="32" height="20" viewBox="0 0 32 20" style="border-radius: 3px;">
                      <rect width="32" height="20" fill="#FF6000" rx="2"/>
                      <text x="16" y="13" font-size="7" font-weight="900" fill="white" text-anchor="middle" font-family="Arial, sans-serif">DISCOVER</text>
                    </svg>
                  </div>
                </div>
              </div>

              <!-- Expiry and CVC -->
              <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 12px;">
                <div>
                  <input type="text" placeholder="MM/YY" id="expiry-input"
                         style="width: 100%; padding: 12px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px; box-sizing: border-box; background: white; color: black;" 
                         onkeyup="this.value = this.value.replace(/\\D/g, '').replace(/(\\d{2})(?=\\d)/, '$1/').slice(0, 5);"
                         oninput="this.value = this.value.replace(/\\D/g, '').replace(/(\\d{2})(?=\\d)/, '$1/').slice(0, 5);" />
                </div>
                <div style="position: relative;">
                  <input type="text" placeholder="CVC"
                         style="width: 100%; padding: 12px; padding-right: 32px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px; box-sizing: border-box; background: white;" />
                  <!-- Card icon with 123 badge -->
                  <svg style="position: absolute; right: 8px; top: 50%; transform: translateY(-50%); width: 24px; height: 24px; pointer-events: none;" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="1.5">
                    <!-- Card outline -->
                    <rect x="2" y="4" width="20" height="16" rx="2" ry="2" stroke="#999" fill="none"/>
                    <!-- Card stripe -->
                    <line x1="2" y1="9" x2="22" y2="9" stroke="#999"/>
                    <!-- 123 badge at bottom right -->
                    <circle cx="18" cy="16" r="5" fill="#f5f5f5" stroke="#ccc" stroke-width="1"/>
                    <text x="18" y="17.5" font-size="8" font-weight="bold" fill="#666" text-anchor="middle" font-family="Arial">123</text>
                  </svg>
                </div>
              </div>

              <!-- Country -->
              <div style="margin-bottom: 16px;">
                <select style="width: 100%; padding: 12px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px; box-sizing: border-box; background: white; cursor: pointer;">
                  <option>Philippines</option>
                  <option>United States</option>
                  <option>Canada</option>
                  <option>United Kingdom</option>
                  <option>Singapore</option>
                  <option>Thailand</option>
                </select>
              </div>
            </div>
          `
        }
        stripeRef.current = { _mockMode: true, confirmPayment: () => ({ error: null }) }
        elementsRef.current = true
        setStripeLoaded(true)
        setProcessingPayment(false)
        return
      }

      // Step 2: Load real Stripe for production
      const stripe = await loadStripe(publicKey)
      
      if (!stripe) {
        console.error('Failed to load Stripe with public key:', publicKey)
        showToast('Failed to load payment processor', 'error')
        setProcessingPayment(false)
        return
      }

      const elements = stripe.elements({ clientSecret })
      const paymentElement = elements.create('payment')

      const formContainer = document.getElementById('stripe-payment-form')
      if (!formContainer) {
        console.error('Payment form container not found in DOM')
        showToast('Payment form unavailable', 'error')
        setProcessingPayment(false)
        return
      }

      formContainer.innerHTML = ''
      paymentElement.mount(formContainer)

      // Store refs for the confirm step
      stripeRef.current = stripe
      elementsRef.current = elements

      // Form is loaded — enable the confirm button
      setStripeLoaded(true)
      setProcessingPayment(false)
    } catch (err) {
      console.error('Stripe payment error:', err)
      showToast('Payment processing failed: ' + (err.message || 'Unknown error'), 'error')
      setProcessingPayment(false)
    }
  }

  const handleStripeConfirm = async () => {
    if (!stripeRef.current || !elementsRef.current) return

    setProcessingPayment(true)
    try {
      // Check if we're in mock mode
      const isMockMode = stripeRef.current._mockMode

      if (isMockMode) {
        console.log('Using mock payment confirmation')
        // In mock mode, skip Stripe confirmation and go directly to backend
      } else {
        // Real Stripe confirmation
        const { error } = await stripeRef.current.confirmPayment({
          elements: elementsRef.current,
          confirmParams: {
            return_url: `${window.location.origin}/settlement?status=success`,
          },
          redirect: 'if_required',
        })

        if (error) {
          showToast(error.message, 'error')
          setProcessingPayment(false)
          return
        }
      }

      // Confirm on backend
      console.log('Confirming payment with settlementId:', selectedSettlement.id, 'paymentIntentId:', paymentIntentIdRef.current)
      const confirmRes = await confirmPayment(paymentIntentIdRef.current, selectedSettlement.id)
      console.log('Confirmation response:', confirmRes)
      
      if (confirmRes.success) {
        showToast('Payment successful!', 'success')
        setShowPaymentModal(false)
        setStripeLoaded(false)
        fetchSettlementData()
      } else {
        showToast('Payment confirmation failed: ' + (confirmRes.error?.message || 'Unknown error'), 'error')
      }
      setProcessingPayment(false)
    } catch (err) {
      console.error('Stripe confirm error:', err)
      showToast('Payment confirmation failed: ' + (err.message || 'Unknown error'), 'error')
      setProcessingPayment(false)
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
          <div className="empty-icon"><CreditCard size={48} /></div>
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
        <h1 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}><CreditCard size={28} /> Settlements</h1>
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
          <span style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}><TrendingDown size={18} /> You Owe ({pendingPayments.length})</span>
        </button>
        <button
          className={`tab ${activeTab === 'received' ? 'active' : ''}`}
          onClick={() => setActiveTab('received')}
        >
          <span style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}><TrendingUp size={18} /> You Receive ({receivedPayments.length})</span>
        </button>
        <button
          className={`tab ${activeTab === 'history' ? 'active' : ''}`}
          onClick={() => setActiveTab('history')}
        >
          <span style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}><History size={18} /> History</span>
        </button>
      </div>

      {activeTab === 'pending' && (
        <div className="settlement-section">
          <h2>Your Pending Payments</h2>
          {pendingPayments.length === 0 ? (
            <div className="empty-message">
              <p style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}><CheckCircle2 size={20} color="#4ade80" /> No pending payments. You're all set!</p>
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
                          {settlement.createdAt && !isNaN(new Date(settlement.createdAt).getTime())
                            ? `Since ${new Date(settlement.createdAt).toLocaleDateString()}`
                            : 'Outstanding'}
                        </p>
                      </div>
                    </div>
                    <div className="settlement-amount">
                      <span className="amount-value">₱{settlement.amount?.toFixed(2)}</span>
                      <span className={`status-badge status-${settlement.status?.toLowerCase()}`}>
                        {settlement.status}
                      </span>
                    </div>
                  </div>
                  <button
                    className="btn-pay"
                    onClick={() => handlePaymentClick(settlement)}
                    style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}
                  >
                    <CreditCard size={18} /> Pay Now
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
                          {settlement.createdAt && !isNaN(new Date(settlement.createdAt).getTime())
                            ? `Since ${new Date(settlement.createdAt).toLocaleDateString()}`
                            : 'Outstanding'}
                        </p>
                      </div>
                    </div>
                    <div className="settlement-amount">
                      <span className="amount-value">₱{settlement.amount?.toFixed(2)}</span>
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
                      <td>{settlement.createdAt && !isNaN(new Date(settlement.createdAt).getTime())
                            ? new Date(settlement.createdAt).toLocaleDateString()
                            : '—'}</td>
                      <td>{settlement.payerName}</td>
                      <td>{settlement.payeeName}</td>
                      <td className="amount">₱{settlement.amount?.toFixed(2)}</td>
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
              <X size={24} />
            </button>
            <h2 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}><CreditCard size={24} /> Payment Details</h2>
            <div className="payment-summary">
              <p className="summary-item">
                <span>Paying to:</span>
                <strong>{selectedSettlement.payeeName}</strong>
              </p>
              <p className="summary-item">
                <span>Amount:</span>
                <strong>₱{paymentAmount}</strong>
              </p>
            </div>

            <div className="payment-method-selector">
              <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <input
                  type="radio"
                  value="stripe"
                  checked={paymentMethod === 'stripe'}
                  onChange={(e) => setPaymentMethod(e.target.value)}
                  disabled={processingPayment}
                />
                <CreditCard size={18} /> Card Payment (Stripe)
              </label>
              <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <input
                  type="radio"
                  value="proof"
                  checked={paymentMethod === 'proof'}
                  onChange={(e) => setPaymentMethod(e.target.value)}
                  disabled={processingPayment}
                />
                <Camera size={18} /> Upload Payment Proof
              </label>
            </div>

            {paymentMethod === 'stripe' && (
              <div className="payment-form">
                <div id="stripe-payment-form"></div>
                {!stripeLoaded ? (
                  <button
                    className="btn-payment"
                    onClick={handleStripePayment}
                    disabled={processingPayment}
                  >
                    {processingPayment ? 'Loading payment form...' : 'Pay with Card'}
                  </button>
                ) : (
                  <button
                    className="btn-payment"
                    onClick={handleStripeConfirm}
                    disabled={processingPayment}
                  >
                    {processingPayment ? 'Processing...' : 'Confirm Payment'}
                  </button>
                )}
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
                  <span className="file-input-text" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    {paymentProofFile ? <><CheckCircle2 size={16} /> {paymentProofFile.name}</> : <><Paperclip size={16} /> Choose payment proof</>}
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


export default Settlement
