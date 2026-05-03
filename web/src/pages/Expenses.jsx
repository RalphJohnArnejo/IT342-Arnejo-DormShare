import React, { useState, useEffect } from 'react';
import { createWorker } from 'tesseract.js';
import { 
  getExpenseLedger, 
  logExpense, 
  getAllRoommates, 
  getExpenseSummary,
  settleSplit,
  getMyGroups
} from '../services/api';
import './Expenses.css';

const CATEGORIES = [
  'Groceries', 'Utilities', 'Rent', 'Food & Dining',
  'Transportation', 'Supplies', 'Entertainment', 'Other'
];

function Expenses() {
  const [ledger, setLedger] = useState([]);
  const [summary, setSummary] = useState({ owedToYou: 0, youOwe: 0, netBalance: 0 });
  const [roommates, setRoommates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [ocrLoading, setOcrLoading] = useState(false);
  const [toast, setToast] = useState(null);
  const [hasGroup, setHasGroup] = useState(true);
  
  // Form State
  const [description, setDescription] = useState('');
  const [amount, setAmount] = useState('');
  const [category, setCategory] = useState('Groceries');
  const [selectedRoommates, setSelectedRoommates] = useState([]);
  const [splitType, setSplitType] = useState('equal');
  const [customAmounts, setCustomAmounts] = useState({});

  // OCR Preview State
  const [receiptImage, setReceiptImage] = useState(null);
  const [extractedData, setExtractedData] = useState(null);
  const [showOcrPreview, setShowOcrPreview] = useState(false);
  const [ocrRawText, setOcrRawText] = useState('');

  const user = JSON.parse(localStorage.getItem('user') || '{}');

  useEffect(() => {
    checkGroupAndFetch();
  }, []);

  const checkGroupAndFetch = async () => {
    try {
      const groupRes = await getMyGroups();
      if (groupRes.success && groupRes.data && groupRes.data.length > 0) {
        setHasGroup(true);
        fetchData();
      } else {
        setHasGroup(false);
        setLoading(false);
      }
    } catch {
      setHasGroup(false);
      setLoading(false);
    }
  };

  // Auto-dismiss toast after 3 seconds
  useEffect(() => {
    if (toast) {
      const timer = setTimeout(() => setToast(null), 3000);
      return () => clearTimeout(timer);
    }
  }, [toast]);

  const showToast = (message, type = 'success') => {
    setToast({ message, type });
  };

  const fetchData = async () => {
    try {
      setLoading(true);
      const [ledgerRes, summaryRes, roommatesRes] = await Promise.all([
        getExpenseLedger(),
        getExpenseSummary(),
        getAllRoommates()
      ]);

      if (ledgerRes.success) setLedger(ledgerRes.data || []);
      if (summaryRes.success) setSummary(summaryRes.data || { owedToYou: 0, youOwe: 0, netBalance: 0 });
      if (roommatesRes.success) {
        // Filter out current user from roommate selector
        const others = (roommatesRes.data || []).filter(rm => rm.id !== user.userId);
        setRoommates(others);
      }
    } catch (err) {
      console.error('Failed to fetch expense data:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleFileUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    // Create image preview
    const reader = new FileReader();
    reader.onload = (event) => {
      setReceiptImage(event.target.result);
    };
    reader.readAsDataURL(file);

    setOcrLoading(true);
    try {
      const worker = await createWorker('eng');
      const { data: { text } } = await worker.recognize(file);
      await worker.terminate();

      setOcrRawText(text);

      // Simple regex to find the largest currency-like number
      const moneyRegex = /₱?\s?(\d+[\.,]\d{2})/g;
      const matches = [...text.matchAll(moneyRegex)];
      
      let detectedAmount = '';
      if (matches.length > 0) {
        const amounts = matches.map(m => parseFloat(m[1].replace(',', '.')));
        const maxAmount = Math.max(...amounts);
        detectedAmount = maxAmount.toFixed(2);
      }
      
      // Try to guess description from first line
      const lines = text.split('\n').map(l => l.trim()).filter(l => l.length > 0);
      const detectedDescription = lines.length > 0 ? lines[0] : '';

      // Detect store/merchant name
      const merchantNames = ['store', 'market', 'shop', 'mall', 'grocery', 'pharmacy'];
      let merchant = '';
      for (const line of lines) {
        if (merchantNames.some(name => line.toLowerCase().includes(name))) {
          merchant = line;
          break;
        }
      }

      const extracted = {
        amount: detectedAmount,
        description: detectedDescription,
        merchant: merchant,
        detectedAt: new Date().toLocaleTimeString(),
        confidence: 'high',
      };

      setExtractedData(extracted);
      setShowOcrPreview(true);
      showToast(`Receipt scanned! Review and confirm details.`, 'success');
      
    } catch (err) {
      console.error('OCR Error:', err);
      showToast('Failed to process receipt image.', 'error');
      setShowOcrPreview(false);
    } finally {
      setOcrLoading(false);
    }
  };

  const toggleRoommate = (id) => {
    if (selectedRoommates.includes(id)) {
      setSelectedRoommates(selectedRoommates.filter(rid => rid !== id));
      const newCustom = { ...customAmounts };
      delete newCustom[id];
      setCustomAmounts(newCustom);
    } else {
      setSelectedRoommates([...selectedRoommates, id]);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (selectedRoommates.length === 0) {
      showToast('Please select at least one roommate to split with.', 'error');
      return;
    }

    const finalSplits = [];
    const totalAmount = parseFloat(amount);
    
    if (splitType === 'equal') {
      const share = totalAmount / (selectedRoommates.length + 1); // +1 for self
      // Add self split (payer)
      finalSplits.push({ userId: user.userId, amount: share });
      // Add others
      selectedRoommates.forEach(rid => {
        finalSplits.push({ userId: rid, amount: share });
      });
    } else {
      // Custom split logic
      Object.entries(customAmounts).forEach(([rid, amt]) => {
        finalSplits.push({ userId: parseInt(rid), amount: parseFloat(amt) });
      });
      // Payer share is remainder
      const sumOthers = finalSplits.reduce((acc, s) => acc + s.amount, 0);
      finalSplits.push({ userId: user.userId, amount: totalAmount - sumOthers });
    }

    try {
      const res = await logExpense({
        amount: totalAmount,
        description,
        category,
        splits: finalSplits
      });

      if (res.success) {
        setIsModalOpen(false);
        resetForm();
        fetchData();
        showToast('Expense logged successfully! 🎉');
      } else {
        showToast(res.error?.message || 'Failed to save expense', 'error');
      }
    } catch (err) {
      showToast('Network error while saving expense', 'error');
    }
  };

  const resetForm = () => {
    setDescription('');
    setAmount('');
    setCategory('Groceries');
    setSelectedRoommates([]);
    setSplitType('equal');
    setCustomAmounts({});
    setReceiptImage(null);
    setExtractedData(null);
    setShowOcrPreview(false);
    setOcrRawText('');
  };

  const handleConfirmOcr = () => {
    if (extractedData) {
      if (extractedData.amount) setAmount(extractedData.amount);
      if (extractedData.description) setDescription(extractedData.description);
      showToast('Receipt data applied! ✅');
    }
    setShowOcrPreview(false);
  };

  const handleCancelOcr = () => {
    setReceiptImage(null);
    setExtractedData(null);
    setShowOcrPreview(false);
    setOcrRawText('');
  };

  const handleSettle = async (splitId) => {
    if (!window.confirm('Mark this split as settled?')) return;
    try {
      const res = await settleSplit(splitId);
      if (res.success) {
        fetchData();
        showToast('Split settled! ✅');
      } else {
        showToast(res.error?.details || res.error?.message || 'Failed to settle split', 'error');
      }
    } catch (err) {
      const msg = err.response?.data?.error?.details || err.response?.data?.error?.message || 'Failed to settle expense'
      showToast(msg, 'error');
    }
  };

  // Calculate equal split preview
  const getEqualShare = () => {
    if (!amount || selectedRoommates.length === 0) return 0;
    return (parseFloat(amount) / (selectedRoommates.length + 1)).toFixed(2);
  };

  if (!hasGroup) {
    return (
      <div className="expenses-page">
        <header className="expenses-header">
          <div>
            <h1>Shared Expenses</h1>
            <p className="subtitle">Track and settle roommate costs</p>
          </div>
        </header>
        <div className="empty-ledger" style={{ padding: '3rem 2rem' }}>
          <span className="empty-icon">👥</span>
          <p style={{ fontSize: '1.05rem', fontWeight: 600, color: '#e2e8f0', marginBottom: '0.5rem' }}>Join a group first</p>
          <p>You need to create or join a dorm group before splitting expenses.</p>
          <button className="btn-add-expense" onClick={() => window.location.href = '/groups'} style={{ marginTop: '1rem' }}>
            <span className="btn-icon">→</span> Go to My Groups
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="expenses-page">
      <header className="expenses-header">
        <div>
          <h1>Shared Expenses</h1>
          <p className="subtitle">Track and settle roommate costs</p>
        </div>
        <button className="btn-add-expense" onClick={() => setIsModalOpen(true)}>
          <span className="btn-icon">+</span> Log Expense
        </button>
      </header>

      {/* Summary Section */}
      <section className="summary-section">
        <div className="summary-card owed">
          <span className="label">Owed to You</span>
          <span className="value">₱{(summary.owedToYou || 0).toLocaleString()}</span>
        </div>
        <div className="summary-card owe">
          <span className="label">You Owe</span>
          <span className="value">₱{(summary.youOwe || 0).toLocaleString()}</span>
        </div>
        <div className="summary-card net">
          <span className="label">Net Balance</span>
          <span className={`value ${(summary.netBalance || 0) >= 0 ? 'positive' : 'negative'}`}>
            ₱{(summary.netBalance || 0).toLocaleString()}
          </span>
        </div>
      </section>

      {/* Ledger Section */}
      <section className="ledger-section">
        <h3>Expense Ledger</h3>
        <div className="ledger-list">
          {loading ? (
            // Loading skeleton
            [1, 2, 3].map(i => (
              <div key={i} className="skeleton-card">
                <div className="skeleton-line title" />
                <div className="skeleton-line badge" />
                <div className="skeleton-line text" />
                <div className="skeleton-line short" />
              </div>
            ))
          ) : ledger.length === 0 ? (
            <div className="empty-ledger">
              <span className="empty-icon">💸</span>
              <p>No expenses recorded yet. Click "Log Expense" to get started!</p>
            </div>
          ) : (
            ledger.map(exp => (
              <div key={exp.id} className="expense-item">
                <div className="exp-info">
                  <div className="exp-main">
                    <span className="exp-desc">{exp.description}</span>
                    <span className="exp-cat">{exp.category || 'General'}</span>
                  </div>
                  <div className="exp-meta">
                    Paid by <strong>{exp.payerName || 'Unknown'}</strong> • {new Date(exp.date).toLocaleDateString()}
                  </div>
                </div>
                <div className="exp-amount">
                  ₱{(exp.amount || 0).toLocaleString()}
                </div>
                <div className="exp-splits">
                  {(exp.splits || []).map(split => (
                    <div key={split.id} className={`split-pill ${split.isSettled ? 'settled' : 'pending'}`}>
                      {split.userName || 'User'}: ₱{(split.amountOwed || 0).toLocaleString()}
                      {split.isSettled && ' ✓'}
                      {!split.isSettled && split.userId !== user.userId && exp.paidById === user.userId && (
                        <button onClick={() => handleSettle(split.id)} className="btn-settle-mini">Settle</button>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            ))
          )}
        </div>
      </section>

      {/* Log Expense Modal */}
      {isModalOpen && (
        <div className="expenses-modal-overlay" onClick={(e) => { if (e.target === e.currentTarget) setIsModalOpen(false); }}>
          <div className="expenses-modal-content">
            <header className="expenses-modal-header">
              <h2>Log New Expense</h2>
              <button className="btn-close" onClick={() => setIsModalOpen(false)}>×</button>
            </header>

            <form onSubmit={handleSubmit} className="expense-form">
              {/* Receipt Scanner */}
              <div className="form-group scan-box">
                <label>📷 Scan Receipt (Optional)</label>
                <input type="file" accept="image/*" onChange={handleFileUpload} />
                {ocrLoading && <div className="ocr-status">Scanning receipt… ⌛</div>}
              </div>

              {/* Description & Amount */}
              <div className="form-row">
                <div className="form-group">
                  <label>Description</label>
                  <input 
                    type="text" 
                    value={description} 
                    onChange={e => setDescription(e.target.value)} 
                    placeholder="e.g. Weekly Groceries" 
                    required 
                  />
                </div>
                <div className="form-group">
                  <label>Amount (₱)</label>
                  <input 
                    type="number" 
                    step="0.01"
                    min="0.01"
                    value={amount} 
                    onChange={e => setAmount(e.target.value)} 
                    placeholder="0.00" 
                    required 
                  />
                </div>
              </div>

              {/* Category */}
              <div className="form-group">
                <label>Category</label>
                <select value={category} onChange={e => setCategory(e.target.value)}>
                  {CATEGORIES.map(cat => (
                    <option key={cat} value={cat}>{cat}</option>
                  ))}
                </select>
              </div>

              {/* Roommate Selector */}
              <div className="form-group">
                <label>Split With</label>
                <div className="roommate-selector">
                  {roommates.length === 0 ? (
                    <span style={{ color: '#718096', fontSize: '0.88rem' }}>No other users found. Register more accounts to split expenses.</span>
                  ) : (
                    roommates.map(rm => (
                      <button 
                        key={rm.id} 
                        type="button"
                        className={`rm-pill ${selectedRoommates.includes(rm.id) ? 'active' : ''}`}
                        onClick={() => toggleRoommate(rm.id)}
                      >
                        {rm.name}
                      </button>
                    ))
                  )}
                </div>
              </div>

              {/* Split Method */}
              {selectedRoommates.length > 0 && (
                <>
                  <div className="form-group">
                    <label>Split Method</label>
                    <div className="split-type-toggle">
                      <button 
                        type="button" 
                        className={splitType === 'equal' ? 'active' : ''} 
                        onClick={() => setSplitType('equal')}
                      >
                        Equal Split
                      </button>
                      <button 
                        type="button" 
                        className={splitType === 'custom' ? 'active' : ''} 
                        onClick={() => setSplitType('custom')}
                      >
                        Custom
                      </button>
                    </div>
                  </div>

                  {/* Equal Split Preview */}
                  {splitType === 'equal' && amount && (
                    <div className="custom-split-inputs" style={{ textAlign: 'center', fontWeight: 600, color: '#4a5568' }}>
                      Each person pays: <span style={{ color: '#c49a3c', fontSize: '1.1rem', fontWeight: 800 }}>₱{getEqualShare()}</span>
                      <span style={{ display: 'block', fontSize: '0.8rem', color: '#718096', marginTop: '0.25rem' }}>
                        Split between you + {selectedRoommates.length} roommate{selectedRoommates.length > 1 ? 's' : ''}
                      </span>
                    </div>
                  )}

                  {/* Custom Split */}
                  {splitType === 'custom' && (
                    <div className="custom-split-inputs">
                      {selectedRoommates.map(rid => {
                        const rm = roommates.find(r => r.id === rid);
                        return (
                          <div key={rid} className="custom-row">
                            <span>{rm?.name || 'User'} owes:</span>
                            <input 
                              type="number" 
                              step="0.01" 
                              placeholder="₱0.00"
                              value={customAmounts[rid] || ''}
                              onChange={e => setCustomAmounts({...customAmounts, [rid]: e.target.value})}
                            />
                          </div>
                        );
                      })}
                      {amount && (
                        <div className="custom-row" style={{ borderTop: '1px solid #e5e0d5', paddingTop: '0.75rem', marginTop: '0.5rem' }}>
                          <span>Your share:</span>
                          <span style={{ fontWeight: 800, color: '#c49a3c' }}>
                            ₱{(parseFloat(amount || 0) - Object.values(customAmounts).reduce((a, v) => a + parseFloat(v || 0), 0)).toFixed(2)}
                          </span>
                        </div>
                      )}
                    </div>
                  )}
                </>
              )}

              <button 
                type="submit" 
                className="btn-submit"
                disabled={!description || !amount || selectedRoommates.length === 0}
              >
                Save Expense
              </button>
            </form>
          </div>
        </div>
      )}

      {/* Toast Notification */}
      {toast && (
        <div className={`toast ${toast.type}`} key={Date.now()}>
          {toast.type === 'success' ? '✅' : '❌'} {toast.message}
        </div>
      )}

      {/* OCR Preview Modal */}
      {showOcrPreview && extractedData && receiptImage && (
        <div className="ocr-preview-overlay" onClick={handleCancelOcr}>
          <div className="ocr-preview-modal" onClick={(e) => e.stopPropagation()}>
            <header className="ocr-preview-header">
              <h2>📸 Receipt Preview</h2>
              <button className="btn-close" onClick={handleCancelOcr}>×</button>
            </header>

            <div className="ocr-preview-content">
              <div className="receipt-image-section">
                <img src={receiptImage} alt="Receipt" className="receipt-preview-img" />
                <p className="image-info">Detected at {extractedData.detectedAt}</p>
              </div>

              <div className="extracted-data-section">
                <h3>📊 Extracted Data</h3>
                
                <div className="extracted-item">
                  <label>Amount:</label>
                  <div className="extracted-display">
                    <span className="value-text">₱{extractedData.amount || 'Not detected'}</span>
                    <input
                      type="number"
                      step="0.01"
                      value={extractedData.amount}
                      onChange={(e) => setExtractedData({...extractedData, amount: e.target.value})}
                      className="edit-input"
                      placeholder="0.00"
                    />
                  </div>
                </div>

                <div className="extracted-item">
                  <label>Description:</label>
                  <div className="extracted-display">
                    <span className="value-text">{extractedData.description || 'Not detected'}</span>
                    <input
                      type="text"
                      value={extractedData.description}
                      onChange={(e) => setExtractedData({...extractedData, description: e.target.value})}
                      className="edit-input"
                      placeholder="e.g. Grocery Store"
                    />
                  </div>
                </div>

                {extractedData.merchant && (
                  <div className="extracted-item">
                    <label>Store/Merchant:</label>
                    <div className="extracted-display">
                      <span className="value-text">{extractedData.merchant}</span>
                    </div>
                  </div>
                )}

                <details className="raw-text-section">
                  <summary>📄 View Raw Text</summary>
                  <div className="raw-text-box">
                    <pre>{ocrRawText}</pre>
                  </div>
                </details>
              </div>
            </div>

            <footer className="ocr-preview-footer">
              <button className="btn-cancel" onClick={handleCancelOcr}>
                ↻ Retake Photo
              </button>
              <button className="btn-confirm" onClick={handleConfirmOcr}>
                ✓ Use This Data
              </button>
            </footer>
          </div>
        </div>
      )}
    </div>
  );
}

export default Expenses;
