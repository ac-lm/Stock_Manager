import { useEffect, useState } from 'react'
import Modal from '../components/Modal'
import { stockService } from '../services/stockService'
import { formatearFechaHoraISO } from '../utils/caja'
import { formatoNumero } from '../utils/format'

const CONFIG = {
  insumo: {
    titulo: 'Stock de insumos',
    subtitulo: 'Saldos, ajustes y kardex de insumos.',
    singular: 'insumo',
    idKey: 'id_insumo',
  },
  producto: {
    titulo: 'Stock de productos',
    subtitulo: 'Saldos, ajustes y kardex de productos terminados.',
    singular: 'producto',
    idKey: 'id_producto',
  },
}

const TIPOS = [
  { value: 'ENTRADA', label: 'Entrada (suma)' },
  { value: 'SALIDA', label: 'Salida (descuenta)' },
]

const MOTIVOS = [
  { value: 'COMPRA', label: 'Compra' },
  { value: 'AJUSTE', label: 'Ajuste' },
  { value: 'MERMA', label: 'Merma' },
  { value: 'DEVOLUCION', label: 'Devolución' },
]

const FORM_VACIO = { tipo: 'ENTRADA', motivo: 'AJUSTE', cantidad: '', observaciones: '' }

function StockPage({ entidad }) {
  const config = CONFIG[entidad]
  const [items, setItems] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [mensaje, setMensaje] = useState(null)
  const [filtroNombre, setFiltroNombre] = useState('')

  const [itemMovimiento, setItemMovimiento] = useState(null)
  const [form, setForm] = useState(FORM_VACIO)
  const [guardando, setGuardando] = useState(false)
  const [errorForm, setErrorForm] = useState(null)

  const [itemKardex, setItemKardex] = useState(null)
  const [movimientos, setMovimientos] = useState([])
  const [cargandoKardex, setCargandoKardex] = useState(false)
  const [errorKardex, setErrorKardex] = useState(null)

  const listar = () =>
    entidad === 'insumo' ? stockService.listarInsumos() : stockService.listarProductos()

  const cargar = async () => {
    setCargando(true)
    setError(null)
    try {
      setItems(await listar())
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
        const data = entidad === 'insumo'
          ? await stockService.listarInsumos()
          : await stockService.listarProductos()
        if (!cancelado) setItems(data)
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
  }, [entidad])

  const itemsFiltrados = items.filter((item) =>
    (item.nombre ?? '').toLocaleLowerCase('es').includes(filtroNombre.toLocaleLowerCase('es')),
  )

  const abrirMovimiento = (item) => {
    setItemMovimiento(item)
    setForm(FORM_VACIO)
    setErrorForm(null)
    setMensaje(null)
  }

  const cambiarCampo = (campo) => (event) => {
    setForm((prev) => ({ ...prev, [campo]: event.target.value }))
  }

  const guardarMovimiento = async (event) => {
    event.preventDefault()
    const cantidad = Number(form.cantidad || 0)
    if (!Number.isFinite(cantidad) || cantidad <= 0) {
      setErrorForm('La cantidad debe ser mayor a cero.')
      return
    }

    setGuardando(true)
    setErrorForm(null)
    const payload = {
      [config.idKey]: itemMovimiento[config.idKey],
      tipo: form.tipo,
      motivo: form.motivo,
      cantidad,
      observaciones: form.observaciones.trim() || null,
    }

    try {
      await stockService.registrarMovimiento(entidad === 'insumo' ? 'insumos' : 'productos', payload)
      setItemMovimiento(null)
      setMensaje(`Movimiento registrado para ${itemMovimiento.nombre}.`)
      await cargar()
    } catch (err) {
      setErrorForm(err.status === 409 ? `Stock insuficiente. ${err.message}` : err.message)
    } finally {
      setGuardando(false)
    }
  }

  const verKardex = async (item) => {
    setItemKardex(item)
    setMovimientos([])
    setErrorKardex(null)
    setCargandoKardex(true)
    try {
      const ruta = entidad === 'insumo' ? 'insumos' : 'productos'
      setMovimientos(await stockService.kardex(ruta, item[config.idKey]))
    } catch (err) {
      setErrorKardex(err.message)
    } finally {
      setCargandoKardex(false)
    }
  }

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>{config.titulo}</h1>
          <p className="page-subtitle">{config.subtitulo}</p>
        </div>
      </header>

      {error && <div className="banner banner-error">{error}</div>}
      {mensaje && <div className="banner banner-success">{mensaje}</div>}

      <div className="toolbar">
        <label className="field field-inline">
          <span>Nombre</span>
          <input
            type="search"
            value={filtroNombre}
            onChange={(event) => setFiltroNombre(event.target.value)}
            placeholder="Buscar por nombre"
          />
        </label>
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          onClick={() => setFiltroNombre('')}
          disabled={!filtroNombre}
        >
          Limpiar filtro
        </button>
      </div>

      <div className="card">
        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Nombre</th>
                {entidad === 'insumo' && <th>Unidad</th>}
                {entidad === 'producto' && <th>Categoría</th>}
                <th>Saldo</th>
                {entidad === 'insumo' && <th>Mínimo</th>}
                {entidad === 'insumo' && <th>Estado</th>}
                <th className="table-actions">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {!cargando && itemsFiltrados.length === 0 && (
                <tr>
                  <td colSpan={entidad === 'insumo' ? 8 : 5} className="empty-state">
                    No hay {entidad === 'insumo' ? 'insumos' : 'productos'} para mostrar.
                  </td>
                </tr>
              )}
              {itemsFiltrados.map((item) => (
                <tr key={item[config.idKey]}>
                  <td className="mono">{item[config.idKey]}</td>
                  <td>{item.nombre}</td>
                  {entidad === 'insumo' && <td>{item.unidadMedida}</td>}
                  {entidad === 'producto' && <td>{item.categoria}</td>}
                  <td className="mono">{formatoNumero(item.stockActual, 3)}</td>
                  {entidad === 'insumo' && (
                    <td className="mono">{formatoNumero(item.stockMinimo, 0)}</td>
                  )}
                  {entidad === 'insumo' && (
                    <td>
                      <span
                        className={
                          item.stockBajoMinimo ? 'badge badge-pendiente' : 'badge badge-activo'
                        }
                      >
                        {item.stockBajoMinimo ? 'Bajo mínimo' : 'Normal'}
                      </span>
                    </td>
                  )}
                  <td className="table-actions">
                    <button
                      type="button"
                      className="btn btn-primary btn-sm"
                      onClick={() => abrirMovimiento(item)}
                    >
                      Registrar movimiento
                    </button>
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      onClick={() => verKardex(item)}
                    >
                      Ver kardex
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {cargando && <p className="loading">Cargando stock...</p>}
      </div>

      {itemMovimiento && (
        <Modal
          title={`Registrar movimiento de ${config.singular}`}
          subtitle={`${itemMovimiento.nombre} · saldo actual ${
            itemMovimiento.stockActual ?? 0
          }`}
          onClose={() => setItemMovimiento(null)}
        >
          <form className="form" onSubmit={guardarMovimiento}>
            {errorForm && <div className="banner banner-error">{errorForm}</div>}

            <div className="form-row">
              <label className="field">
                <span>Tipo *</span>
                <select value={form.tipo} onChange={cambiarCampo('tipo')}>
                  {TIPOS.map((opcion) => (
                    <option key={opcion.value} value={opcion.value}>
                      {opcion.label}
                    </option>
                  ))}
                </select>
              </label>

              <label className="field">
                <span>Motivo *</span>
                <select value={form.motivo} onChange={cambiarCampo('motivo')}>
                  {MOTIVOS.map((opcion) => (
                    <option key={opcion.value} value={opcion.value}>
                      {opcion.label}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            <label className="field">
              <span>Cantidad *</span>
              <input
                type="number"
                min="0.001"
                step="0.001"
                value={form.cantidad}
                onChange={cambiarCampo('cantidad')}
                required
                autoFocus
              />
            </label>

            <label className="field">
              <span>Observaciones</span>
              <input
                type="text"
                value={form.observaciones}
                onChange={cambiarCampo('observaciones')}
                maxLength="150"
                placeholder="Opcional"
              />
            </label>

            <div className="modal-footer">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setItemMovimiento(null)}
              >
                Cancelar
              </button>
              <button type="submit" className="btn btn-primary" disabled={guardando}>
                {guardando ? 'Guardando...' : 'Guardar'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {itemKardex && (
        <Modal
          title={`Kardex de ${itemKardex.nombre}`}
          subtitle="Movimientos del más reciente al más viejo."
          onClose={() => setItemKardex(null)}
        >
          {errorKardex && <div className="banner banner-error">{errorKardex}</div>}

          <div className="table-wrapper">
            <table className="table table-compact">
              <thead>
                <tr>
                  <th>Fecha y hora</th>
                  <th>Tipo</th>
                  <th>Motivo</th>
                  <th>Cantidad</th>
                  <th>Usuario</th>
                  <th>Observaciones</th>
                </tr>
              </thead>
              <tbody>
                {!cargandoKardex && movimientos.length === 0 && (
                  <tr>
                    <td colSpan="6" className="empty-state">
                      Este {config.singular} todavía no tiene movimientos.
                    </td>
                  </tr>
                )}
                {movimientos.map((movimiento) => (
                  <tr key={movimiento.id_movimiento_stock}>
                    <td className="mono">{formatearFechaHoraISO(movimiento.fecha_hora)}</td>
                    <td>{movimiento.tipo}</td>
                    <td>{movimiento.motivo}</td>
                    <td className="mono">{formatoNumero(movimiento.cantidad, 3)}</td>
                    <td>{movimiento.usuario}</td>
                    <td>{movimiento.observaciones ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {cargandoKardex && <p className="loading">Cargando movimientos...</p>}
        </Modal>
      )}
    </section>
  )
}

export default StockPage
