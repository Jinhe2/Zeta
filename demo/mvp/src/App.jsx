import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import RequireAuth from './components/RequireAuth'
import RootRedirect from './components/RootRedirect'
import LoginPage from './pages/LoginPage'
import AdminPage from './pages/AdminPage'
import TeacherPage from './pages/TeacherPage'
import StudentHomePage from './pages/StudentHomePage'
import StudentDiagramPage from './pages/StudentDiagramPage'
import DiagramEnginePage from './pages/DiagramEnginePage'
import DiagramEnginePageV2 from './pages/DiagramEnginePageV2'
import DiagramEnginePageV3 from './pages/DiagramEnginePageV3'
import DiagramEnginePageV4 from './pages/DiagramEnginePageV4'

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
              <RequireAuth role="student">
                <StudentHomePage />
              </RequireAuth>
            }
          />
          <Route
            path="/student/diagram/:filename"
            element={
              <RequireAuth role="student">
                <StudentDiagramPage />
              </RequireAuth>
            }
          />

          <Route
            path="/teacher"
            element={
              <RequireAuth role="teacher">
                <TeacherPage />
              </RequireAuth>
            }
          />

          <Route
            path="/admin"
            element={
              <RequireAuth role="admin">
                <AdminPage />
              </RequireAuth>
            }
          />

          {/* 引擎开发调试路由（无需登录） */}
          <Route
            path="/dev/legacy"
            element={<DiagramEnginePage key="route-legacy" engineId="legacy" />}
          />
          <Route
            path="/dev/v1"
            element={<DiagramEnginePage key="route-v1" engineId="v1" />}
          />
          <Route path="/dev/v2" element={<DiagramEnginePageV2 key="route-v2" />} />
          <Route path="/dev/v3" element={<DiagramEnginePageV3 key="route-v3" />} />
          <Route path="/dev/v4" element={<DiagramEnginePageV4 key="route-v4" />} />

          {/* 旧路由重定向 */}
          <Route path="/v1" element={<Navigate to="/dev/v1" replace />} />
          <Route path="/v2" element={<Navigate to="/dev/v2" replace />} />
          <Route path="/v3" element={<Navigate to="/dev/v3" replace />} />
          <Route path="/v4" element={<Navigate to="/dev/v4" replace />} />

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
