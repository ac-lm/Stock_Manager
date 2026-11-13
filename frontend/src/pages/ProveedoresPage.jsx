import { useEffect, useState } from 'react'
import Modal from '../components/Modal'
import { proveedoresService } from '../services/proveedoresService'

const FORM_VACIO = {
  nombre: '',
  cuit: '',
  telefono: '',
  email: '',
  tipo_producto: '',
  observaciones: '',
}

function ProveedoresPage() {
  const [proveedores, setProveedores] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [mensaje, setMensaje] = useState(null)
  const [modalAbierto, setModalAbierto] = useState(false)
  const [editando, setEditando] = useState(null)
  const [form, setForm] = useState(FORM_VACIO)
  const [guardando, setGuardando] = useState(false)

  const cargar = async () => {
    setCargando(true)
    setError(null)
    try {
      setProveedores(await proveedoresService.listar())
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
  }

  useEffect(() => {
    let cancelado = false

    const cargarInicial = async () => {
      try {
        const data = await proveedoresService.listar()
        if (!cancelado) setProveedores(data)
      } catch (err) {
        if (!cancelado) setError(err.message)
      } finally {
        if (!cancelado) setCargando(false)
      }
    }

    cargarInicial()
    return () => {
      cancelado = true
    }
  }, [])

  const abrirCrear = () => {
    setEditando(null)
    setForm(FORM_VACIO)
    setError(null)
    setMensaje(null)
    setModalAbierto(true)
  }

  const abrirEditar = (proveedor) => {
    setEditando(proveedor)
    setForm({
      nombre: proveedor.nombre ?? '',
      cuit: proveedor.cuit ?? '',
      telefono: proveedor.telefono ?? '',
      email: proveedor.email ?? '',
      tipo_producto: proveedor.tipo_producto ?? '',
      observaciones: proveedor.observaciones ?? '',
    })
    setError(null)
    setMensaje(null)
    setModalAbierto(true)
  }

  const cambiarCampo = (campo) => (event) => {
    setForm((prev) => ({ ...prev, [campo]: event.target.value }))
  }

  const guardar = async (event) => {
    event.preventDefault()
    setGuardando(true)
    setError(null)

    const payload = {
      nombre: form.nombre.trim(),
      cuit: form.cuit.trim(),
      telefono: form.telefono.trim(),
      email: form.email.trim() || null,
      tipo_producto: form.tipo_producto.trim(),
      observaciones: form.observaciones.trim() || null,
    }

    try {
      if (editando) {
        await proveedoresService.actualizar(editando.id_proveedor, payload)
        setMensaje('Proveedor actualizado correctamente.')
      } else {
        await proveedoresService.crear(payload)
        setMensaje('Proveedor creado correctamente.')
      }
      setModalAbierto(false)
      await cargar()
    } catch (err) {
      setError(err.message)
    } finally {
      setGuardando(false)
    }
  }

  const eliminar = async (proveedor) => {
    if (!window.confirm(`¿Eliminar el proveedor "${proveedor.nombre}"?`)) return

    setError(null)
    try {
      await proveedoresService.eliminar(proveedor.id_proveedor)
      setMensaje('Proveedor eliminado correctamente.')
      await cargar()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Proveedores</h1>
          <p className="page-subtitle">Empresas que abastecen los insumos del taller.</p>
        </div>
        <button type="button" className="btn btn-primary" onClick={abrirCrear}>
          Nuevo proveedor
        </button>
      </header>

      {error && <div className="banner banner-error">{error}</div>}
      {mensaje && <div className="banner banner-success">{mensaje}</div>}

      <div className="card">
        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Nombre</th>
                <th>CUIT</th>
                <th>Teléfono</th>
                <th>Email</th>
                <th>Tipo de producto</th>
                <th>Observaciones</th>
                <th className="table-actions">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {!cargando && proveedores.length === 0 && (
                <tr>
                  <td colSpan="8" className="empty-state">
                    No hay proveedores cargados todavía.
                  </td>
                </tr>
              )}
              {proveedores.map((proveedor) => (
                <tr key={proveedor.id_proveedor}>
                  <td className="mono">{proveedor.id_proveedor}</td>
                  <td>{proveedor.nombre}</td>
                  <td className="mono">{proveedor.cuit}</td>
                  <td>{proveedor.telefono}</td>
                  <td>{proveedor.email || '—'}</td>
                  <td>{proveedor.tipo_producto}</td>
                  <td>{proveedor.observaciones || '—'}</td>
                  <td className="table-actions">
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      onClick={() => abrirEditar(proveedor)}
                    >
                      Editar
                    </button>
                    <button
                      type="button"
                      className="btn btn-danger btn-sm"
                      onClick={() => eliminar(proveedor)}
                    >
                      Eliminar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {cargando && <p className="loading">Cargando proveedores...</p>}
      </div>

      {modalAbierto && (
        <Modal
          title={editando ? 'Editar proveedor' : 'Nuevo proveedor'}
          subtitle={editando ? `ID ${editando.id_proveedor}` : 'Completá los datos del proveedor.'}
          onClose={() => setModalAbierto(false)}
        >
          <form className="form" onSubmit={guardar}>
            <label className="field">
              <span>Nombre *</span>
              <input
                type="text"
                value={form.nombre}
                onChange={cambiarCampo('nombre')}
                maxLength="50"
                required
                autoFocus
              />
            </label>

            <div className="form-row">
              <label className="field">
                <span>CUIT *</span>
                <input
                  type="text"
                  value={form.cuit}
                  onChange={cambiarCampo('cuit')}
                  maxLength="13"
                  required
                />
              </label>

              <label className="field">
                <span>Teléfono *</span>
                <input
                  type="text"
                  value={form.telefono}
                  onChange={cambiarCampo('telefono')}
                  maxLength="50"
                  required
                />
              </label>
            </div>

            <label className="field">
              <span>Email</span>
              <input
                type="email"
                value={form.email}
                onChange={cambiarCampo('email')}
                maxLength="50"
              />
            </label>

            <label className="field">
              <span>Tipo de producto *</span>
              <input
                type="text"
                value={form.tipo_producto}
                onChange={cambiarCampo('tipo_producto')}
                maxLength="50"
                required
              />
            </label>

            <label className="field">
              <span>Observaciones</span>
              <input
                type="text"
                value={form.observaciones}
                onChange={cambiarCampo('observaciones')}
                maxLength="50"
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

export default ProveedoresPage
