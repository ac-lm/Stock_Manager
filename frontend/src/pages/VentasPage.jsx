import { useEffect, useState } from 'react'
import Modal from '../components/Modal'
import { cajaService } from '../services/cajaService'
import { clientesService } from '../services/clientesService'
import { productosService } from '../services/catalogoService'
import { ventasService } from '../services/ventasService'
import { MEDIOS_PAGO, etiquetaMedio } from '../utils/caja'
import { formatoNumero } from '../utils/format'

const TAMANIO_PAGINA = 10

const ESTADOS = [
  { value: 'CONFIRMADA', label: 'Confirmada' },
  { value: 'ANULADA', label: 'Anulada' },
]

const pagoVacio = () => ({ medioPago: 'EFECTIVO', monto: '' })

const formVacio = () => ({
  id_cliente: '',
  fechaVencimiento: '',
  detalles: [{ id_producto_fk: '', cantidad: '' }],
  pagos: [pagoVacio()],
})

const etiquetaEstado = (estado) =>
  ESTADOS.find((item) => item.value === estado)?.label ?? estado ?? '—'

const formatearFecha = (valor) => {
  if (!valor) return '—'
  const [anio, mes, dia] = valor.split('-')
  return dia && mes && anio ? `${dia}/${mes}/${anio}` : valor
}

const formatoCantidad = (valor) =>
  Number(valor ?? 0).toLocaleString('es-AR', { maximumFractionDigits: 3 })

function VentasPage() {
  // Listado
  const [ventas, setVentas] = useState([])
  const [pagina, setPagina] = useState(0)
  const [totalPaginas, setTotalPaginas] = useState(0)
  const [totalElementos, setTotalElementos] = useState(0)
  const [recarga, setRecarga] = useState(0)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [mensaje, setMensaje] = useState(null)

  // Filtros
  const [estado, setEstado] = useState('')
  const [id_cliente, setIdCliente] = useState('')
  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')

  // Catálogos para los selectores
  const [clientes, setClientes] = useState([])
  const [productos, setProductos] = useState([])
  const [errorCatalogos, setErrorCatalogos] = useState(null)

  // Caja: undefined mientras carga, null si no hay sesión abierta.
  const [caja, setCaja] = useState(undefined)
  const [errorCaja, setErrorCaja] = useState(null)

  // Modales
  const [modal, setModal] = useState(null)
  const [seleccionada, setSeleccionada] = useState(null)
  const [cargandoDetalle, setCargandoDetalle] = useState(false)
  const [form, setForm] = useState(formVacio)
  const [guardando, setGuardando] = useState(false)
  const [errorForm, setErrorForm] = useState(null)

  useEffect(() => {
    let cancelado = false

    const cargar = async () => {
      setCargando(true)
      try {
        const data = await ventasService.listar({
          estado,
          id_cliente,
          desde,
          hasta,
          page: pagina,
          size: TAMANIO_PAGINA,
        })
        if (!cancelado) {
          setVentas(data.content)
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
  }, [estado, id_cliente, desde, hasta, pagina, recarga])

  // Clientes y productos se cargan una vez: no cambian mientras se arma la venta.
  useEffect(() => {
    let cancelado = false

    const cargarCatalogos = async () => {
      const [clientesRes, productosRes, cajaRes] = await Promise.allSettled([
        clientesService.listar({ size: 100 }),
        productosService.listar(),
        cajaService.actual(),
      ])

      if (cancelado) return

      if (clientesRes.status === 'fulfilled') {
        setClientes(clientesRes.value.content ?? [])
      } else {
        setErrorCatalogos(`No se pudieron cargar los clientes: ${clientesRes.reason.message}`)
      }

      if (productosRes.status === 'fulfilled') {
        setProductos(productosRes.value ?? [])
      } else {
        setErrorCatalogos(`No se pudieron cargar los productos: ${productosRes.reason.message}`)
      }

      if (cajaRes.status === 'fulfilled') {
        setCaja(cajaRes.value)
      } else {
        setCaja(null)
        setErrorCaja(`No se pudo consultar el estado de la caja: ${cajaRes.reason.message}`)
      }
    }

    cargarCatalogos()
    return () => {
      cancelado = true
    }
  }, [])

  const clientesParaVender = clientes.filter(
    (cliente) => cliente.activo && cliente.tipoCliente !== 'GENERICO',
  )

  const hayCajaAbierta = caja?.sesion?.estado === 'ABIERTA'

  const cambiarFiltro = (setter) => (event) => {
    setter(event.target.value)
    setPagina(0)
  }

  const limpiarFiltros = () => {
    setEstado('')
    setIdCliente('')
    setDesde('')
    setHasta('')
    setPagina(0)
  }

  const cerrarModal = () => {
    setModal(null)
    setErrorForm(null)
  }

  const abrirCrear = () => {
    setForm(formVacio())
    setErrorForm(null)
    setError(null)
    setMensaje(null)
    setModal('crear')
  }

  const abrirDetalle = async (id_venta) => {
    setError(null)
    setMensaje(null)
    setSeleccionada(null)
    setCargandoDetalle(true)
    setModal('detalle')
    try {
      setSeleccionada(await ventasService.obtener(id_venta))
    } catch (err) {
      setError(err.message)
      setModal(null)
    } finally {
      setCargandoDetalle(false)
    }
  }

  const cambiarCampo = (campo) => (event) => {
    setForm((prev) => ({ ...prev, [campo]: event.target.value }))
  }

  const cambiarDetalle = (index, campo) => (event) => {
    setForm((prev) => ({
      ...prev,
      detalles: prev.detalles.map((detalle, i) =>
        i === index ? { ...detalle, [campo]: event.target.value } : detalle,
      ),
    }))
  }

  const agregarDetalle = () => {
    setForm((prev) => ({
      ...prev,
      detalles: [...prev.detalles, { id_producto_fk: '', cantidad: '' }],
    }))
  }

  const quitarDetalle = (index) => {
    setForm((prev) => ({
      ...prev,
      detalles: prev.detalles.filter((_, i) => i !== index),
    }))
  }

  const cambiarPago = (index, campo) => (event) => {
    setForm((prev) => ({
      ...prev,
      pagos: prev.pagos.map((pago, i) =>
        i === index ? { ...pago, [campo]: event.target.value } : pago,
      ),
    }))
  }

  const agregarPago = () => {
    setForm((prev) => ({ ...prev, pagos: [...prev.pagos, pagoVacio()] }))
  }

  const quitarPago = (index) => {
    setForm((prev) => ({
      ...prev,
      pagos: prev.pagos.filter((_, i) => i !== index),
    }))
  }

  const productoDe = (id_producto_fk) =>
    productos.find((producto) => producto.id_producto === Number(id_producto_fk))

  const subtotalEstimado = (detalle) => {
    const producto = productoDe(detalle.id_producto_fk)
    if (!producto) return 0
    return Number(producto.precio ?? 0) * Number(detalle.cantidad || 0)
  }

  const totalEstimado = form.detalles.reduce(
    (total, detalle) => total + subtotalEstimado(detalle),
    0,
  )

  const totalPagado = form.pagos.reduce(
    (total, pago) => total + Number(pago.monto || 0),
    0,
  )

  const guardar = async (event) => {
    event.preventDefault()
    if (!hayCajaAbierta) {
      setErrorForm('No hay una sesión de caja abierta. Abra la caja antes de confirmar la venta.')
      return
    }
    setGuardando(true)
    setErrorForm(null)

    const payload = {
      id_cliente: form.id_cliente === '' ? null : Number(form.id_cliente),
      fechaVencimiento: form.fechaVencimiento || null,
      detalles: form.detalles.map((detalle) => ({
        id_producto_fk: Number(detalle.id_producto_fk),
        cantidad: Number(detalle.cantidad),
      })),
      pagos: form.pagos.map((pago) => ({
        medioPago: pago.medioPago,
        monto: Number(pago.monto),
      })),
    }

    try {
      const creada = await ventasService.crear(payload)
      setMensaje(`Venta #${creada.id_venta} confirmada por $${formatoNumero(creada.total)}.`)
      setModal(null)
      setRecarga((n) => n + 1)
    } catch (err) {
      setErrorForm(err.message)
    } finally {
      setGuardando(false)
    }
  }

  const anular = async (venta) => {
    if (
      !window.confirm(
        `¿Anular la venta #${venta.id_venta}? La venta no se borra y no se revierte stock.`,
      )
    ) {
      return
    }

    setError(null)
    setMensaje(null)
    try {
      const anulada = await ventasService.anular(venta.id_venta)
      setSeleccionada((prev) => (prev?.id_venta === anulada.id_venta ? anulada : prev))
      setMensaje(`Venta #${anulada.id_venta} anulada.`)
      setRecarga((n) => n + 1)
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Ventas</h1>
          <p className="page-subtitle">Altas rápidas de mostrador y ventas a clientes registrados.</p>
        </div>
        <button type="button" className="btn btn-primary" onClick={abrirCrear}>
          Nueva venta
        </button>
      </header>

      {error && <div className="banner banner-error">{error}</div>}
      {errorCatalogos && <div className="banner banner-error">{errorCatalogos}</div>}
      {errorCaja && <div className="banner banner-error">{errorCaja}</div>}
      {caja !== undefined && !hayCajaAbierta && !errorCaja && (
        <div className="banner banner-error">
          No hay una sesión de caja abierta. Abra la caja en el módulo Caja para poder vender.
        </div>
      )}
      {mensaje && <div className="banner banner-success">{mensaje}</div>}

      <div className="toolbar">
        <label className="field">
          <span>Estado</span>
          <select value={estado} onChange={cambiarFiltro(setEstado)}>
            <option value="">Todos</option>
            {ESTADOS.map((item) => (
              <option key={item.value} value={item.value}>
                {item.label}
              </option>
            ))}
          </select>
        </label>

        <label className="field">
          <span>Cliente</span>
          <select value={id_cliente} onChange={cambiarFiltro(setIdCliente)}>
            <option value="">Todos</option>
            {clientes.map((cliente) => (
              <option key={cliente.id_cliente} value={cliente.id_cliente}>
                {cliente.nombre}
              </option>
            ))}
          </select>
        </label>

        <label className="field">
          <span>Desde</span>
          <input type="date" value={desde} onChange={cambiarFiltro(setDesde)} />
        </label>

        <label className="field">
          <span>Hasta</span>
          <input type="date" value={hasta} onChange={cambiarFiltro(setHasta)} />
        </label>

        <button type="button" className="btn btn-secondary btn-sm" onClick={limpiarFiltros}>
          Limpiar
        </button>
      </div>

      <div className="card">
        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Cliente</th>
                <th>Fecha</th>
                <th>Hora</th>
                <th>Estado</th>
                <th>Total</th>
                <th className="table-actions">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {!cargando && ventas.length === 0 && (
                <tr>
                  <td colSpan="7" className="empty-state">
                    No se encontraron ventas.
                  </td>
                </tr>
              )}
              {ventas.map((venta) => (
                <tr key={venta.id_venta}>
                  <td className="mono">#{venta.id_venta}</td>
                  <td>{venta.cliente || `Cliente ${venta.id_cliente}`}</td>
                  <td>{formatearFecha(venta.fecha)}</td>
                  <td className="mono">{venta.hora?.slice(0, 5) || '—'}</td>
                  <td>
                    <span
                      className={
                        venta.estado === 'CONFIRMADA' ? 'badge badge-activo' : 'badge badge-inactivo'
                      }
                    >
                      {etiquetaEstado(venta.estado)}
                    </span>
                  </td>
                  <td className="mono">${formatoNumero(venta.total)}</td>
                  <td className="table-actions">
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      onClick={() => abrirDetalle(venta.id_venta)}
                    >
                      Ver
                    </button>
                    {venta.estado === 'CONFIRMADA' && (
                      <button
                        type="button"
                        className="btn btn-danger btn-sm"
                        onClick={() => anular(venta)}
                      >
                        Anular
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {cargando && <p className="loading">Cargando ventas...</p>}
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
          Página {pagina + 1} de {Math.max(totalPaginas, 1)} · {totalElementos} ventas
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

      {modal === 'crear' && (
        <Modal
          title="Nueva venta"
          subtitle={`Se confirma al guardar. Caja ${caja?.sesion ? `sesión #${caja.sesion.id_sesion}` : 'sin abrir'}. El precio se toma de la lista del tipo de cliente.`}
          onClose={cerrarModal}
        >
          {errorForm && <div className="banner banner-error">{errorForm}</div>}
          {!hayCajaAbierta && (
            <div className="aviso">
              No hay caja abierta: abra una sesión en el módulo Caja antes de confirmar.
            </div>
          )}

          <form className="form" onSubmit={guardar}>
            <div className="form-row">
              <label className="field">
                <span>Cliente</span>
                <select value={form.id_cliente} onChange={cambiarCampo('id_cliente')}>
                  <option value="">Consumidor final (mostrador)</option>
                  {clientesParaVender.map((cliente) => (
                    <option key={cliente.id_cliente} value={cliente.id_cliente}>
                      {cliente.nombre}
                    </option>
                  ))}
                </select>
                <small className="field-help">Sin cliente cargado se usa el genérico.</small>
              </label>

              <label className="field">
                <span>Vencimiento del saldo (opcional)</span>
                <input
                  type="date"
                  value={form.fechaVencimiento}
                  onChange={cambiarCampo('fechaVencimiento')}
                />
              </label>
            </div>

            <div className="seccion-header">
              <h3>Productos</h3>
              <button type="button" className="btn btn-secondary btn-sm" onClick={agregarDetalle}>
                Agregar producto
              </button>
            </div>

            <div className="table-wrapper">
              <table className="table table-compact">
                <thead>
                  <tr>
                    <th>Producto</th>
                    <th>Cantidad</th>
                    <th>Subtotal estimado</th>
                    <th className="table-actions" />
                  </tr>
                </thead>
                <tbody>
                  {form.detalles.map((detalle, index) => (
                    <tr key={index}>
                      <td>
                        <select
                          required
                          value={detalle.id_producto_fk}
                          onChange={cambiarDetalle(index, 'id_producto_fk')}
                        >
                          <option value="">Seleccionar...</option>
                          {productos.map((producto) => (
                            <option key={producto.id_producto} value={producto.id_producto}>
                              {producto.nombre}
                            </option>
                          ))}
                        </select>
                      </td>
                      <td>
                        <input
                          required
                          type="number"
                          min="0.001"
                          step="0.001"
                          value={detalle.cantidad}
                          onChange={cambiarDetalle(index, 'cantidad')}
                          placeholder="0.000"
                        />
                      </td>
                      <td className="mono">
                        {productoDe(detalle.id_producto_fk)
                          ? `$${formatoNumero(subtotalEstimado(detalle))}`
                          : '—'}
                      </td>
                      <td className="table-actions">
                        <button
                          type="button"
                          className="btn btn-ghost btn-sm"
                          disabled={form.detalles.length === 1}
                          onClick={() => quitarDetalle(index)}
                          aria-label="Quitar renglón"
                        >
                          ×
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {productos.length === 0 && (
              <div className="aviso">No hay productos cargados en el catálogo.</div>
            )}

            <div className="total-filtro">
              <span>Total estimado:</span>
              <strong>${formatoNumero(totalEstimado)}</strong>
              <small>Usa precios de catálogo; el servidor aplica la lista del cliente.</small>
            </div>

            <div className="seccion-header">
              <h3>Pagos</h3>
              <button type="button" className="btn btn-secondary btn-sm" onClick={agregarPago}>
                Agregar pago
              </button>
            </div>

            <div className="table-wrapper">
              <table className="table table-compact">
                <thead>
                  <tr>
                    <th>Medio</th>
                    <th>Monto</th>
                    <th className="table-actions" />
                  </tr>
                </thead>
                <tbody>
                  {form.pagos.map((pago, index) => (
                    <tr key={index}>
                      <td>
                        <select
                          required
                          value={pago.medioPago}
                          onChange={cambiarPago(index, 'medioPago')}
                        >
                          {MEDIOS_PAGO.map((opcion) => (
                            <option key={opcion.value} value={opcion.value}>
                              {opcion.label}
                            </option>
                          ))}
                        </select>
                      </td>
                      <td>
                        <input
                          required
                          type="number"
                          min="0.01"
                          step="0.01"
                          value={pago.monto}
                          onChange={cambiarPago(index, 'monto')}
                          placeholder="0.00"
                        />
                      </td>
                      <td className="table-actions">
                        <button
                          type="button"
                          className="btn btn-ghost btn-sm"
                          disabled={form.pagos.length === 1}
                          onClick={() => quitarPago(index)}
                          aria-label="Quitar pago"
                        >
                          ×
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="total-filtro">
              <span>Total pagado:</span>
              <strong>${formatoNumero(totalPagado)}</strong>
              <small>
                Diferencia con el estimado: ${formatoNumero(totalPagado - totalEstimado)}. El
                servidor exige que los pagos sumen el total final.
              </small>
            </div>

            <div className="aviso">
              Cada pago genera un ingreso en la caja abierta. El vendedor se toma de la sesión.
              No se descuenta stock ni se genera comprobante.
            </div>

            <div className="modal-footer">
              <button type="button" className="btn btn-secondary" onClick={cerrarModal}>
                Cancelar
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={guardando || !hayCajaAbierta}
              >
                {guardando ? 'Guardando...' : 'Confirmar venta'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {modal === 'detalle' && (
        <Modal
          title={seleccionada ? `Venta #${seleccionada.id_venta}` : 'Venta'}
          subtitle={seleccionada ? `Cliente: ${seleccionada.cliente}` : ''}
          onClose={cerrarModal}
        >
          {cargandoDetalle || !seleccionada ? (
            <p className="loading">Cargando venta...</p>
          ) : (
            <>
              <div className="total-filtro">
                <span>Total:</span>
                <strong>${formatoNumero(seleccionada.total)}</strong>
                <span>{etiquetaEstado(seleccionada.estado)}</span>
              </div>

              <div className="form-row">
                <div className="field">
                  <span>Fecha</span>
                  <span className="mono">{formatearFecha(seleccionada.fecha)}</span>
                </div>
                <div className="field">
                  <span>Hora</span>
                  <span className="mono">{seleccionada.hora?.slice(0, 5) || '—'}</span>
                </div>
                <div className="field">
                  <span>Vendedor</span>
                  <span className="mono">#{seleccionada.id_vendedor}</span>
                </div>
                <div className="field">
                  <span>Vencimiento</span>
                  <span className="mono">{formatearFecha(seleccionada.fechaVencimiento)}</span>
                </div>
              </div>

              <div className="seccion-header">
                <h3>Renglones</h3>
              </div>

              <div className="table-wrapper">
                <table className="table table-compact">
                  <thead>
                    <tr>
                      <th>Producto</th>
                      <th>Cantidad</th>
                      <th>Precio unitario</th>
                      <th>Subtotal</th>
                    </tr>
                  </thead>
                  <tbody>
                    {seleccionada.detalles?.map((detalle) => (
                      <tr key={detalle.id_detalle_venta}>
                        <td className="mono">#{detalle.id_producto_fk}</td>
                        <td className="mono">{formatoCantidad(detalle.cantidad)}</td>
                        <td className="mono">${formatoNumero(detalle.precioHistorico)}</td>
                        <td className="mono">${formatoNumero(detalle.subtotal)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="seccion-header">
                <h3>Pagos registrados en caja</h3>
              </div>

              <div className="table-wrapper">
                <table className="table table-compact">
                  <thead>
                    <tr>
                      <th>Medio</th>
                      <th>Monto</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(seleccionada.pagos ?? []).map((pago, index) => (
                      <tr key={index}>
                        <td>{etiquetaMedio(pago.medioPago)}</td>
                        <td className="mono">${formatoNumero(pago.monto)}</td>
                      </tr>
                    ))}
                    {(!seleccionada.pagos || seleccionada.pagos.length === 0) && (
                      <tr>
                        <td colSpan="2" className="empty-state">
                          Sin pagos registrados.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>

              <div className="modal-footer">
                {seleccionada.estado === 'CONFIRMADA' && (
                  <button
                    type="button"
                    className="btn btn-danger"
                    onClick={() => anular(seleccionada)}
                  >
                    Anular venta
                  </button>
                )}
                <button type="button" className="btn btn-secondary" onClick={cerrarModal}>
                  Cerrar
                </button>
              </div>
            </>
          )}
        </Modal>
      )}
    </section>
  )
}

export default VentasPage
