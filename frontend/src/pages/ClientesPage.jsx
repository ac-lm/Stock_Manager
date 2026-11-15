import { useEffect, useState } from 'react'
import Modal from '../components/Modal'
import { clientesService } from '../services/clientesService'

const TAMANIO_PAGINA = 10

const TIPOS = [
    { value: 'GRAN_CLIENTE', label: 'Gran cliente' },
    { value: 'CONSUMIDOR_FINAL', label: 'Consumidor final' },
]

const etiquetaTipo = (tipo) =>
    tipo === 'GENERICO' ? 'Genérico' : (TIPOS.find((t) => t.value === tipo)?.label ?? tipo)

const FORM_VACIO = {
    cuit: '',
    nombre: '',
    telefono: '',
    email: '',
    direccion: '',
    tipo_cliente: 'CONSUMIDOR_FINAL',
    observaciones: '',
    plazo_pago_dias: '',
}

function ClientesPage() {
    // Listado
    const [clientes, setClientes] = useState([])
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

    // Formulario
    const [modalAbierto, setModalAbierto] = useState(false)
    const [editando, setEditando] = useState(null)
    const [form, setForm] = useState(FORM_VACIO)
    const [guardando, setGuardando] = useState(false)
    const [errorForm, setErrorForm] = useState(null)
    const [similares, setSimilares] = useState([])

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
                const data = await clientesService.listar({
                    busqueda: busquedaAplicada,
                    incluirInactivos,
                    page: pagina,
                    size: TAMANIO_PAGINA,
                })
                if (!cancelado) {
                    setClientes(data.content)
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

    // Aviso de nombres parecidos: solo al registrar, no al editar
    useEffect(() => {
        if (!modalAbierto || editando || form.nombre.trim().length < 3) {
            return
        }

        let cancelado = false
        const timer = setTimeout(async () => {
            try {
                const data = await clientesService.similares(form.nombre)
                if (!cancelado) setSimilares(data)
            } catch {
                if (!cancelado) setSimilares([]) // el aviso es opcional: si falla, no se muestra
            }
        }, 300)

        return () => {
            cancelado = true
            clearTimeout(timer)
        }
    }, [modalAbierto, editando, form.nombre])

    const cambiarFiltroInactivos = (event) => {
        setIncluirInactivos(event.target.checked)
        setPagina(0)
    }

    const abrirCrear = () => {
        setEditando(null)
        setForm(FORM_VACIO)
        setSimilares([])
        setErrorForm(null)
        setError(null)
        setMensaje(null)
        setModalAbierto(true)
    }

    const abrirEditar = (cliente) => {
        setEditando(cliente)
        setSimilares([])
        setForm({
            cuit: cliente.cuit ?? '',
            nombre: cliente.nombre ?? '',
            telefono: cliente.telefono ?? '',
            email: cliente.email ?? '',
            direccion: cliente.direccion ?? '',
            tipo_cliente: cliente.tipo_cliente,
            observaciones: cliente.observaciones ?? '',
            plazo_pago_dias: cliente.plazo_pago_dias != null ? String(cliente.plazo_pago_dias) : '',
        })
        setErrorForm(null)
        setError(null)
        setMensaje(null)
        setModalAbierto(true)
    }

    // Desde el aviso de similares: abre el cliente existente en lugar de crear uno nuevo
    const abrirExistente = async (id_cliente) => {
        try {
            abrirEditar(await clientesService.obtener(id_cliente))
        } catch (err) {
            setErrorForm(err.message)
        }
    }

    const cambiarCampo = (campo) => (event) => {
        setForm((prev) => ({ ...prev, [campo]: event.target.value }))
    }

    const esGranCliente = form.tipo_cliente === 'GRAN_CLIENTE'

    // Si era gran cliente con plazo y ahora cambia de tipo, pierde sus condiciones comerciales
    const perderaCondiciones =
        !!editando &&
        editando.tipo_cliente === 'GRAN_CLIENTE' &&
        !!editando.plazo_pago_dias &&
        form.tipo_cliente !== 'GRAN_CLIENTE'

    const guardar = async (event) => {
        event.preventDefault()

        if (
            perderaCondiciones &&
            !window.confirm(
                'Este cliente dejará de ser gran cliente y se eliminarán sus condiciones comerciales. ¿Querés continuar?',
            )
        ) {
            return
        }

        setGuardando(true)
        setErrorForm(null)

        const payload = {
            cuit: form.cuit.trim() || null,
            nombre: form.nombre.trim(),
            telefono: form.telefono.trim() || null,
            email: form.email.trim(),
            direccion: form.direccion.trim(),
            tipo_cliente: form.tipo_cliente,
            observaciones: form.observaciones.trim() || null,
            plazo_pago_dias: esGranCliente && form.plazo_pago_dias !== '' ? Number(form.plazo_pago_dias) : null,
        }

        try {
            if (editando) {
                await clientesService.actualizar(editando.id_cliente, payload)
                setMensaje('Cliente actualizado correctamente.')
            } else {
                await clientesService.crear(payload)
                setMensaje('Cliente registrado correctamente.')
            }
            setModalAbierto(false)
            setRecarga((n) => n + 1)
        } catch (err) {
            setErrorForm(err.message)
        } finally {
            setGuardando(false)
        }
    }

    const cambiarEstado = async (cliente) => {
        if (
            cliente.activo &&
            !window.confirm(
                `¿Desactivar al cliente "${cliente.nombre}"? Dejará de aparecer al vender, pero conserva su historial.`,
            )
        ) {
            return
        }

        setError(null)
        setMensaje(null)
        try {
            await clientesService.cambiarEstado(cliente.id_cliente, !cliente.activo)
            setMensaje(cliente.activo ? 'Cliente desactivado.' : 'Cliente reactivado.')
            setRecarga((n) => n + 1)
        } catch (err) {
            setError(err.message)
        }
    }

    return (
        <section className="page">
            <header className="page-header">
                <div>
                    <h1>Clientes</h1>
                    <p className="page-subtitle">Grandes clientes y consumidores finales del taller.</p>
                </div>
                <button type="button" className="btn btn-primary" onClick={abrirCrear}>
                    Nuevo cliente
                </button>
            </header>

            {error && <div className="banner banner-error">{error}</div>}
            {mensaje && <div className="banner banner-success">{mensaje}</div>}

            <div className="toolbar">
                <input
                    type="search"
                    className="toolbar-search"
                    placeholder="Buscar por nombre o CUIT..."
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
                            <th>CUIT</th>
                            <th>Tipo</th>
                            <th>Teléfono</th>
                            <th>Email</th>
                            <th>Plazo de pago</th>
                            <th>Estado</th>
                            <th className="table-actions">Acciones</th>
                        </tr>
                        </thead>
                        <tbody>
                        {!cargando && clientes.length === 0 && (
                            <tr>
                                <td colSpan="8" className="empty-state">
                                    No se encontraron clientes.
                                </td>
                            </tr>
                        )}
                        {clientes.map((cliente) => (
                            <tr key={cliente.id_cliente} className={cliente.activo ? '' : 'fila-inactiva'}>
                                <td>{cliente.nombre}</td>
                                <td className="mono">{cliente.cuit || '—'}</td>
                                <td>{etiquetaTipo(cliente.tipo_cliente)}</td>
                                <td>{cliente.telefono || '—'}</td>
                                <td>{cliente.email || '—'}</td>
                                <td>
                                    {cliente.tipo_cliente !== 'GRAN_CLIENTE'
                                        ? '—'
                                        : cliente.plazo_pago_dias
                                            ? `${cliente.plazo_pago_dias} días`
                                            : 'Contado'}
                                </td>
                                <td>
                    <span className={cliente.activo ? 'badge badge-activo' : 'badge badge-inactivo'}>
                      {cliente.activo ? 'Activo' : 'Inactivo'}
                    </span>
                                </td>
                                <td className="table-actions">
                                    {cliente.tipo_cliente === 'GENERICO' ? (
                                        <span className="mono">Reservado</span>
                                    ) : (
                                        <>
                                            <button
                                                type="button"
                                                className="btn btn-secondary btn-sm"
                                                onClick={() => abrirEditar(cliente)}
                                            >
                                                Editar
                                            </button>
                                            <button
                                                type="button"
                                                className={cliente.activo ? 'btn btn-danger btn-sm' : 'btn btn-secondary btn-sm'}
                                                onClick={() => cambiarEstado(cliente)}
                                            >
                                                {cliente.activo ? 'Desactivar' : 'Reactivar'}
                                            </button>
                                        </>
                                    )}
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
                {cargando && <p className="loading">Cargando clientes...</p>}
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
          Página {pagina + 1} de {Math.max(totalPaginas, 1)} · {totalElementos} clientes
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

            {modalAbierto && (
                <Modal
                    title={editando ? 'Editar cliente' : 'Nuevo cliente'}
                    subtitle={editando ? `ID ${editando.id_cliente}` : 'Completá los datos del cliente.'}
                    onClose={() => setModalAbierto(false)}
                >
                    {errorForm && <div className="banner banner-error">{errorForm}</div>}

                    <form className="form" onSubmit={guardar}>
                        <label className="field">
                            <span>Nombre o razón social *</span>
                            <input
                                type="text"
                                value={form.nombre}
                                onChange={cambiarCampo('nombre')}
                                maxLength="50"
                                required
                                autoFocus
                            />
                        </label>

                        {!editando && form.nombre.trim().length >= 3 && similares.length > 0 && (
                            <div className="aviso aviso-similares" role="status">
                                <strong>Ya hay clientes con un nombre parecido:</strong>
                                <ul>
                                    {similares.map((similar) => (
                                        <li key={similar.id_cliente}>
                      <span>
                        {similar.nombre}
                          {similar.cuit ? ` · ${similar.cuit}` : ''}
                          {!similar.activo ? ' (inactivo)' : ''}
                      </span>
                                            <button
                                                type="button"
                                                className="btn btn-secondary btn-sm"
                                                onClick={() => abrirExistente(similar.id_cliente)}
                                            >
                                                Ver cliente
                                            </button>
                                        </li>
                                    ))}
                                </ul>
                                <small>Podés seguir y guardar igual si es otro cliente.</small>
                            </div>
                        )}

                        <div className="form-row">
                            <label className="field">
                                <span>Tipo de cliente *</span>
                                <select value={form.tipo_cliente} onChange={cambiarCampo('tipo_cliente')} required>
                                    {TIPOS.map((tipo) => (
                                        <option key={tipo.value} value={tipo.value}>
                                            {tipo.label}
                                        </option>
                                    ))}
                                </select>
                            </label>

                            <label className="field">
                                <span>CUIT {esGranCliente ? '*' : ''}</span>
                                <input
                                    type="text"
                                    value={form.cuit}
                                    onChange={cambiarCampo('cuit')}
                                    maxLength="13"
                                    placeholder="20-12345678-6"
                                    required={esGranCliente}
                                />
                            </label>
                        </div>

                        {perderaCondiciones && (
                            <div className="aviso" role="alert">
                                Al cambiar a {etiquetaTipo(form.tipo_cliente)}, las condiciones comerciales (plazo de
                                pago de {editando.plazo_pago_dias} días) ya no se tendrán en cuenta y se eliminarán al
                                guardar.
                            </div>
                        )}

                        {esGranCliente && (
                            <label className="field">
                                <span>Plazo de pago (días)</span>
                                <input
                                    type="number"
                                    min="0"
                                    max="365"
                                    step="1"
                                    value={form.plazo_pago_dias}
                                    onChange={cambiarCampo('plazo_pago_dias')}
                                    placeholder="Vacío = contado"
                                />
                            </label>
                        )}

                        <div className="form-row">
                            <label className="field">
                                <span>Teléfono</span>
                                <input
                                    type="text"
                                    value={form.telefono}
                                    onChange={cambiarCampo('telefono')}
                                    maxLength="30"
                                />
                            </label>

                            <label className="field">
                                <span>Email *</span>
                                <input
                                    type="email"
                                    value={form.email}
                                    onChange={cambiarCampo('email')}
                                    maxLength="150"
                                    required
                                />
                            </label>
                        </div>

                        <label className="field">
                            <span>Dirección *</span>
                            <input
                                type="text"
                                value={form.direccion}
                                onChange={cambiarCampo('direccion')}
                                maxLength="80"
                                required
                            />
                        </label>

                        <label className="field">
                            <span>Observaciones</span>
                            <input
                                type="text"
                                value={form.observaciones}
                                onChange={cambiarCampo('observaciones')}
                                maxLength="250"
                            />
                        </label>

                        <div className="modal-footer">
                            <button type="button" className="btn btn-secondary" onClick={() => setModalAbierto(false)}>
                                Cancelar
                            </button>
                            <button type="submit" className="btn btn-primary" disabled={guardando}>
                                {guardando ? 'Guardando...' : 'Guardar'}
                            </button>
                        </div>
                    </form>
                </Modal>
            )}
        </section>
    )
}

export default ClientesPage