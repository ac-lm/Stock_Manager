import { useEffect, useMemo, useState } from 'react'
import { EVENTO_NO_AUTORIZADO } from '../services/api'
import { authService } from '../services/authService'
import { AuthContext } from './auth-context'

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(null)
  const [cargando, setCargando] = useState(true)

  // Al cargar la aplicación se intenta restaurar la sesión con la cookie existente.
  useEffect(() => {
    let cancelado = false

    const restaurar = async () => {
      try {
        const datos = await authService.me()
        if (!cancelado) setUsuario(datos)
      } catch {
        if (!cancelado) setUsuario(null)
      } finally {
        if (!cancelado) setCargando(false)
      }
    }

    restaurar()
    return () => {
      cancelado = true
    }
  }, [])

  // Si una petición autenticada responde 401, se limpia la sesión local y las
  // rutas protegidas redirigen solas al login.
  useEffect(() => {
    const manejarNoAutorizado = () => setUsuario(null)
    window.addEventListener(EVENTO_NO_AUTORIZADO, manejarNoAutorizado)
    return () => window.removeEventListener(EVENTO_NO_AUTORIZADO, manejarNoAutorizado)
  }, [])

  const valor = useMemo(
    () => ({
      usuario,
      cargando,

      login: async (email, password) => {
        const datos = await authService.login(email, password)
        setUsuario(datos)
        return datos
      },

      logout: async () => {
        try {
          await authService.logout()
        } catch {
          // Si el backend no responde, la sesión local se cierra igual.
        } finally {
          setUsuario(null)
        }
      },

      refrescarSesion: async () => {
        const datos = await authService.me()
        setUsuario(datos)
        return datos
      },
    }),
    [usuario, cargando],
  )

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}
