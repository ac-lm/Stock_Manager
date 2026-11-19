import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import Navbar from './components/Navbar'
import RutaProtegida from './components/RutaProtegida'
import { AuthProvider } from './context/AuthProvider'
import { useAuth } from './context/auth-context'
import ClientesPage from './pages/ClientesPage'
import CostosPage from './pages/CostosPage'
import InsumosPage from './pages/InsumosPage'
import LoginPage from './pages/LoginPage'
import ProductosPage from './pages/ProductosPage'
import ProveedoresPage from './pages/ProveedoresPage'
import UsuariosPage from './pages/UsuariosPage'
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
            <Route path="/proveedores" element={<ProveedoresPage />} />
            <Route path="/clientes" element={<ClientesPage />} />
            <Route element={<RutaProtegida soloAdmin />}>
              <Route path="/costos" element={<CostosPage />} />
              <Route path="/usuarios" element={<UsuariosPage />} />
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
