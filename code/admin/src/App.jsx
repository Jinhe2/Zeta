import { BrowserRouter, HashRouter, Routes, Route, Navigate, Outlet, useParams } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import RequireAuth from './components/RequireAuth'
import BindingGuard from './components/BindingGuard'
import RootRedirect from './components/RootRedirect'
import LoginPage from './pages/LoginPage'
import StudentHomePage from './pages/StudentHomePage'
import StudentDiagramPage from './pages/StudentDiagramPage'
import PanoramaListPage from './pages/student/PanoramaListPage'
import StudentPlaceholderPage from './pages/student/StudentPlaceholderPage'
import ProfilePage from './pages/student/ProfilePage'
import MistakesPage from './pages/student/MistakesPage'
import TasksPage from './pages/student/TasksPage'
import ChangePasswordPage from './pages/student/ChangePasswordPage'
import CoachModePage from './pages/student/CoachModePage'
import CabinetCognitionPage from './pages/student/CabinetCognitionPage'
import TeacherPage from './pages/TeacherPage'
import AdminLayout from './components/AdminLayout'
import UsersPage from './pages/admin/business/UsersPage'
import AdminPlaceholderPage from './pages/admin/business/AdminPlaceholderPage'
import QueueDebugPanel from './components/QueueDebugPanel'
import DisplayCabinetListPage from './pages/admin/business/display/DisplayCabinetListPage'
import CabinetDisplayItemsPage from './pages/admin/business/display/CabinetDisplayItemsPage'
import CognitionDevicesPage from './pages/admin/business/display/CognitionDevicesPage'
import DeviceDisplayItemsPage from './pages/admin/business/display/DeviceDisplayItemsPage'
import CabinetBindingPage from './pages/admin/binding/CabinetBindingPage'
import CabinetCatalogListPage from './pages/admin/screen/catalog/CabinetCatalogListPage'
import CabinetCatalogDetailPage from './pages/admin/screen/catalog/CabinetCatalogDetailPage'
import CatalogDeviceDetailPage from './pages/admin/screen/catalog/CatalogDeviceDetailPage'

function LegacyDiagramRedirect() {
  const { id } = useParams()
  return <Navigate to={`/student/modes/panorama/${id}`} replace />
}

function LegacyDeviceDisplayItemsRedirect() {
  const { deviceId } = useParams()
  return <Navigate to="/admin/display" replace state={{ legacyDeviceId: deviceId }} />
}

function LegacyCabinetDisplayItemsRedirect() {
  const { cabinetId } = useParams()
  return <Navigate to={`/admin/display/cabinets/${cabinetId}`} replace />
}

function LegacyPresentationRedirect() {
  return <Navigate to="/admin/display" replace />
}

function LegacyCognitionItemsRedirect() {
  const { cabinetId, deviceId } = useParams()
  if (deviceId) {
    return <Navigate to={`/admin/display/devices/${deviceId}/items`} replace />
  }
  if (cabinetId) {
    return <Navigate to={`/admin/display/cabinets/${cabinetId}`} replace />
  }
  return <Navigate to="/admin/display" replace />
}

function LegacyAdminCabinetsRedirect() {
  return <Navigate to="/admin/screen/cabinets" replace />
}

function LegacyCabinetDevicesRedirect() {
  const { cabinetId } = useParams()
  return <Navigate to={`/admin/screen/cabinets/${cabinetId}`} replace />
}

export default function App() {
  // Electron 使用 HashRouter（file:// 协议不支持 pushState），Web 使用 BrowserRouter
  const Router = window.electronAPI ? HashRouter : BrowserRouter

  return (
    <Router>
      <AuthProvider>
        <Routes>
          <Route path="/" element={<RootRedirect />} />
          <Route path="/login" element={<LoginPage />} />

          {/* ── Student routes (with binding check) ── */}
          <Route
            path="/student"
            element={
              <RequireAuth role="STUDENT">
                <BindingGuard>
                  <Outlet />
                </BindingGuard>
              </RequireAuth>
            }
          >
            <Route index element={<StudentHomePage />} />
            <Route path="modes/coach" element={<CoachModePage />} />
            <Route path="modes/coach/cabinet" element={<CabinetCognitionPage />} />
            <Route path="modes/exam" element={<StudentPlaceholderPage title="测评模式" description="模拟测评考核，功能开发中。" />} />
            <Route path="modes/panorama" element={<PanoramaListPage />} />
            <Route path="modes/panorama/:id" element={<StudentDiagramPage />} />
            <Route path="settings/password" element={<ChangePasswordPage />} />
            <Route path="profile" element={<ProfilePage />} />
            <Route path="mistakes" element={<MistakesPage />} />
            <Route path="tasks" element={<TasksPage />} />
            <Route path="resources/:type" element={<StudentPlaceholderPage title="资源库" description="资源内容加载功能开发中。" />} />
          </Route>
          <Route path="/student/diagram/:id" element={<LegacyDiagramRedirect />} />

          {/* ── Teacher routes (with binding check) ── */}
          <Route
            path="/teacher"
            element={
              <RequireAuth role="TEACHER">
                <BindingGuard>
                  <TeacherPage />
                </BindingGuard>
              </RequireAuth>
            }
          />

          <Route
            path="/admin"
            element={
              <RequireAuth role="ADMIN">
                <AdminLayout />
              </RequireAuth>
            }
          >
            <Route index element={<Navigate to="/admin/users/students" replace />} />
            <Route path="users" element={<Navigate to="/admin/users/students" replace />} />
            <Route path="users/:roleKey" element={<UsersPage />} />

            <Route path="display" element={<DisplayCabinetListPage />} />
            <Route path="binding" element={<CabinetBindingPage />} />
            <Route path="display/cabinets/:cabinetId" element={<CabinetDisplayItemsPage />} />
            <Route path="display/cabinet-items/:itemId/cognition-devices" element={<CognitionDevicesPage />} />
            <Route
              path="display/cognition-devices/:cognitionDeviceId/items"
              element={<DeviceDisplayItemsPage />}
            />
            <Route
              path="display/cabinets/:cabinetId/items"
              element={<LegacyCabinetDisplayItemsRedirect />}
            />
            <Route
              path="display/cabinets/:cabinetId/devices/:deviceId/items"
              element={<LegacyDeviceDisplayItemsRedirect />}
            />
            <Route
              path="display/devices/:deviceId/items"
              element={<LegacyDeviceDisplayItemsRedirect />}
            />

            <Route path="presentation" element={<LegacyPresentationRedirect />} />
            <Route path="presentation/devices" element={<LegacyPresentationRedirect />} />
            <Route
              path="presentation/cabinets/:cabinetId/cognition-items"
              element={<LegacyCognitionItemsRedirect />}
            />
            <Route
              path="presentation/devices/:deviceId/cognition-items"
              element={<LegacyCognitionItemsRedirect />}
            />
            <Route
              path="settings"
              element={
                <AdminPlaceholderPage
                  title="系统设置"
                  description="系统参数与运行配置功能开发中。"
                />
              }
            />

            <Route path="screen/cabinets" element={<CabinetCatalogListPage />} />
            <Route path="screen/cabinets/:cabinetId/devices" element={<LegacyCabinetDevicesRedirect />} />
            <Route
              path="screen/cabinets/:cabinetId/devices/:deviceId"
              element={<CatalogDeviceDetailPage />}
            />
            <Route path="screen/cabinets/:cabinetId" element={<CabinetCatalogDetailPage />} />

            <Route path="cabinets/*" element={<LegacyAdminCabinetsRedirect />} />
            <Route path="screen-legacy/*" element={<LegacyAdminCabinetsRedirect />} />
            <Route path="screen/archive/*" element={<LegacyAdminCabinetsRedirect />} />
            <Route path="protection-logics" element={<Navigate to="/admin/screen/cabinets" replace />} />
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
        <QueueDebugPanel />
      </AuthProvider>
    </Router>
  )
}
