import { useEffect, useState } from 'react'
import Modal from '../components/Modal'
import { insumosService, productosService } from '../services/catalogoService'
import { formatoNumero } from '../utils/format'

const FORM_VACIO = {
  nombre: '',
  categoria: '',
  precio: '0',
  stockActual: '0',
}

const ASIGNACION_VACIA = {
  id_insumo: '',
  cantNecesaria: '1',
  unidadMedida: '',
}

function ProductosPage() {
  const [productos, setProductos] = useState([])
  const [insumos, setInsumos] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [mensaje, setMensaje] = useState(null)

  const [modalProducto, setModalProducto] = useState(false)
  const [editando, setEditando] = useState(null)
  const [form, setForm] = useState(FORM_VACIO)
  const [guardando, setGuardando] = useState(false)

  const [productoActivo, setProductoActivo] = useState(null)
  const [asignaciones, setAsignaciones] = useState([])
  const [cargandoAsignaciones, setCargandoAsignaciones] = useState(false)
  const [errorAsignacion, setErrorAsignacion] = useState(null)
  const [editandoAsignacion, setEditandoAsignacion] = useState(null)
  const [formAsignacion, setFormAsignacion] = useState(ASIGNACION_VACIA)

  const cargar = async () => {
    setCargando(true)
    setError(null)
    try {
      const [productosData, insumosData] = await Promise.all([
        productosService.listar(),
        insumosService.listar(),
      ])
      setProductos(productosData)
      setInsumos(insumosData)
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
        const [productosData, insumosData] = await Promise.all([
          productosService.listar(),
          insumosService.listar(),
        ])
        if (!cancelado) {
          setProductos(productosData)
          setInsumos(insumosData)
        }
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
    setModalProducto(true)
  }

  const abrirEditar = (producto) => {
    setEditando(producto)
    setForm({
      nombre: producto.nombre ?? '',
      categoria: producto.categoria ?? '',
      precio: String(producto.precio ?? 0),
      stockActual: String(producto.stockActual ?? 0),
    })
    setError(null)
    setMensaje(null)
    setModalProducto(true)
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
      categoria: form.categoria.trim(),
      precio: Number(form.precio || 0),
      stockActual: Number(form.stockActual || 0),
    }

    try {
      if (editando) {
        await productosService.actualizar(editando.id_producto, payload)
        setMensaje('Producto actualizado correctamente.')
      } else {
        await productosService.crear(payload)
        setMensaje('Producto creado correctamente.')
      }
      setModalProducto(false)
      await cargar()
    } catch (err) {
      setError(err.message)
    } finally {
      setGuardando(false)
    }
  }

  const eliminar = async (producto) => {
    if (!window.confirm(`¿Eliminar el producto "${producto.nombre}"?`)) return

    setError(null)
    try {
      await productosService.eliminar(producto.id_producto)
      setMensaje('Producto eliminado correctamente.')
      await cargar()
    } catch (err) {
      setError(err.message)
    }
  }

  const cargarAsignaciones = async (id_producto) => {
    setCargandoAsignaciones(true)
    setErrorAsignacion(null)
    try {
      setAsignaciones(await productosService.listarInsumos(id_producto))
    } catch (err) {
      setErrorAsignacion(err.message)
    } finally {
      setCargandoAsignaciones(false)
    }
  }

  const abrirInsumos = async (producto) => {
    setProductoActivo(producto)
    setEditandoAsignacion(null)
    setFormAsignacion(ASIGNACION_VACIA)
    setErrorAsignacion(null)
    await cargarAsignaciones(producto.id_producto)
  }

  const cerrarInsumos = () => {
    setProductoActivo(null)
    setAsignaciones([])
    setEditandoAsignacion(null)
    setFormAsignacion(ASIGNACION_VACIA)
    setErrorAsignacion(null)
  }

  const cambiarInsumo = (event) => {
    const id_insumo = event.target.value
    const insumo = insumos.find((item) => String(item.id_insumo) === id_insumo)
    setFormAsignacion((prev) => ({
      ...prev,
      id_insumo,
      unidadMedida: insumo?.unidadMedida ?? prev.unidadMedida,
    }))
  }

  const cambiarCampoAsignacion = (campo) => (event) => {
    setFormAsignacion((prev) => ({ ...prev, [campo]: event.target.value }))
  }

  const guardarAsignacion = async (event) => {
    event.preventDefault()
    setErrorAsignacion(null)

    if (!formAsignacion.id_insumo) {
      setErrorAsignacion('Seleccioná un insumo.')
      return
    }
    if (!formAsignacion.unidadMedida.trim()) {
      setErrorAsignacion('Indicá la unidad de medida.')
      return
    }

    const payload = {
      cantNecesaria: Number(formAsignacion.cantNecesaria || 0),
      unidadMedida: formAsignacion.unidadMedida.trim(),
    }

    try {
      if (editandoAsignacion) {
        await productosService.actualizarInsumo(
          productoActivo.id_producto,
          editandoAsignacion.id_producto_insumo,
          payload,
        )
      } else {
        await productosService.asignarInsumo(productoActivo.id_producto, {
          id_insumo: Number(formAsignacion.id_insumo),
          ...payload,
        })
      }
      setEditandoAsignacion(null)
      setFormAsignacion(ASIGNACION_VACIA)
      await cargarAsignaciones(productoActivo.id_producto)
    } catch (err) {
      setErrorAsignacion(err.message)
    }
  }

  const editarAsignacion = (asignacion) => {
    setEditandoAsignacion(asignacion)
    setFormAsignacion({
      id_insumo: String(asignacion.id_insumo),
      cantNecesaria: String(asignacion.cantNecesaria ?? 0),
      unidadMedida: asignacion.unidadMedida ?? '',
    })
    setErrorAsignacion(null)
  }

  const cancelarEdicionAsignacion = () => {
    setEditandoAsignacion(null)
    setFormAsignacion(ASIGNACION_VACIA)
  }

  const quitarAsignacion = async (asignacion) => {
    if (!window.confirm(`¿Quitar el insumo "${asignacion.nombreInsumo}" del producto?`)) return

    setErrorAsignacion(null)
    try {
      await productosService.eliminarInsumo(
        productoActivo.id_producto,
        asignacion.id_producto_insumo,
      )
      await cargarAsignaciones(productoActivo.id_producto)
    } catch (err) {
      setErrorAsignacion(err.message)
    }
  }

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Productos</h1>
          <p className="page-subtitle">Productos terminados y los insumos que consumen.</p>
        </div>
        <button type="button" className="btn btn-primary" onClick={abrirCrear}>
          Nuevo producto
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
                <th>Categoría</th>
                <th>Precio</th>
                <th>Stock actual</th>
                <th className="table-actions">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {!cargando && productos.length === 0 && (
                <tr>
                  <td colSpan="6" className="empty-state">
                    No hay productos cargados todavía.
                  </td>
                </tr>
              )}
              {productos.map((producto) => (
                <tr key={producto.id_producto}>
                  <td className="mono">{producto.id_producto}</td>
                  <td>{producto.nombre}</td>
                  <td>{producto.categoria}</td>
                  <td>{formatoNumero(producto.precio)}</td>
                  <td>{formatoNumero(producto.stockActual, 3)}</td>
                  <td className="table-actions">
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      onClick={() => abrirInsumos(producto)}
                    >
                      Insumos
                    </button>
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      onClick={() => abrirEditar(producto)}
                    >
                      Editar
                    </button>
                    <button
                      type="button"
                      className="btn btn-danger btn-sm"
                      onClick={() => eliminar(producto)}
                    >
                      Eliminar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {cargando && <p className="loading">Cargando productos...</p>}
      </div>

      {modalProducto && (
        <Modal
          title={editando ? 'Editar producto' : 'Nuevo producto'}
          subtitle={
            editando ? `ID ${editando.id_producto}` : 'Completá los datos del producto.'
          }
          onClose={() => setModalProducto(false)}
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
              <span>Categoría *</span>
              <input
                type="text"
                value={form.categoria}
                onChange={cambiarCampo('categoria')}
                maxLength="50"
                required
              />
            </label>

            <div className="form-row">
              <label className="field">
                <span>Precio</span>
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={form.precio}
                  onChange={cambiarCampo('precio')}
                />
              </label>

              <label className="field">
                <span>Stock actual</span>
                <input
                  type="number"
                  min="0"
                  step="0.001"
                  value={form.stockActual}
                  onChange={cambiarCampo('stockActual')}
                />
              </label>
            </div>

            <div className="modal-footer">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setModalProducto(false)}
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

      {productoActivo && (
        <Modal
          title={`Insumos de ${productoActivo.nombre}`}
          subtitle="Cantidad de insumo necesaria por unidad de producto."
          onClose={cerrarInsumos}
        >
          {errorAsignacion && <div className="banner banner-error">{errorAsignacion}</div>}

          <div className="table-wrapper">
            <table className="table table-compact">
              <thead>
                <tr>
                  <th>Insumo</th>
                  <th>Cantidad necesaria</th>
                  <th>Unidad</th>
                  <th className="table-actions">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {!cargandoAsignaciones && asignaciones.length === 0 && (
                  <tr>
                    <td colSpan="4" className="empty-state">
                      Este producto todavía no tiene insumos asignados.
                    </td>
                  </tr>
                )}
                {asignaciones.map((asignacion) => (
                  <tr key={asignacion.id_producto_insumo}>
                    <td>{asignacion.nombreInsumo}</td>
                    <td>{formatoNumero(asignacion.cantNecesaria, 3)}</td>
                    <td>{asignacion.unidadMedida}</td>
                    <td className="table-actions">
                      <button
                        type="button"
                        className="btn btn-secondary btn-sm"
                        onClick={() => editarAsignacion(asignacion)}
                      >
                        Editar
                      </button>
                      <button
                        type="button"
                        className="btn btn-danger btn-sm"
                        onClick={() => quitarAsignacion(asignacion)}
                      >
                        Quitar
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {cargandoAsignaciones && <p className="loading">Cargando insumos...</p>}

          <form className="form form-asignacion" onSubmit={guardarAsignacion}>
            <h3>{editandoAsignacion ? 'Editar asignación' : 'Agregar insumo'}</h3>

            <div className="form-row">
              <label className="field">
                <span>Insumo *</span>
                <select
                  value={formAsignacion.id_insumo}
                  onChange={cambiarInsumo}
                  disabled={Boolean(editandoAsignacion)}
                  required
                >
                  <option value="">Seleccioná un insumo</option>
                  {insumos.map((insumo) => (
                    <option key={insumo.id_insumo} value={insumo.id_insumo}>
                      {insumo.nombre}
                    </option>
                  ))}
                </select>
              </label>

              <label className="field">
                <span>Cantidad necesaria *</span>
                <input
                  type="number"
                  min="0"
                  step="0.001"
                  value={formAsignacion.cantNecesaria}
                  onChange={cambiarCampoAsignacion('cantNecesaria')}
                  required
                />
              </label>

              <label className="field">
                <span>Unidad de medida *</span>
                <input
                  type="text"
                  value={formAsignacion.unidadMedida}
                  onChange={cambiarCampoAsignacion('unidadMedida')}
                  maxLength="50"
                  required
                />
              </label>
            </div>

            <div className="modal-footer">
              {editandoAsignacion && (
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={cancelarEdicionAsignacion}
                >
                  Cancelar edición
                </button>
              )}
              <button type="submit" className="btn btn-primary">
                {editandoAsignacion ? 'Guardar cambios' : 'Agregar insumo'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </section>
  )
}

export default ProductosPage
