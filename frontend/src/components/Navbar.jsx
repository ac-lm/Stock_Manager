import { useEffect, useRef, useState } from 'react'
import { NavLink, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/auth-context'

// Configuración del menú. Un grupo con un solo enlace visible se renderiza como link directo.
// "soloAdmin" se aplica al grupo entero o a un item suelto (Revisión de caja).
const MENUS = [
  {
    id: 'inventario',
    label: 'Inventario',
    items: [
      { to: '/insumos', label: 'Insumos' },
      { to: '/productos', label: 'Productos' },
      { to: '/stock/insumos', label: 'Stock de insumos' },
      { to: '/stock/productos', label: 'Stock de productos' },
      { to: '/proveedores', label: 'Proveedores' },
    ],
  },
  {
    id: 'ventas',
    label: 'Ventas',
    items: [
      { to: '/ventas', label: 'Ventas' },
      { to: '/clientes', label: 'Clientes' },
    ],
  },
  {
    id: 'caja',
    label: 'Caja',
    items: [
      { to: '/caja', label: 'Caja' },
      { to: '/caja/revision', label: 'Revisión de caja', soloAdmin: true },
    ],
  },
  {
    id: 'costos',
    label: 'Costos',
    to: '/costos',
    soloAdmin: true,
  },
  {
    id: 'reportes',
    label: 'Reportes',
    soloAdmin: true,
    items: [
      { to: '/reportes/ventas-por-periodo', label: 'Ventas por período' },
      { to: '/reportes/ventas-por-producto', label: 'Ventas por producto' },
      { to: '/reportes/ventas-por-cliente', label: 'Ventas por cliente' },
      { to: '/reportes/ventas-por-vendedor', label: 'Ventas por vendedor' },
      { to: '/reportes/evolucion-ventas', label: 'Evolución de ventas' },
      { to: '/reportes/cierres-caja', label: 'Cierres de caja' },
    ],
  },
  {
    id: 'administracion',
    label: 'Administración',
    soloAdmin: true,
    items: [
      { to: '/usuarios', label: 'Usuarios' },
      { to: '/auditoria', label: 'Auditoría' },
    ],
  },
]

const ROLES = {
  ADMINISTRADOR: 'Administrador',
  VENDEDOR: 'Vendedor',
}

function itemsVisibles(menu, esAdmin) {
  if (menu.soloAdmin && !esAdmin) return []
  return (menu.items ?? []).filter((item) => !item.soloAdmin || esAdmin)
}

// El grupo se resalta mientras la ruta actual sea el enlace o una ruta hija (ej. /caja/historial).
function grupoActivo(items, pathname) {
  return items.some((item) => pathname === item.to || pathname.startsWith(`${item.to}/`))
}

function clasesEnlace({ isActive }) {
  return isActive ? 'app-link active' : 'app-link'
}

function GrupoDesplegable({ menu, items, pathname, abierto, onToggle, onNavigate }) {
  const activo = grupoActivo(items, pathname)

  return (
    <div className="nav-dropdown">
      <button
        type="button"
        className={`app-link nav-dropdown-toggle${activo || abierto ? ' active' : ''}`}
        onClick={onToggle}
        aria-haspopup="true"
        aria-expanded={abierto}
      >
        {menu.label} <span className="nav-caret" aria-hidden="true">▾</span>
      </button>
      {abierto && (
        <div className="nav-submenu">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                isActive ? 'nav-submenu-link active' : 'nav-submenu-link'
              }
              onClick={onNavigate}
            >
              {item.label}
            </NavLink>
          ))}
        </div>
      )}
    </div>
  )
}

function Navbar() {
  const { usuario, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [saliendo, setSaliendo] = useState(false)
  // Solo un grupo abierto a la vez. Se guarda la ruta en que se abrió: al navegar,
  // esa combinación deja de coincidir y el submenú se cierra solo.
  const [grupoAbierto, setGrupoAbierto] = useState(null)
  const navRef = useRef(null)
  const hayGrupoAbierto = grupoAbierto !== null && grupoAbierto.ruta === location.pathname

  useEffect(() => {
    if (!hayGrupoAbierto) return undefined

    const cerrarAlClickearFuera = (event) => {
      if (navRef.current && !navRef.current.contains(event.target)) {
        setGrupoAbierto(null)
      }
    }
    const cerrarConEscape = (event) => {
      if (event.key === 'Escape') setGrupoAbierto(null)
    }

    document.addEventListener('mousedown', cerrarAlClickearFuera)
    document.addEventListener('keydown', cerrarConEscape)
    return () => {
      document.removeEventListener('mousedown', cerrarAlClickearFuera)
      document.removeEventListener('keydown', cerrarConEscape)
    }
  }, [hayGrupoAbierto])

  if (!usuario) return null

  const esAdmin = usuario.rol === 'ADMINISTRADOR'
  const cerrar = () => setGrupoAbierto(null)

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
      <nav className="app-links" ref={navRef}>
        {MENUS.map((menu) => {
          if (menu.soloAdmin && !esAdmin) return null

          if (menu.to) {
            return (
              <NavLink key={menu.id} to={menu.to} className={clasesEnlace}>
                {menu.label}
              </NavLink>
            )
          }

          const items = itemsVisibles(menu, esAdmin)
          if (items.length === 0) return null

          if (items.length === 1) {
            return (
              <NavLink key={menu.id} to={items[0].to} className={clasesEnlace}>
                {items[0].label}
              </NavLink>
            )
          }

          return (
            <GrupoDesplegable
              key={menu.id}
              menu={menu}
              items={items}
              pathname={location.pathname}
              abierto={hayGrupoAbierto && grupoAbierto.id === menu.id}
              onToggle={() =>
                setGrupoAbierto((actual) =>
                  actual?.id === menu.id && actual.ruta === location.pathname
                    ? null
                    : { id: menu.id, ruta: location.pathname })
              }
              onNavigate={cerrar}
            />
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
