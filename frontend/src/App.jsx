import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import Navbar from './components/Navbar'
import InsumosPage from './pages/InsumosPage'
import ProductosPage from './pages/ProductosPage'
import ProveedoresPage from './pages/ProveedoresPage'
import './styles/App.css'

function App() {
  return (
    <BrowserRouter>
      <Navbar />
      <main className="app-main">
        <Routes>
          <Route path="/" element={<Navigate to="/insumos" replace />} />
          <Route path="/insumos" element={<InsumosPage />} />
          <Route path="/productos" element={<ProductosPage />} />
          <Route path="/proveedores" element={<ProveedoresPage />} />
          <Route path="*" element={<Navigate to="/insumos" replace />} />
        </Routes>
      </main>
    </BrowserRouter>
  )
}

export default App
