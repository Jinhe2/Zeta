import { BrowserRouter, Routes, Route, Navigate, useParams } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import RequireAuth from './components/RequireAuth'
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
import UsersPage from './pages/admin/UsersPage'
import AdminPlaceholderPage from './pages/admin/AdminPlaceholderPage'
import CabinetsPage from './pages/admin/CabinetsPage'
import CabinetDevicesPage from './pages/admin/CabinetDevicesPage'
import DeviceProtectionLogicsPage from './pages/admin/DeviceProtectionLogicsPage'
import DeviceCognitionItemsPage from './pages/admin/DeviceCognitionItemsPage'
import ProtectionLogicConfigPage from './pages/admin/ProtectionLogicConfigPage'

function LegacyDiagramRedirect() {
  const { id } = useParams()
  return <Navigate to={`/student/modes/panorama/${id}`} replace />
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/" element={<RootRedirect />} />
          <Route path="/login" element={<LoginPage />} />

          <Route
            path="/student"
            element={
              <RequireAuth role="STUDENT">
                <StudentHomePage />
              </RequireAuth>
            }
          />

          <Route
            path="/student/modes/coach"
            element={
              <RequireAuth role="STUDENT">
                <CoachModePage />
              </RequireAuth>
            }
          />
          <Route
            path="/student/modes/coach/cabinet"
            element={
              <RequireAuth role="STUDENT">
                <CabinetCognitionPage />
              </RequireAuth>
            }
          />
          <Route
            path="/student/modes/exam"
            element={
              <RequireAuth role="STUDENT">
                <StudentPlaceholderPage title="测评模式" description="模拟测评考核，功能开发中。" />
              </RequireAuth>
            }
          />
          <Route
            path="/student/modes/panorama"
            element={
              <RequireAuth role="STUDENT">
                <PanoramaListPage />
              </RequireAuth>
            }
          />
          <Route
            path="/student/modes/panorama/:id"
            element={
              <RequireAuth role="STUDENT">
                <StudentDiagramPage />
              </RequireAuth>
            }
          />

          <Route
            path="/student/settings/password"
            element={
              <RequireAuth role="STUDENT">
                <ChangePasswordPage />
              </RequireAuth>
            }
          />

          <Route
            path="/student/profile"
            element={
              <RequireAuth role="STUDENT">
                <ProfilePage />
              </RequireAuth>
            }
          />

          <Route
            path="/student/mistakes"
            element={
              <RequireAuth role="STUDENT">
                <MistakesPage />
              </RequireAuth>
            }
          />
          <Route
            path="/student/tasks"
            element={
              <RequireAuth role="STUDENT">
                <TasksPage />
              </RequireAuth>
            }
          />
          <Route
            path="/student/resources/:type"
            element={
              <RequireAuth role="STUDENT">
                <StudentPlaceholderPage title="资源库" description="资源内容加载功能开发中。" />
              </RequireAuth>
            }
          />

          {/* 旧路由兼容 */}
          <Route path="/student/diagram/:id" element={<LegacyDiagramRedirect />} />

          <Route
            path="/teacher"
            element={
              <RequireAuth role="TEACHER">
                <TeacherPage />
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
            <Route path="cabinets" element={<CabinetsPage />} />
            <Route path="cabinets/:cabinetId/devices" element={<CabinetDevicesPage />} />
            <Route
              path="cabinets/:cabinetId/devices/:deviceId/protection-logics/:logicId/config"
              element={<ProtectionLogicConfigPage />}
            />
            <Route
              path="cabinets/:cabinetId/devices/:deviceId/cognition-items"
              element={<DeviceCognitionItemsPage />}
            />
            <Route
              path="cabinets/:cabinetId/devices/:deviceId/protection-logics"
              element={<DeviceProtectionLogicsPage />}
            />
            <Route
              path="protection-logics"
              element={
                <AdminPlaceholderPage
                  title="保护逻辑配置"
                  description="保护逻辑的增删改查与发布管理功能开发中。"
                />
              }
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
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
