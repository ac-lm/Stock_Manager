import { useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/auth-context'

function LoginPage() {
  const { usuario, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)

  const desde = location.state?.from
  const destino = desde && desde !== '/login' ? desde : '/insumos'
  const aviso = location.state?.aviso

  if (usuario) {
    return <Navigate to={destino} replace />
  }

  const enviar = async (event) => {
    event.preventDefault()
    setEnviando(true)
    setError(null)

    try {
      await login(email.trim(), password)
      navigate(destino, { replace: true })
    } catch (err) {
      setError(err.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="login-page">
      <section className="login-card">
        <header className="login-header">
          <h1>Stock Manager</h1>
          <p>Ingresá con tu email y contraseña para usar el sistema.</p>
        </header>

        {aviso && <div className="banner banner-success">{aviso}</div>}
        {error && <div className="banner banner-error">{error}</div>}

        <form className="form" onSubmit={enviar}>
          <label className="field">
            <span>Email</span>
            <input
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              autoComplete="email"
              maxLength="255"
              required
              autoFocus
            />
          </label>

          <label className="field">
            <span>Contraseña</span>
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              autoComplete="current-password"
              required
            />
          </label>

          <button type="submit" className="btn btn-primary btn-block" disabled={enviando}>
            {enviando ? 'Ingresando...' : 'Ingresar'}
          </button>
        </form>

        {import.meta.env.DEV && (
          <p className="login-hint">
            Administrador inicial: <strong>admin@taller.local</strong> / <strong>Admin1234</strong>
          </p>
        )}
      </section>
    </div>
  )
}

export default LoginPage
