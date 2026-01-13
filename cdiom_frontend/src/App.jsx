import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import Login from './pages/Login'
import Layout from './components/Layout'
import Dashboard from './pages/Dashboard'
import UserManagement from './pages/UserManagement'
import RoleManagement from './pages/RoleManagement'
import ConfigManagement from './pages/ConfigManagement'
import NoticeManagement from './pages/NoticeManagement'
import OperationLog from './pages/OperationLog'
import LoginLog from './pages/LoginLog'
import DrugManagement from './pages/DrugManagement'
import InventoryManagement from './pages/InventoryManagement'
import InboundManagement from './pages/InboundManagement'
import OutboundManagement from './pages/OutboundManagement'
import PurchaseOrderManagement from './pages/PurchaseOrderManagement'
import SupplierManagement from './pages/SupplierManagement'
import PrivateRoute from './components/PrivateRoute'
import './App.css'

function App() {
  return (
    <ConfigProvider locale={zhCN}>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route
            path="/"
            element={
              <PrivateRoute>
                <Layout />
              </PrivateRoute>
            }
          >
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="dashboard" element={<Dashboard />} />
            <Route path="drugs" element={<DrugManagement />} />
            <Route path="inventory" element={<InventoryManagement />} />
            <Route path="inbound" element={<InboundManagement />} />
            <Route path="outbound" element={<OutboundManagement />} />
            <Route path="purchase-orders" element={<PurchaseOrderManagement />} />
            <Route path="suppliers" element={<SupplierManagement />} />
            <Route path="users" element={<UserManagement />} />
            <Route path="roles" element={<RoleManagement />} />
            <Route path="configs" element={<ConfigManagement />} />
            <Route path="notices" element={<NoticeManagement />} />
            <Route path="operation-logs" element={<OperationLog />} />
            <Route path="login-logs" element={<LoginLog />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </ConfigProvider>
  )
}

export default App


