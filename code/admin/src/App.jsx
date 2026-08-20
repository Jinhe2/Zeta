import { BrowserRouter, HashRouter, Routes, Route, Navigate, Outlet, useParams } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import RequireAuth from './components/RequireAuth'
import BindingGuard from './components/BindingGuard'
import RootRedirect from './components/RootRedirect'
import LoginPage from './pages/LoginPage'
import StudentHomePage from './pages/StudentHomePage'
import StudentDiagramPage from './pages/StudentDiagramPage'
import PanoramaListPage from './pages/student/PanoramaListPage'
import StudentLogicGroupDetailPage from './pages/student/StudentLogicGroupDetailPage'
import StudentPlaceholderPage from './pages/student/StudentPlaceholderPage'
import ProfilePage from './pages/student/ProfilePage'
import MistakesPage from './pages/student/MistakesPage'
import TasksPage from './pages/student/TasksPage'
import ChangePasswordPage from './pages/student/ChangePasswordPage'
import CoachModePage from './pages/student/CoachModePage'
import CircuitLearningPage from './pages/student/CircuitLearningPage'
import CircuitViewerPage from './pages/student/CircuitViewerPage'
import CabinetCognitionPage from './pages/student/CabinetCognitionPage'
import DrawingLearningPage from './pages/student/DrawingLearningPage'
import TeacherPage from './pages/TeacherPage'
import TeacherBaselinePage from './pages/teacher/TeacherBaselinePage'
import TeacherLayout from './components/TeacherLayout'
import AdminLayout from './components/AdminLayout'
import AccountProfilePage from './pages/AccountProfilePage'
import UsersPage from './pages/admin/business/UsersPage'
import LearningResourcesPage from './pages/admin/business/LearningResourcesPage'
import AdminPlaceholderPage from './pages/admin/business/AdminPlaceholderPage'
import DisplayCabinetListPage from './pages/admin/business/display/DisplayCabinetListPage'
import CabinetDisplayItemsPage from './pages/admin/business/display/CabinetDisplayItemsPage'
import CognitionDevicesPage from './pages/admin/business/display/CognitionDevicesPage'
import DeviceDisplayItemsPage from './pages/admin/business/display/DeviceDisplayItemsPage'
import DrawingCabinetListPage from './pages/admin/business/drawing/DrawingCabinetListPage'
import DrawingGroupsPage from './pages/admin/business/drawing/DrawingGroupsPage'
import DrawingPagesPage from './pages/admin/business/drawing/DrawingPagesPage'
import DrawingCognitionItemsPage from './pages/admin/business/drawing/DrawingCognitionItemsPage'
import LogicLearningPage from './pages/admin/business/logic/LogicLearningPage'
import LogicLearningDevicesPage from './pages/admin/business/logic/LogicLearningDevicesPage'
import LogicLearningLogicsPage from './pages/admin/business/logic/LogicLearningLogicsPage'
import LogicLearningGroupsPage from './pages/admin/business/logic/LogicLearningGroupsPage'
import LogicLearningNodesPage from './pages/admin/business/logic/LogicLearningNodesPage'
import LogicNodeItemsPage from './pages/admin/business/logic/LogicNodeItemsPage'
import ExperimentGuidePage from './pages/admin/business/logic/ExperimentGuidePage'
import SettingListPage from './pages/admin/business/logic/SettingListPage'
import SoftPressboardListPage from './pages/admin/business/logic/SoftPressboardListPage'
import HardPressboardListPage from './pages/admin/business/logic/HardPressboardListPage'
import WiringRequirementPage from './pages/admin/business/logic/WiringRequirementPage'
import CabinetBindingPage from './pages/admin/binding/CabinetBindingPage'
import CabinetCatalogListPage from './pages/admin/screen/catalog/CabinetCatalogListPage'
import CabinetCatalogDetailPage from './pages/admin/screen/catalog/CabinetCatalogDetailPage'
import CatalogDeviceDetailPage from './pages/admin/screen/catalog/CatalogDeviceDetailPage'
import StudentResourcesPage from './pages/student/StudentResourcesPage'
import SamplingCabinetListPage from './pages/admin/business/sampling/SamplingCabinetListPage'
import SamplingItemsPage from './pages/admin/business/sampling/SamplingItemsPage'
import SamplingTestPage from './pages/student/SamplingTestPage'

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
            <Route path="modes/coach/circuit" element={<CircuitLearningPage />} />
            <Route path="modes/coach/circuit/:category/:name" element={<CircuitViewerPage />} />
            <Route path="modes/coach/sampling" element={<SamplingTestPage />} />
            <Route path="modes/coach/drawing" element={<DrawingLearningPage />} />
            <Route path="modes/coach/accident" element={<StudentPlaceholderPage title="事故处理" description="学习事故处理流程与案例分析，功能开发中。" />} />
            <Route path="modes/exam" element={<StudentPlaceholderPage title="测评模式" description="模拟测评考核，功能开发中。" />} />
            <Route path="modes/panorama" element={<PanoramaListPage />} />
            <Route path="modes/panorama/groups/:groupId" element={<StudentLogicGroupDetailPage />} />
            <Route path="modes/panorama/:id" element={<StudentDiagramPage />} />
            <Route path="settings/password" element={<ChangePasswordPage />} />
            <Route path="profile" element={<ProfilePage />} />
            <Route path="mistakes" element={<MistakesPage />} />
            <Route path="tasks" element={<TasksPage />} />
            <Route path="resources/:type" element={<StudentResourcesPage />} />
            <Route path="resources/:type/:id" element={<StudentResourcesPage />} />
          </Route>
          <Route path="/student/diagram/:id" element={<LegacyDiagramRedirect />} />

          {/* ── Teacher routes ── */}
          <Route
            path="/teacher"
            element={
              <RequireAuth role="TEACHER">
                <TeacherLayout />
              </RequireAuth>
            }
          >
            <Route index element={<Navigate to="/teacher/students" replace />} />
            <Route path="students" element={<TeacherPage />} />
            <Route path="baselines" element={<TeacherBaselinePage />} />
            <Route path="baselines/cabinets/:cabinetId" element={<TeacherBaselinePage />} />
            <Route path="baselines/devices/:deviceId" element={<TeacherBaselinePage />} />
            <Route path="baselines/devices/:deviceId/settings" element={<SettingListPage scopeType="IED_DEVICE" basePath="/teacher/baselines" apiNamespace="teacher" />} />
            <Route path="baselines/devices/:deviceId/soft-pressboards" element={<SoftPressboardListPage scopeType="IED_DEVICE" basePath="/teacher/baselines" apiNamespace="teacher" />} />
            <Route path="baselines/logics/:logicDiagramId/settings" element={<SettingListPage scopeType="LOGIC_DIAGRAM" basePath="/teacher/baselines" apiNamespace="teacher" />} />
            <Route path="baselines/logics/:logicDiagramId/soft-pressboards" element={<SoftPressboardListPage scopeType="LOGIC_DIAGRAM" basePath="/teacher/baselines" apiNamespace="teacher" />} />
            <Route path="baselines/groups/:groupId/settings" element={<SettingListPage scopeType="LOGIC_GROUP" basePath="/teacher/baselines" apiNamespace="teacher" />} />
            <Route path="baselines/groups/:groupId/soft-pressboards" element={<SoftPressboardListPage scopeType="LOGIC_GROUP" basePath="/teacher/baselines" apiNamespace="teacher" />} />
            <Route path="profile" element={<AccountProfilePage />} />
          </Route>

          <Route
            path="/admin"
            element={
              <RequireAuth role="ADMIN">
                <AdminLayout />
              </RequireAuth>
            }
          >
            <Route index element={<Navigate to="/admin/users/students" replace />} />
            <Route path="profile" element={<AccountProfilePage />} />
            <Route path="users" element={<Navigate to="/admin/users/students" replace />} />
            <Route path="users/:roleKey" element={<UsersPage />} />

            <Route path="display" element={<DisplayCabinetListPage />} />
            <Route path="drawing-learning" element={<DrawingCabinetListPage />} />
            <Route path="drawing-learning/cabinets/:cabinetId" element={<DrawingGroupsPage />} />
            <Route path="drawing-learning/groups/:groupId/pages" element={<DrawingPagesPage />} />
            <Route path="drawing-learning/pages/:pageId/items" element={<DrawingCognitionItemsPage />} />
            <Route path="logic-learning" element={<LogicLearningPage />} />
            <Route path="learning-resources" element={<LearningResourcesPage />} />
            <Route path="sampling-tests" element={<SamplingCabinetListPage />} />
            <Route path="sampling-tests/cabinets/:cabinetId" element={<SamplingItemsPage />} />
            <Route path="logic-learning/cabinets/:cabinetId/devices" element={<LogicLearningDevicesPage />} />
            <Route path="logic-learning/devices/:deviceId/logics" element={<LogicLearningLogicsPage />} />
            <Route path="logic-learning/devices/:deviceId/logic-groups" element={<LogicLearningGroupsPage />} />
            <Route path="logic-learning/devices/:deviceId/settings" element={<SettingListPage scopeType="IED_DEVICE" />} />
            <Route path="logic-learning/logics/:logicDiagramId/settings" element={<SettingListPage scopeType="LOGIC_DIAGRAM" />} />
            <Route path="logic-learning/devices/:deviceId/soft-pressboards" element={<SoftPressboardListPage scopeType="IED_DEVICE" />} />
            <Route path="logic-learning/logics/:logicDiagramId/soft-pressboards" element={<SoftPressboardListPage scopeType="LOGIC_DIAGRAM" />} />
            <Route path="logic-learning/devices/:deviceId/hard-pressboards" element={<HardPressboardListPage scopeType="IED_DEVICE" />} />
            <Route path="logic-learning/logics/:logicDiagramId/hard-pressboards" element={<HardPressboardListPage scopeType="LOGIC_DIAGRAM" />} />
            <Route path="logic-learning/logics/:logicDiagramId/wiring" element={<WiringRequirementPage />} />
            <Route path="logic-learning/groups/:groupId/settings" element={<SettingListPage scopeType="LOGIC_GROUP" />} />
            <Route path="logic-learning/groups/:groupId/soft-pressboards" element={<SoftPressboardListPage scopeType="LOGIC_GROUP" />} />
            <Route path="logic-learning/groups/:groupId/hard-pressboards" element={<HardPressboardListPage scopeType="LOGIC_GROUP" />} />
            <Route path="logic-learning/groups/:groupId/wiring" element={<WiringRequirementPage scopeType="LOGIC_GROUP" />} />
            <Route path="logic-learning/logics/:logicDiagramId/nodes" element={<LogicLearningNodesPage />} />
            <Route
              path="logic-learning/logics/:logicDiagramId/nodes/:nodeId/items"
              element={<LogicNodeItemsPage />}
            />
            <Route path="logic-learning/logics/:logicDiagramId/guide" element={<ExperimentGuidePage scopeType="LOGIC_DIAGRAM" />} />
            <Route path="logic-learning/groups/:groupId/guide" element={<ExperimentGuidePage scopeType="LOGIC_GROUP" />} />
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
      </AuthProvider>
    </Router>
  )
}
