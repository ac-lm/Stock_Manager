import { useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/auth-context'

const LINKS = [
  { to: '/insumos', label: 'Insumos' },
  { to: '/productos', label: 'Productos' },
  { to: '/proveedores', label: 'Proveedores' },
  { to: '/clientes', label: 'Clientes' },
  { to: '/ventas', label: 'Ventas' },
  { to: '/caja', label: 'Caja' },
]

const LINKS_ADMIN = [
  { to: '/caja/revision', label: 'Revisión de caja' },
  { to: '/costos', label: 'Costos' },
  { to: '/usuarios', label: 'Usuarios' },
  { to: '/auditoria', label: 'Auditoría' },
]

const ROLES = {
  ADMINISTRADOR: 'Administrador',
  VENDEDOR: 'Vendedor',
}

function Navbar() {
  const { usuario, logout } = useAuth()
  const navigate = useNavigate()
  const [saliendo, setSaliendo] = useState(false)

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
        {links.map((link) => (
          <NavLink
            key={link.to}
            to={link.to}
            className={({ isActive }) => (isActive ? 'app-link active' : 'app-link')}
          >
            {link.label}
          </NavLink>
        ))}
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
