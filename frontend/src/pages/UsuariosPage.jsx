import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Modal from '../components/Modal'
import { useAuth } from '../context/auth-context'
import { usuariosService } from '../services/usuariosService'

const TAMANIO_PAGINA = 10

const ROLES = [
  { value: 'VENDEDOR', label: 'Vendedor' },
  { value: 'ADMINISTRADOR', label: 'Administrador' },
]

const etiquetaRol = (rol) => ROLES.find((r) => r.value === rol)?.label ?? rol

const FORM_VACIO = { nombre: '', email: '', rol: 'VENDEDOR', password: '' }
const PASSWORD_VACIO = { password: '', repetir: '' }

const LARGO_MAXIMO_NOMBRE = 100
const LARGO_MINIMO_PASSWORD = 8
const LARGO_MAXIMO_PASSWORD = 72
const EMAIL_VALIDO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function UsuariosPage() {
    const { usuario, logout, refrescarSesion } = useAuth()
    const navigate = useNavigate()

    // Listado
    const [usuarios, setUsuarios] = useState([])
    const [pagina, setPagina] = useState(0)
    const [totalPaginas, setTotalPaginas] = useState(0)
    const [totalElementos, setTotalElementos] = useState(0)
    const [busqueda, setBusqueda] = useState('')
    const [busquedaAplicada, setBusquedaAplicada] = useState('')
    const [incluirInactivos, setIncluirInactivos] = useState(false)
    const [recarga, setRecarga] = useState(0)
    const [cargando, setCargando] = useState(true)
    const [error, setError] = useState(null)
    const [mensaje, setMensaje] = useState(null)

    // Formularios
    const [modal, setModal] = useState(null)
    const [seleccionado, setSeleccionado] = useState(null)
    const [form, setForm] = useState(FORM_VACIO)
    const [passwordForm, setPasswordForm] = useState(PASSWORD_VACIO)
    const [guardando, setGuardando] = useState(false)
    const [errorForm, setErrorForm] = useState(null)
    const [erroresCampos, setErroresCampos] = useState({})

    // Espera 300 ms desde la última tecla antes de buscar (debounce)
    useEffect(() => {
        const timer = setTimeout(() => {
            setBusquedaAplicada(busqueda)
            setPagina(0)
        }, 300)
        return () => clearTimeout(timer)
    }, [busqueda])

    // Carga el listado: se repite si cambia la búsqueda, el filtro, la página o "recarga"
    useEffect(() => {
        let cancelado = false

        const cargar = async () => {
            setCargando(true)
            try {
                const data = await usuariosService.listar({
                    busqueda: busquedaAplicada,
                    incluirInactivos,
                    page: pagina,
                    size: TAMANIO_PAGINA,
                })
                if (!cancelado) {
                    setUsuarios(data.content)
                    setTotalPaginas(data.totalPages)
                    setTotalElementos(data.totalElements)
                    setError(null)
                }
            } catch (err) {
                if (!cancelado) setError(err.message)
            } finally {
                if (!cancelado) setCargando(false)
            }
        }

        cargar()
        return () => {
            cancelado = true
        }
    }, [busquedaAplicada, incluirInactivos, pagina, recarga])

    const cambiarFiltroInactivos = (event) => {
        setIncluirInactivos(event.target.checked)
        setPagina(0)
    }

    const cerrarModal = () => {
        setModal(null)
        setErrorForm(null)
        setErroresCampos({})
    }

    const abrirCrear = () => {
        setSeleccionado(null)
        setForm(FORM_VACIO)
        setErrorForm(null)
        setErroresCampos({})
        setError(null)
        setMensaje(null)
        setModal('crear')
    }

    const abrirEditar = (usuarioEditado) => {
        setSeleccionado(usuarioEditado)
        setForm({
            nombre: usuarioEditado.nombre ?? '',
            email: usuarioEditado.email ?? '',
            rol: usuarioEditado.rol,
            password: '',
        })
        setErrorForm(null)
        setErroresCampos({})
        setError(null)
        setMensaje(null)
        setModal('editar')
    }

    const abrirPassword = (usuarioEditado) => {
        setSeleccionado(usuarioEditado)
        setPasswordForm(PASSWORD_VACIO)
        setErrorForm(null)
        setErroresCampos({})
        setError(null)
        setMensaje(null)
        setModal('password')
    }

    const cambiarCampo = (campo) => (event) => {
        const valor = event.target.value
        setForm((prev) => ({ ...prev, [campo]: valor }))
        // Al corregir un campo se oculta el error anterior de ese campo.
        setErroresCampos((prev) => {
            if (!prev[campo]) return prev
            const resto = { ...prev }
            delete resto[campo]
            return resto
        })
    }

    const cambiarCampoPassword = (campo) => (event) => {
        setPasswordForm((prev) => ({ ...prev, [campo]: event.target.value }))
    }

    // Validación en el cliente con los mismos criterios que los DTO del backend,
    // para avisar qué campo está mal antes de enviar la petición.
    const validarFormulario = () => {
        const errores = {}
        const nombre = form.nombre.trim()
        const email = form.email.trim()
        const password = form.password

        if (!nombre) {
            errores.nombre = 'Ingresá el nombre.'
        } else if (nombre.length > LARGO_MAXIMO_NOMBRE) {
            errores.nombre = `El nombre no puede superar los ${LARGO_MAXIMO_NOMBRE} caracteres.`
        }

        if (!email) {
            errores.email = 'Ingresá el email.'
        } else if (!EMAIL_VALIDO.test(email)) {
            errores.email = 'El email no es válido. Ejemplo: nombre@dominio.com'
        } else if (email.length > 255) {
            errores.email = 'El email no puede superar los 255 caracteres.'
        }

        if (modal === 'crear') {
            if (!password) {
                errores.password = 'Ingresá una contraseña inicial.'
            } else if (
                password.length < LARGO_MINIMO_PASSWORD ||
                password.length > LARGO_MAXIMO_PASSWORD
            ) {
                errores.password =
                    `La contraseña debe tener entre ${LARGO_MINIMO_PASSWORD} y ` +
                    `${LARGO_MAXIMO_PASSWORD} caracteres.`
            }

            if (!ROLES.some((rol) => rol.value === form.rol)) {
                errores.rol = 'Seleccioná un rol.'
            }
        }

        return errores
    }

    const guardar = async (event) => {
        event.preventDefault()
        setErrorForm(null)

        const errores = validarFormulario()
        setErroresCampos(errores)
        if (Object.keys(errores).length > 0) {
            return
        }

        setGuardando(true)

        try {
            if (modal === 'crear') {
                await usuariosService.crear({
                    nombre: form.nombre.trim(),
                    email: form.email.trim(),
                    rol: form.rol,
                    password: form.password,
                })
                setMensaje('Usuario registrado correctamente.')
            } else {
                const emailNuevo = form.email.trim().toLowerCase()
                const esPropio = usuario?.id_usuario === seleccionado.id_usuario
                const cambioEmail = esPropio && emailNuevo !== seleccionado.email.toLowerCase()

                await usuariosService.actualizar(seleccionado.id_usuario, {
                    nombre: form.nombre.trim(),
                    email: form.email.trim(),
                    rol: form.rol,
                })

                // La sesión guarda el email con el que se inició sesión: si el
                // administrador se cambia el suyo, tiene que volver a entrar.
                if (cambioEmail) {
                    await logout()
                    navigate('/login', {
                        replace: true,
                        state: { aviso: 'Cambiaste tu email. Volvé a iniciar sesión con el nuevo email.' },
                    })
                    return
                }

                if (esPropio && refrescarSesion) {
                    await refrescarSesion()
                }
                setMensaje('Usuario actualizado correctamente.')
            }

            setModal(null)
            setRecarga((n) => n + 1)
        } catch (err) {
            // Si el backend devolvió errores por campo, se marcan en el formulario.
            if (err.campos && Object.keys(err.campos).length > 0) {
                setErroresCampos(err.campos)
                setErrorForm('Revisá los datos marcados en el formulario.')
            } else {
                setErrorForm(err.message)
            }
        } finally {
            setGuardando(false)
        }
    }

    const guardarPassword = async (event) => {
        event.preventDefault()

        if (passwordForm.password !== passwordForm.repetir) {
            setErrorForm('Las contraseñas no coinciden.')
            return
        }

        setGuardando(true)
        setErrorForm(null)

        try {
            await usuariosService.cambiarPassword(seleccionado.id_usuario, passwordForm.password)
            setMensaje('Contraseña actualizada correctamente.')
            setModal(null)
        } catch (err) {
            setErrorForm(err.message)
        } finally {
            setGuardando(false)
        }
    }

    const cambiarEstado = async (usuarioEditado) => {
        if (
            usuarioEditado.activo &&
            !window.confirm(
                `¿Desactivar a "${usuarioEditado.nombre}"? Dejará de poder iniciar sesión, pero conserva su historial.`,
            )
        ) {
            return
        }

        setError(null)
        setMensaje(null)
        try {
            await usuariosService.cambiarEstado(usuarioEditado.id_usuario, !usuarioEditado.activo)
            setMensaje(usuarioEditado.activo ? 'Usuario desactivado.' : 'Usuario reactivado.')
            setRecarga((n) => n + 1)
        } catch (err) {
            setError(err.message)
        }
    }

    return (
        <section className="page">
            <header className="page-header">
                <div>
                    <h1>Usuarios</h1>
                    <p className="page-subtitle">Altas, roles, contraseñas y estado de las cuentas.</p>
                </div>
                <button type="button" className="btn btn-primary" onClick={abrirCrear}>
                    Nuevo usuario
                </button>
            </header>

            {error && <div className="banner banner-error">{error}</div>}
            {mensaje && <div className="banner banner-success">{mensaje}</div>}

            <div className="toolbar">
                <input
                    type="search"
                    className="toolbar-search"
                    placeholder="Buscar por nombre o email..."
                    value={busqueda}
                    onChange={(event) => setBusqueda(event.target.value)}
                />
                <label className="check">
                    <input type="checkbox" checked={incluirInactivos} onChange={cambiarFiltroInactivos} />
                    Mostrar inactivos
                </label>
            </div>

            <div className="card">
                <div className="table-wrapper">
                    <table className="table">
                        <thead>
                        <tr>
                            <th>Nombre</th>
                            <th>Email</th>
                            <th>Rol</th>
                            <th>Estado</th>
                            <th className="table-actions">Acciones</th>
                        </tr>
                        </thead>
                        <tbody>
                        {!cargando && usuarios.length === 0 && (
                            <tr>
                                <td colSpan="5" className="empty-state">
                                    No se encontraron usuarios.
                                </td>
                            </tr>
                        )}
                        {usuarios.map((usuarioEditado) => {
                            const esPropio = usuario?.id_usuario === usuarioEditado.id_usuario
                            return (
                                <tr
                                    key={usuarioEditado.id_usuario}
                                    className={usuarioEditado.activo ? '' : 'fila-inactiva'}
                                >
                                    <td>
                                        {usuarioEditado.nombre}
                                        {esPropio ? ' (vos)' : ''}
                                    </td>
                                    <td>{usuarioEditado.email}</td>
                                    <td>{etiquetaRol(usuarioEditado.rol)}</td>
                                    <td>
                                        <span
                                            className={
                                                usuarioEditado.activo
                                                    ? 'badge badge-activo'
                                                    : 'badge badge-inactivo'
                                            }
                                        >
                                            {usuarioEditado.activo ? 'Activo' : 'Inactivo'}
                                        </span>
                                    </td>
                                    <td className="table-actions">
                                        <button
                                            type="button"
                                            className="btn btn-secondary btn-sm"
                                            onClick={() => abrirEditar(usuarioEditado)}
                                        >
                                            Editar
                                        </button>
                                        <button
                                            type="button"
                                            className="btn btn-secondary btn-sm"
                                            onClick={() => abrirPassword(usuarioEditado)}
                                        >
                                            Cambiar contraseña
                                        </button>
                                        <button
                                            type="button"
                                            className={
                                                usuarioEditado.activo
                                                    ? 'btn btn-danger btn-sm'
                                                    : 'btn btn-secondary btn-sm'
                                            }
                                            onClick={() => cambiarEstado(usuarioEditado)}
                                            disabled={esPropio && usuarioEditado.activo}
                                            title={
                                                esPropio && usuarioEditado.activo
                                                    ? 'No podés desactivar tu propia cuenta'
                                                    : undefined
                                            }
                                        >
                                            {usuarioEditado.activo ? 'Desactivar' : 'Reactivar'}
                                        </button>
                                    </td>
                                </tr>
                            )
                        })}
                        </tbody>
                    </table>
                </div>
                {cargando && <p className="loading">Cargando usuarios...</p>}
            </div>

            <div className="paginacion">
                <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    disabled={pagina === 0}
                    onClick={() => setPagina(pagina - 1)}
                >
                    Anterior
                </button>
                <span>
                    Página {pagina + 1} de {Math.max(totalPaginas, 1)} · {totalElementos} usuarios
                </span>
                <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    disabled={pagina + 1 >= totalPaginas}
                    onClick={() => setPagina(pagina + 1)}
                >
                    Siguiente
                </button>
            </div>

            {(modal === 'crear' || modal === 'editar') && (
                <Modal
                    title={modal === 'crear' ? 'Nuevo usuario' : 'Editar usuario'}
                    subtitle={
                        modal === 'editar'
                            ? `ID ${seleccionado.id_usuario}`
                            : 'Completá los datos de la cuenta.'
                    }
                    onClose={cerrarModal}
                >
                    {errorForm && <div className="banner banner-error">{errorForm}</div>}

                    <form className="form" onSubmit={guardar} noValidate>
                        <label className="field">
                            <span>Nombre *</span>
                            <input
                                type="text"
                                value={form.nombre}
                                onChange={cambiarCampo('nombre')}
                                maxLength={LARGO_MAXIMO_NOMBRE}
                                autoComplete="name"
                                aria-invalid={Boolean(erroresCampos.nombre)}
                                aria-describedby={
                                    erroresCampos.nombre ? 'error-nombre' : undefined
                                }
                                required
                                autoFocus
                            />
                            {erroresCampos.nombre && (
                                <small className="field-error" id="error-nombre">
                                    {erroresCampos.nombre}
                                </small>
                            )}
                        </label>

                        <div className="form-row">
                            <label className="field">
                                <span>Email *</span>
                                <input
                                    type="email"
                                    value={form.email}
                                    onChange={cambiarCampo('email')}
                                    maxLength="255"
                                    autoComplete="email"
                                    aria-invalid={Boolean(erroresCampos.email)}
                                    aria-describedby={
                                        erroresCampos.email ? 'error-email' : undefined
                                    }
                                    required
                                />
                                {erroresCampos.email && (
                                    <small className="field-error" id="error-email">
                                        {erroresCampos.email}
                                    </small>
                                )}
                            </label>

                            <label className="field">
                                <span>Rol *</span>
                                <select
                                    value={form.rol}
                                    onChange={cambiarCampo('rol')}
                                    aria-invalid={Boolean(erroresCampos.rol)}
                                    required
                                >
                                    {ROLES.map((rol) => (
                                        <option key={rol.value} value={rol.value}>
                                            {rol.label}
                                        </option>
                                    ))}
                                </select>
                                {erroresCampos.rol && (
                                    <small className="field-error">{erroresCampos.rol}</small>
                                )}
                            </label>
                        </div>

                        {modal === 'crear' && (
                            <label className="field">
                                <span>Contraseña inicial *</span>
                                <input
                                    type="password"
                                    value={form.password}
                                    onChange={cambiarCampo('password')}
                                    minLength={LARGO_MINIMO_PASSWORD}
                                    maxLength={LARGO_MAXIMO_PASSWORD}
                                    autoComplete="new-password"
                                    aria-invalid={Boolean(erroresCampos.password)}
                                    aria-describedby={
                                        erroresCampos.password
                                            ? 'error-password ayuda-password'
                                            : 'ayuda-password'
                                    }
                                    required
                                />
                                {erroresCampos.password ? (
                                    <small className="field-error" id="error-password">
                                        {erroresCampos.password}
                                    </small>
                                ) : (
                                    <small className="field-help" id="ayuda-password">
                                        Mínimo {LARGO_MINIMO_PASSWORD} caracteres.
                                    </small>
                                )}
                            </label>
                        )}

                        <div className="modal-footer">
                            <button type="button" className="btn btn-secondary" onClick={cerrarModal}>
                                Cancelar
                            </button>
                            <button type="submit" className="btn btn-primary" disabled={guardando}>
                                {guardando ? 'Guardando...' : 'Guardar'}
                            </button>
                        </div>
                    </form>
                </Modal>
            )}

            {modal === 'password' && (
                <Modal
                    title="Cambiar contraseña"
                    subtitle={`${seleccionado.nombre} · ${seleccionado.email}`}
                    onClose={cerrarModal}
                >
                    {errorForm && <div className="banner banner-error">{errorForm}</div>}

                    <form className="form" onSubmit={guardarPassword}>
                        <label className="field">
                            <span>Nueva contraseña *</span>
                            <input
                                type="password"
                                value={passwordForm.password}
                                onChange={cambiarCampoPassword('password')}
                                minLength="8"
                                maxLength="72"
                                autoComplete="new-password"
                                required
                                autoFocus
                            />
                            <small className="field-help">
                                Mínimo 8 caracteres y hasta 72 bytes.
                            </small>
                        </label>

                        <label className="field">
                            <span>Repetir contraseña *</span>
                            <input
                                type="password"
                                value={passwordForm.repetir}
                                onChange={cambiarCampoPassword('repetir')}
                                minLength="8"
                                maxLength="72"
                                autoComplete="new-password"
                                required
                            />
                        </label>

                        <div className="modal-footer">
                            <button type="button" className="btn btn-secondary" onClick={cerrarModal}>
                                Cancelar
                            </button>
                            <button type="submit" className="btn btn-primary" disabled={guardando}>
                                {guardando ? 'Guardando...' : 'Cambiar contraseña'}
                            </button>
                        </div>
                    </form>
                </Modal>
            )}
        </section>
    )
}

export default UsuariosPage
