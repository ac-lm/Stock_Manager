import { NavLink } from 'react-router-dom'

const LINKS = [
  { to: '/insumos', label: 'Insumos' },
  { to: '/productos', label: 'Productos' },
  { to: '/proveedores', label: 'Proveedores' },
]

function Navbar() {
  return (
    <header className="app-nav">
      <div className="app-brand">Stock Manager</div>
      <nav className="app-links">
        {LINKS.map((link) => (
          <NavLink
            key={link.to}
            to={link.to}
            className={({ isActive }) => (isActive ? 'app-link active' : 'app-link')}
          >
            {link.label}
          </NavLink>
        ))}
      </nav>
    </header>
  )
}

export default Navbar
