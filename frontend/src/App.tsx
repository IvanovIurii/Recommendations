import { BrowserRouter, Routes, Route, NavLink } from 'react-router-dom'
import { OfflinePipelinePage } from './pages/OfflinePipelinePage'
import { OnlinePipelinePage } from './pages/OnlinePipelinePage'
import './App.css'

function App() {
  return (
    <BrowserRouter>
      <div className="app">
        <header className="app-header">
          <div className="header-content">
            <div className="logo">
              <div className="logo-icon">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M12 2L2 7l10 5 10-5-10-5z"/>
                  <path d="M2 17l10 5 10-5"/>
                  <path d="M2 12l10 5 10-5"/>
                </svg>
              </div>
              <div>
                <h1>RLAB Pipeline Monitor</h1>
                <p className="subtitle">XLM-RoBERTa Recommendation System</p>
              </div>
            </div>
            <nav className="header-nav">
              <NavLink to="/" end className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                Online Pipeline
              </NavLink>
              <NavLink to="/offline" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                Offline Training
              </NavLink>
            </nav>
          </div>
        </header>
        <main className="app-main">
          <Routes>
            <Route path="/" element={<OnlinePipelinePage />} />
            <Route path="/offline" element={<OfflinePipelinePage />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  )
}

export default App
