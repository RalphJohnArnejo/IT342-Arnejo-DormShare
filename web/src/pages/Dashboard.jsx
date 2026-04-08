import './Dashboard.css'

function Dashboard({ user, onLogout }) {
  return (
    <div className="dashboard-container">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <span className="brand-icon">🏠</span>
          <h2>DormShare</h2>
        </div>
        <nav className="sidebar-nav">
          <a href="#" className="nav-item active">
            <span className="nav-icon">📊</span>
            Dashboard
          </a>
          <a href="#" className="nav-item">
            <span className="nav-icon">🛒</span>
            Pantry
          </a>
          <a href="#" className="nav-item">
            <span className="nav-icon">💰</span>
            Expenses
          </a>
          <a href="#" className="nav-item">
            <span className="nav-icon">👤</span>
            Profile
          </a>
        </nav>
        <button className="btn-logout" onClick={onLogout}>
          <span className="nav-icon">🚪</span>
          Logout
        </button>
      </aside>

      <main className="main-content">
        <header className="dashboard-header">
          <div>
            <h1>Welcome back, {user?.firstName || 'User'}!</h1>
            <p className="header-subtitle">Here's your dorm overview</p>
          </div>
        </header>

        <div className="stats-grid">
          <div className="stat-card owed-to-you">
            <div className="stat-label">Owed to You</div>
            <div className="stat-value">₱0.00</div>
            <div className="stat-change positive">No pending debts</div>
          </div>
          <div className="stat-card you-owe">
            <div className="stat-label">You Owe</div>
            <div className="stat-value">₱0.00</div>
            <div className="stat-change positive">All settled up!</div>
          </div>
          <div className="stat-card net-balance">
            <div className="stat-label">Net Balance</div>
            <div className="stat-value">₱0.00</div>
            <div className="stat-change">You're all even</div>
          </div>
        </div>

        <div className="content-grid">
          <div className="card">
            <h3>Roommate Ledger</h3>
            <div className="empty-state">
              <span className="empty-icon">📋</span>
              <p>No expenses yet. Start by adding an expense!</p>
            </div>
          </div>
          <div className="card">
            <h3>Recent Updates</h3>
            <div className="empty-state">
              <span className="empty-icon">🔔</span>
              <p>No recent activity. Your pantry and expenses will appear here.</p>
            </div>
          </div>
        </div>
      </main>
    </div>
  )
}

export default Dashboard
