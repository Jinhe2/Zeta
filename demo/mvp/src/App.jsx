import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import DiagramEnginePage from './pages/DiagramEnginePage'
import DiagramEnginePageV2 from './pages/DiagramEnginePageV2'
import DiagramEnginePageV3 from './pages/DiagramEnginePageV3'
import DiagramEnginePageV4 from './pages/DiagramEnginePageV4'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/"
          element={<DiagramEnginePage key="route-legacy" engineId="legacy" />}
        />
        <Route
          path="/v1"
          element={<DiagramEnginePage key="route-v1" engineId="v1" />}
        />
        <Route
          path="/v2"
          element={<DiagramEnginePageV2 key="route-v2" />}
        />
        <Route
          path="/v3"
          element={<DiagramEnginePageV3 key="route-v3" />}
        />
        <Route
          path="/v4"
          element={<DiagramEnginePageV4 key="route-v4" />}
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
