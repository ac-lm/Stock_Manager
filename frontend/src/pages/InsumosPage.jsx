import { useEffect, useState } from 'react'
import Modal from '../components/Modal'
import { insumosService } from '../services/catalogoService'
import { formatoNumero } from '../utils/format'

const FORM_VACIO = {
  nombre: '',
  unidadMedida: '',
  stockActual: '0',
  stockMinimo: '0',
  costo: '0',
}

function InsumosPage() {
  const [insumos, setInsumos] = useState([])
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
      setInsumos(await insumosService.listar())
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
        const data = await insumosService.listar()
        if (!cancelado) setInsumos(data)
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

  const abrirEditar = (insumo) => {
    setEditando(insumo)
    setForm({
      nombre: insumo.nombre ?? '',
      unidadMedida: insumo.unidadMedida ?? '',
      stockActual: String(insumo.stockActual ?? 0),
      stockMinimo: String(insumo.stockMinimo ?? 0),
      costo: String(insumo.costo ?? 0),
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
      unidadMedida: form.unidadMedida.trim(),
      stockActual: Number(form.stockActual || 0),
      stockMinimo: Number(form.stockMinimo || 0),
      costo: Number(form.costo || 0),
    }

    try {
      if (editando) {
        await insumosService.actualizar(editando.id_insumo, payload)
        setMensaje('Insumo actualizado correctamente.')
      } else {
        await insumosService.crear(payload)
        setMensaje('Insumo creado correctamente.')
      }
      setModalAbierto(false)
      await cargar()
    } catch (err) {
      setError(err.message)
    } finally {
      setGuardando(false)
    }
  }

  const eliminar = async (insumo) => {
    if (!window.confirm(`¿Eliminar el insumo "${insumo.nombre}"?`)) return

    setError(null)
    try {
      await insumosService.eliminar(insumo.id_insumo)
      setMensaje('Insumo eliminado correctamente.')
      await cargar()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Insumos</h1>
          <p className="page-subtitle">Materias primas que se usan para producir.</p>
        </div>
        <button type="button" className="btn btn-primary" onClick={abrirCrear}>
          Nuevo insumo
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
                <th>Unidad</th>
                <th>Stock actual</th>
                <th>Stock mínimo</th>
                <th>Costo</th>
                <th className="table-actions">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {!cargando && insumos.length === 0 && (
                <tr>
                  <td colSpan="7" className="empty-state">
                    No hay insumos cargados todavía.
                  </td>
                </tr>
              )}
              {insumos.map((insumo) => (
                <tr key={insumo.id_insumo}>
                  <td className="mono">{insumo.id_insumo}</td>
                  <td>{insumo.nombre}</td>
                  <td>{insumo.unidadMedida}</td>
                  <td>{formatoNumero(insumo.stockActual, 0)}</td>
                  <td>{formatoNumero(insumo.stockMinimo, 0)}</td>
                  <td>{formatoNumero(insumo.costo)}</td>
                  <td className="table-actions">
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      onClick={() => abrirEditar(insumo)}
                    >
                      Editar
                    </button>
                    <button
                      type="button"
                      className="btn btn-danger btn-sm"
                      onClick={() => eliminar(insumo)}
                    >
                      Eliminar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {cargando && <p className="loading">Cargando insumos...</p>}
      </div>

      {modalAbierto && (
        <Modal
          title={editando ? 'Editar insumo' : 'Nuevo insumo'}
          subtitle={editando ? `ID ${editando.id_insumo}` : 'Completá los datos del insumo.'}
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

            <label className="field">
              <span>Unidad de medida *</span>
              <input
                type="text"
                value={form.unidadMedida}
                onChange={cambiarCampo('unidadMedida')}
                maxLength="50"
                placeholder="kg, litros, unidades..."
                required
              />
            </label>

            <div className="form-row">
              <label className="field">
                <span>Stock actual</span>
                <input
                  type="number"
                  min="0"
                  step="1"
                  value={form.stockActual}
                  onChange={cambiarCampo('stockActual')}
                />
              </label>

              <label className="field">
                <span>Stock mínimo</span>
                <input
                  type="number"
                  min="0"
                  step="1"
                  value={form.stockMinimo}
                  onChange={cambiarCampo('stockMinimo')}
                />
              </label>
            </div>

            <label className="field">
              <span>Costo</span>
              <input
                type="number"
                min="0"
                step="0.01"
                value={form.costo}
                onChange={cambiarCampo('costo')}
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

export default InsumosPage
