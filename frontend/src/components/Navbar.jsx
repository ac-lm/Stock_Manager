import { useEffect, useRef, useState } from 'react'
import { NavLink, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/auth-context'

const LINKS = [
  { to: '/insumos', label: 'Insumos' },
  { to: '/productos', label: 'Productos' },
  { stock: true },
  { to: '/proveedores', label: 'Proveedores' },
  { to: '/clientes', label: 'Clientes' },
  { to: '/ventas', label: 'Ventas' },
  { to: '/caja', label: 'Caja' },
]

const LINKS_STOCK = [
  { to: '/stock/insumos', label: 'Stock de insumos' },
  { to: '/stock/productos', label: 'Stock de productos' },
]

const LINKS_ADMIN = [
  { to: '/caja/revision', label: 'Revisión de caja' },
  { to: '/costos', label: 'Costos' },
  { to: '/usuarios', label: 'Usuarios' },
  { to: '/auditoria', label: 'Auditoría' },
  { to: '/reportes/ventas-por-periodo', label: 'Reportes' },
]

const ROLES = {
  ADMINISTRADOR: 'Administrador',
  VENDEDOR: 'Vendedor',
}

function Navbar() {
  const { usuario, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [saliendo, setSaliendo] = useState(false)
  // El submenu queda asociado a la ruta en que se abrió: al navegar se cierra solo.
  const [rutaConStockAbierto, setRutaConStockAbierto] = useState(null)
  const stockRef = useRef(null)
  const enRutaStock = location.pathname.startsWith('/stock/')
  const stockAbierto = rutaConStockAbierto === location.pathname

  // Cerrar al hacer clic fuera o con Escape.
  useEffect(() => {
    if (!stockAbierto) return undefined

    const cerrarAlClickearFuera = (event) => {
      if (stockRef.current && !stockRef.current.contains(event.target)) {
        setRutaConStockAbierto(null)
      }
    }
    const cerrarConEscape = (event) => {
      if (event.key === 'Escape') setRutaConStockAbierto(null)
    }

    document.addEventListener('mousedown', cerrarAlClickearFuera)
    document.addEventListener('keydown', cerrarConEscape)
    return () => {
      document.removeEventListener('mousedown', cerrarAlClickearFuera)
      document.removeEventListener('keydown', cerrarConEscape)
    }
  }, [stockAbierto])

  if (!usuario) return null

  const links = usuario.rol === 'ADMINISTRADOR' ? [...LINKS, ...LINKS_ADMIN] : LINKS

  const cerrarSesion = async () => {
    setSaliendo(true)
    try {
      await logout()
      navigate('/login', { replace: true })
    } finally {
      setSaliendo(false)
    }
  }

  return (
    <header className="app-nav">
      <div className="app-brand">Stock Manager</div>
      <nav className="app-links">
        {links.map((link) => {
          if (link.stock) {
            return (
              <div className="nav-dropdown" key="stock" ref={stockRef}>
                <button
                  type="button"
                  className={`app-link nav-dropdown-toggle${
                    enRutaStock || stockAbierto ? ' active' : ''
                  }`}
                  onClick={() =>
                    setRutaConStockAbierto((ruta) =>
                      ruta === location.pathname ? null : location.pathname)
                  }
                  aria-haspopup="true"
                  aria-expanded={stockAbierto}
                >
                  Stock <span className="nav-caret" aria-hidden="true">▾</span>
                </button>
                {stockAbierto && (
                  <div className="nav-submenu">
                    {LINKS_STOCK.map((sublink) => (
                      <NavLink
                        key={sublink.to}
                        to={sublink.to}
                        className={({ isActive }) =>
                          isActive ? 'nav-submenu-link active' : 'nav-submenu-link'
                        }
                        onClick={() => setRutaConStockAbierto(null)}
                      >
                        {sublink.label}
                      </NavLink>
                    ))}
                  </div>
                )}
              </div>
            )
          }

          return (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) => (isActive ? 'app-link active' : 'app-link')}
            >
              {link.label}
            </NavLink>
          )
        })}
      </nav>
      <div className="app-session">
        <div className="app-user">
          <span className="app-user-name">{usuario.nombre}</span>
          <span className="app-user-role">{ROLES[usuario.rol] ?? usuario.rol}</span>
        </div>
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          onClick={cerrarSesion}
          disabled={saliendo}
        >
          {saliendo ? 'Saliendo...' : 'Cerrar sesión'}
        </button>
      </div>
    </header>
  )
}

export default Navbar
