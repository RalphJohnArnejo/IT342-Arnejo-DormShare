import React, { useState } from 'react'
import { runApiIntegrationTests } from '../utils/apiIntegrationTests'
import './TestRunner.css'

function TestRunner() {
  const [testResults, setTestResults] = useState(null)
  const [isRunning, setIsRunning] = useState(false)
  const [output, setOutput] = useState([])

  const handleRunTests = async () => {
    setIsRunning(true)
    setOutput([])
    const logs = []

    // Capture console logs
    const originalLog = console.log
    const originalError = console.error
    console.log = (...args) => {
      logs.push({ type: 'log', message: args.join(' ') })
      originalLog(...args)
    }
    console.error = (...args) => {
      logs.push({ type: 'error', message: args.join(' ') })
      originalError(...args)
    }

    try {
      const results = await runApiIntegrationTests()
      setTestResults(results)
      setOutput(logs)
    } catch (err) {
      logs.push({ type: 'error', message: `Test suite error: ${err.message}` })
      setOutput(logs)
    } finally {
      console.log = originalLog
      console.error = originalError
      setIsRunning(false)
    }
  }

  const getSummary = () => {
    if (!testResults) return null
    return testResults.getSummary()
  }

  const summary = getSummary()

  return (
    <div className="test-runner-container">
      <div className="test-runner-header">
        <h1>🧪 API Integration Test Runner</h1>
        <p className="test-runner-subtitle">
          Verify all API endpoints and workflows
        </p>
      </div>

      <button
        className={`btn-run-tests ${isRunning ? 'running' : ''}`}
        onClick={handleRunTests}
        disabled={isRunning}
      >
        {isRunning ? (
          <>
            <span className="spinner-mini"></span>
            Running Tests...
          </>
        ) : (
          <>
            ▶️ Run Tests
          </>
        )}
      </button>

      {summary && (
        <div className="test-summary">
          <div className="summary-card">
            <div className="summary-metric">
              <span className="metric-label">Total</span>
              <span className="metric-value">{summary.total}</span>
            </div>
          </div>
          <div className="summary-card success">
            <div className="summary-metric">
              <span className="metric-label">Passed</span>
              <span className="metric-value">{summary.passed}</span>
            </div>
          </div>
          <div className="summary-card failure">
            <div className="summary-metric">
              <span className="metric-label">Failed</span>
              <span className="metric-value">{summary.failed}</span>
            </div>
          </div>
          <div className="summary-card info">
            <div className="summary-metric">
              <span className="metric-label">Success Rate</span>
              <span className="metric-value">{summary.percentage}</span>
            </div>
          </div>
        </div>
      )}

      {output.length > 0 && (
        <div className="test-output">
          <h3>📋 Test Output</h3>
          <div className="output-box">
            {output.map((log, idx) => (
              <div key={idx} className={`output-line ${log.type}`}>
                <span className="output-type">
                  {log.type === 'error' ? '❌' : 'ℹ️'}
                </span>
                <span className="output-message">{log.message}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {summary && summary.failed > 0 && (
        <div className="test-failures">
          <h3>🔴 Failed Tests</h3>
          {summary.errors.map((err, idx) => (
            <div key={idx} className="failure-item">
              <div className="failure-name">{err.test}</div>
              <div className="failure-error">{err.error}</div>
            </div>
          ))}
        </div>
      )}

      {summary && summary.failed === 0 && summary.total > 0 && (
        <div className="test-success">
          <div className="success-icon">✅</div>
          <h3>All Tests Passed!</h3>
          <p>All {summary.total} API endpoints are working correctly.</p>
        </div>
      )}
    </div>
  )
}

export default TestRunner
