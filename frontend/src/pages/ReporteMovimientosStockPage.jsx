import { useEffect, useState } from 'react'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { insumosService, productosService } from '../services/catalogoService'
import { proveedoresService } from '../services/proveedoresService'
import { reportesService } from '../services/reportesService'
import { formatoNumero } from '../utils/format'

// Las fechas se eligen como fechas de calendario: el backend las aplica sin convertir zonas.
const ZONA_TALLER = 'America/Argentina/Buenos_Aires'
const FORMATOS_EXPORTACION = [
  { formato: 'xlsx', etiqueta: 'Excel' },
  { formato: 'csv', etiqueta: 'CSV' },
  { formato: 'pdf', etiqueta: 'PDF' },
]
const TIPOS_MOVIMIENTO = [
  { value: 'TODOS', label: 'Todos' },
  { value: 'ENTRADA', label: 'Entradas' },
  { value: 'SALIDA', label: 'Salidas' },
]
const SERIES = [
  { clave: 'entradas', etiqueta: 'Entradas', color: '#16a34a' },
  { clave: 'salidasVenta', etiqueta: 'Salidas por venta', color: '#2563eb' },
  { clave: 'descartes', etiqueta: 'Descartes', color: '#dc2626' },
  { clave: 'ajustes', etiqueta: 'Ajustes', color: '#f59e0b' },
  { clave: 'otrasSalidas', etiqueta: 'Otras salidas', color: '#7c3aed' },
]

function fechaHoyEnTaller() {
  const partes = new Intl.DateTimeFormat('es-AR', {
    timeZone: ZONA_TALLER,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date())
  const valor = (tipo) => partes.find((parte) => parte.type === tipo).value
  return `${valor('year')}-${valor('month')}-${valor('day')}`
}

function filtrosIniciales() {
  const hoy = fechaHoyEnTaller()
  return {
    desde: `${hoy.slice(0, 7)}-01`,
    hasta: hoy,
    articulo: '',
    tipo_movimiento: 'TODOS',
    id_proveedor: '',
  }
}

// El selector de artículo junta ambos catálogos; el tipo viaja con el id para no mezclar ids iguales.
function aFiltrosApi(filtros) {
  const [tipoArticulo, idArticulo] = filtros.articulo
    ? filtros.articulo.split(':')
    : ['TODOS', '']
  return {
    desde: filtros.desde,
    hasta: filtros.hasta,
    tipo_articulo: tipoArticulo,
    id_articulo: idArticulo,
    tipo_movimiento: filtros.tipo_movimiento,
    id_proveedor: filtros.id_proveedor,
  }
}

function validarFiltros({ desde, hasta }) {
  if (!desde || !hasta) return 'Ingresá las dos fechas del rango.'
  if (desde > hasta) return 'La fecha "desde" no puede ser posterior a "hasta".'
  return null
}

function descargarArchivo(blob, nombreArchivo) {
  const url = URL.createObjectURL(blob)
  const enlace = document.createElement('a')
  enlace.href = url
  enlace.download = nombreArchivo
  document.body.appendChild(enlace)
  enlace.click()
  enlace.remove()
  URL.revokeObjectURL(url)
}

// Cada barra es un solo artículo: las series se apilan dentro de esa unidad, nunca entre unidades.
function datosGrafico(filas) {
  return filas.map((fila) => ({
    etiqueta: fila.unidad ? `${fila.articulo} (${fila.unidad})` : fila.articulo,
    entradas: Number(fila.entradas ?? 0),
    salidasVenta: Number(fila.salidasVenta ?? 0),
    descartes: Number(fila.descartes ?? 0),
    ajustes: Number(fila.ajustes ?? 0),
    otrasSalidas: Number(fila.otrasSalidas ?? 0),
  }))
}

function ReporteMovimientosStockPage() {
  const [filtros, setFiltros] = useState(filtrosIniciales)
  const [filtrosAplicados, setFiltrosAplicados] = useState(() =>
    aFiltrosApi(filtrosIniciales()),
  )
  const [productos, setProductos] = useState([])
  const [insumos, setInsumos] = useState([])
  const [proveedores, setProveedores] = useState([])
  const [insumosProveedor, setInsumosProveedor] = useState(null)
  const [errorCatalogos, setErrorCatalogos] = useState(null)
  const [reporte, setReporte] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [errorForm, setErrorForm] = useState(null)
  const [errorExportacion, setErrorExportacion] = useState(null)
  const [exportando, setExportando] = useState(null)

  useEffect(() => {
    let cancelado = false

    Promise.all([
      productosService.listar(),
      insumosService.listar(),
      proveedoresService.listar(),
    ])
      .then(([listaProductos, listaInsumos, listaProveedores]) => {
        if (cancelado) return
        setProductos(listaProductos)
        setInsumos(listaInsumos)
        setProveedores(listaProveedores)
      })
      .catch((err) => {
        if (!cancelado) setErrorCatalogos(`No se pudieron cargar los filtros: ${err.message}`)
      })

    return () => {
      cancelado = true
    }
  }, [])

  useEffect(() => {
    const idProveedor = filtros.id_proveedor
    if (!idProveedor) return undefined

    let cancelado = false
    proveedoresService
      .listarInsumos(idProveedor)
      .then((lista) => {
        if (cancelado) return
        const ids = lista.map((asignacion) => asignacion.id_insumo)
        setInsumosProveedor(ids)
        // Si el artículo elegido ya no pertenece al proveedor, se saca del filtro.
        setFiltros((prev) => {
          if (prev.id_proveedor !== idProveedor || !prev.articulo.startsWith('INSUMO:')) {
            return prev
          }
          const idArticulo = Number(prev.articulo.split(':')[1])
          return ids.includes(idArticulo) ? prev : { ...prev, articulo: '' }
        })
      })
      .catch((err) => {
        if (!cancelado) {
          setErrorCatalogos(`No se pudieron cargar los insumos del proveedor: ${err.message}`)
        }
      })

    return () => {
      cancelado = true
    }
  }, [filtros.id_proveedor])

  useEffect(() => {
    let cancelado = false

    reportesService
      .movimientosStock(filtrosAplicados)
      .then((data) => {
        if (!cancelado) {
          setReporte(data)
          setError(null)
        }
      })
      .catch((err) => {
        if (!cancelado) setError(err.message)
      })
      .finally(() => {
        if (!cancelado) setCargando(false)
      })

    return () => {
      cancelado = true
    }
  }, [filtrosAplicados])

  const cambiarFiltro = (campo) => (evento) => {
    setFiltros((prev) => ({ ...prev, [campo]: evento.target.value }))
    setErrorForm(null)
  }

  const cambiarProveedor = (evento) => {
    const idProveedor = evento.target.value
    setInsumosProveedor(null)
    setErrorCatalogos(null)
    setFiltros((prev) => ({
      ...prev,
      id_proveedor: idProveedor,
      articulo:
        idProveedor && prev.articulo.startsWith('PRODUCTO:') ? '' : prev.articulo,
    }))
    setErrorForm(null)
  }

  const aplicarFiltros = (evento) => {
    evento.preventDefault()
    const problema = validarFiltros(filtros)
    if (problema) {
      setErrorForm(problema)
      return
    }
    setErrorForm(null)
    setErrorExportacion(null)
    setCargando(true)
    setFiltrosAplicados(aFiltrosApi(filtros))
  }

  const exportar = async (formato) => {
    setExportando(formato)
    setErrorExportacion(null)
    try {
      const { blob, nombreArchivo } = await reportesService.exportar({
        reporte: 'movimientos-stock',
        formato,
        ...filtrosAplicados,
      })
      descargarArchivo(blob, nombreArchivo || `movimientos-stock.${formato}`)
    } catch (err) {
      setErrorExportacion(err.message)
    } finally {
      setExportando(null)
    }
  }

  const insumosVisibles = insumos.filter(
    (insumo) => !filtros.id_proveedor || insumosProveedor?.includes(insumo.id_insumo),
  )
  const grafico = datosGrafico(reporte?.filas ?? [])
  const cantidadArticulos = reporte?.totales.articulos ?? 0
  const cantidadMovimientos = reporte?.totales.movimientos ?? 0

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Entradas, salidas y descartes de stock</h1>
          <p className="page-subtitle">
            Movimientos del período por artículo, con entradas, salidas por venta, descartes,
            ajustes y otras salidas.
          </p>
        </div>
      </header>

      {errorCatalogos && <div className="banner banner-error">{errorCatalogos}</div>}
      {error && <div className="banner banner-error">{error}</div>}
      {errorForm && <div className="banner banner-error">{errorForm}</div>}
      {errorExportacion && <div className="banner banner-error">{errorExportacion}</div>}

      <form className="toolbar toolbar-auditoria" onSubmit={aplicarFiltros}>
        <div className="toolbar-grupo">
          <label className="toolbar-campo">
            <span>Desde</span>
            <input
              type="date"
              required
              value={filtros.desde}
              onChange={cambiarFiltro('desde')}
            />
          </label>
          <label className="toolbar-campo">
            <span>Hasta</span>
            <input type="date" required value={filtros.hasta} onChange={cambiarFiltro('hasta')} />
          </label>
          <label className="toolbar-campo">
            <span>Artículo</span>
            <select value={filtros.articulo} onChange={cambiarFiltro('articulo')}>
              <option value="">Todos</option>
              {!filtros.id_proveedor && productos.length > 0 && (
                <optgroup label="Productos">
                  {productos.map((producto) => (
                    <option
                      key={`PRODUCTO:${producto.id_producto}`}
                      value={`PRODUCTO:${producto.id_producto}`}
                    >
                      {producto.nombre}
                    </option>
                  ))}
                </optgroup>
              )}
              {insumosVisibles.length > 0 && (
                <optgroup label="Insumos">
                  {insumosVisibles.map((insumo) => (
                    <option
                      key={`INSUMO:${insumo.id_insumo}`}
                      value={`INSUMO:${insumo.id_insumo}`}
                    >
                      {insumo.nombre} ({insumo.unidadMedida})
                    </option>
                  ))}
                </optgroup>
              )}
            </select>
          </label>
          <label className="toolbar-campo">
            <span>Tipo de movimiento</span>
            <select
              value={filtros.tipo_movimiento}
              onChange={cambiarFiltro('tipo_movimiento')}
            >
              {TIPOS_MOVIMIENTO.map((tipo) => (
                <option key={tipo.value} value={tipo.value}>
                  {tipo.label}
                </option>
              ))}
            </select>
          </label>
          <label className="toolbar-campo">
            <span>Proveedor</span>
            <select value={filtros.id_proveedor} onChange={cambiarProveedor}>
              <option value="">Todos</option>
              {proveedores.map((proveedor) => (
                <option key={proveedor.id_proveedor} value={proveedor.id_proveedor}>
                  {proveedor.nombre}
                </option>
              ))}
            </select>
          </label>
        </div>
        <div className="toolbar-acciones">
          <button type="submit" className="btn btn-primary btn-sm" disabled={cargando}>
            Generar reporte
          </button>
        </div>
      </form>

      <p className="aviso">
        Las salidas por venta van a aparecer cuando se implemente VEN-10: hoy no se registra
        ninguna, así que esa columna da 0. Las cantidades se suman solo dentro del mismo artículo y
        unidad; los totales informan artículos y movimientos, nunca suman kg con unidades.
      </p>
      {filtros.id_proveedor && (
        <p className="aviso">
          Al elegir proveedor se ocultan los productos (no tienen proveedor) y solo se listan sus
          insumos asignados. Es la relación vigente, no la del momento del movimiento; un insumo con
          varios proveedores no se duplica.
        </p>
      )}

      {reporte && !error && (
        <>
          <div className="reporte-exportacion">
            <span>Descargar el reporte con estos filtros:</span>
            {FORMATOS_EXPORTACION.map(({ formato, etiqueta }) => (
              <button
                key={formato}
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={() => exportar(formato)}
                disabled={cargando || exportando !== null}
              >
                {exportando === formato ? 'Generando...' : etiqueta}
              </button>
            ))}
          </div>

          <div className="card reporte-grafico">
            <div className="reporte-grafico-titulo">
              <h2>Entradas y salidas por artículo (barras apiladas)</h2>
              <span className="badge">
                {reporte.desde} a {reporte.hasta}
              </span>
            </div>
            {grafico.length > 0 ? (
              <ResponsiveContainer
                width="100%"
                height={Math.max(220, grafico.length * 42 + 60)}
              >
                <BarChart
                  data={grafico}
                  layout="vertical"
                  margin={{ top: 8, right: 24, bottom: 8, left: 8 }}
                >
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis
                    type="number"
                    tickFormatter={(valor) => formatoNumero(valor, 0)}
                    tick={{ fontSize: 11 }}
                  />
                  <YAxis type="category" dataKey="etiqueta" width={160} tick={{ fontSize: 11 }} />
                  <Tooltip formatter={(valor) => formatoNumero(valor, 3)} />
                  <Legend />
                  {SERIES.map((serie) => (
                    <Bar
                      key={serie.clave}
                      dataKey={serie.clave}
                      name={serie.etiqueta}
                      stackId="movimientos"
                      fill={serie.color}
                    />
                  ))}
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <p className="loading">No hay movimientos con estos filtros.</p>
            )}
          </div>

          <div className="card">
            <div className="table-wrapper">
              <table className="table table-stock">
                <thead>
                  <tr>
                    <th>Artículo</th>
                    <th>Unidad</th>
                    <th className="reporte-numero">Entradas</th>
                    <th className="reporte-numero">Salidas por venta</th>
                    <th className="reporte-numero">Descartes</th>
                    <th className="reporte-numero">Ajustes</th>
                    <th className="reporte-numero">Otras salidas</th>
                  </tr>
                </thead>
                <tbody>
                  {reporte.filas.map((fila) => (
                    <tr key={`${fila.tipo}-${fila.id_articulo}`}>
                      <td>{fila.articulo}</td>
                      <td>{fila.unidad ?? '—'}</td>
                      <td className="reporte-numero">{formatoNumero(fila.entradas, 3)}</td>
                      <td className="reporte-numero">{formatoNumero(fila.salidasVenta, 3)}</td>
                      <td className="reporte-numero">{formatoNumero(fila.descartes, 3)}</td>
                      <td className="reporte-numero">{formatoNumero(fila.ajustes, 3)}</td>
                      <td className="reporte-numero">{formatoNumero(fila.otrasSalidas, 3)}</td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr>
                    <th colSpan="2">
                      Totales ({cantidadArticulos}{' '}
                      {cantidadArticulos === 1 ? 'artículo' : 'artículos'}, {cantidadMovimientos}{' '}
                      {cantidadMovimientos === 1 ? 'movimiento' : 'movimientos'})
                    </th>
                    <th className="reporte-numero">—</th>
                    <th className="reporte-numero">—</th>
                    <th className="reporte-numero">—</th>
                    <th className="reporte-numero">—</th>
                    <th className="reporte-numero">—</th>
                  </tr>
                </tfoot>
              </table>
            </div>
            {cargando && <p className="loading">Actualizando el reporte...</p>}
          </div>
        </>
      )}

      {!reporte && !error && cargando && <p className="loading">Generando el reporte...</p>}
    </section>
  )
}

export default ReporteMovimientosStockPage
