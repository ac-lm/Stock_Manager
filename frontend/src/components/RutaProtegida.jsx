import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../context/auth-context'

function RutaProtegida({ soloAdmin = false }) {
  const { usuario } = useAuth()
  const location = useLocation()

  if (!usuario) {
    return (
      <Navigate
        to="/login"
        replace
        state={{ from: `${location.pathname}${location.search}` }}
      />
    )
  }

  if (soloAdmin && usuario.rol !== 'ADMINISTRADOR') {
    return <Navigate to="/insumos" replace />
  }

  return <Outlet />
}

export default RutaProtegida
