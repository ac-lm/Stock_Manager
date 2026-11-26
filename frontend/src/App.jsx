import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import Navbar from './components/Navbar'
import RutaProtegida from './components/RutaProtegida'
import { AuthProvider } from './context/AuthProvider'
import { useAuth } from './context/auth-context'
import AuditoriaPage from './pages/AuditoriaPage'
import ClientesPage from './pages/ClientesPage'
import CostosPage from './pages/CostosPage'
import CajaPage from './pages/CajaPage'
import CajaHistorialPage from './pages/CajaHistorialPage'
import CajaRevisionPage from './pages/CajaRevisionPage'
import InsumosPage from './pages/InsumosPage'
import LoginPage from './pages/LoginPage'
import ProductosPage from './pages/ProductosPage'
import StockInsumosPage from './pages/StockInsumosPage'
import StockProductosPage from './pages/StockProductosPage'
import ProveedoresPage from './pages/ProveedoresPage'
import ReporteVentasPorPeriodoPage from './pages/ReporteVentasPorPeriodoPage'
import UsuariosPage from './pages/UsuariosPage'
import VentasPage from './pages/VentasPage'
import './styles/App.css'

function AppShell() {
  const { usuario, cargando } = useAuth()

  if (cargando) {
    return <div className="app-loading">Verificando sesión...</div>
  }

  return (
    <>
      {usuario && <Navbar />}
      <main className="app-main">
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route element={<RutaProtegida />}>
            <Route path="/" element={<Navigate to="/insumos" replace />} />
            <Route path="/insumos" element={<InsumosPage />} />
            <Route path="/productos" element={<ProductosPage />} />
            <Route path="/stock/insumos" element={<StockInsumosPage />} />
            <Route path="/stock/productos" element={<StockProductosPage />} />
            <Route path="/proveedores" element={<ProveedoresPage />} />
            <Route path="/clientes" element={<ClientesPage />} />
            <Route path="/ventas" element={<VentasPage />} />
            <Route path="/caja" element={<CajaPage />} />
            <Route path="/caja/historial" element={<CajaHistorialPage />} />
            <Route element={<RutaProtegida soloAdmin />}>
              <Route path="/caja/revision" element={<CajaRevisionPage />} />
              <Route path="/costos" element={<CostosPage />} />
              <Route path="/usuarios" element={<UsuariosPage />} />
              <Route path="/auditoria" element={<AuditoriaPage />} />
              <Route
                path="/reportes/ventas-por-periodo"
                element={<ReporteVentasPorPeriodoPage />}
              />
            </Route>
          </Route>
          <Route path="*" element={<Navigate to="/insumos" replace />} />
        </Routes>
      </main>
    </>
  )
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppShell />
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
