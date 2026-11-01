import { Routes, Route, Navigate, Outlet } from 'react-router-dom';

import Sidebar from './components/Sidebar';
import ProtectedRoute from './components/ProtectedRoute';

import Dashboard from './pages/Dashboard';
import Subscriptions from './pages/Subscriptions';
import Transactions from './pages/Transactions';
import Login from './pages/Login';
import Register from './pages/Register';

import './styles/global.css';

function AppLayout() {
  return (
    <div className="app-container">
      <Sidebar />

      <div className="main-content">
        

        <main className="page-body">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/subscriptions" element={<Subscriptions />} />
          <Route path="/transactions" element={<Transactions />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;