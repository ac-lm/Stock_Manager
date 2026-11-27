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
import { productosService } from '../services/catalogoService'
import { reportesService } from '../services/reportesService'
import { formatoNumero } from '../utils/format'

// Las ventas se guardan con fecha de calendario; la zona define qué día es "hoy" en el taller.
const ZONA_TALLER = 'America/Argentina/Buenos_Aires'
const FORMATOS_EXPORTACION = [
  { formato: 'xlsx', etiqueta: 'Excel' },
  { formato: 'csv', etiqueta: 'CSV' },
  { formato: 'pdf', etiqueta: 'PDF' },
]
const TOPES_RANKING = [5, 10, 20, 50]

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
    id_producto: '',
    categoria: '',
    top: '10',
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

function ReporteVentasPorProductoPage() {
  const [filtros, setFiltros] = useState(filtrosIniciales)
  const [filtrosAplicados, setFiltrosAplicados] = useState(filtrosIniciales)
  const [productos, setProductos] = useState([])
  const [categorias, setCategorias] = useState([])
  const [errorCatalogos, setErrorCatalogos] = useState(null)
  const [reporte, setReporte] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [errorForm, setErrorForm] = useState(null)
  const [errorExportacion, setErrorExportacion] = useState(null)
  const [exportando, setExportando] = useState(null)

  useEffect(() => {
    let cancelado = false

    productosService
      .listar()
      .then((lista) => {
        if (cancelado) return
        const ordenados = [...lista].sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'))
        setProductos(ordenados)
        setCategorias(
          [...new Set(ordenados.map((producto) => producto.categoria).filter(Boolean))]
            .sort((a, b) => a.localeCompare(b, 'es')),
        )
      })
      .catch((err) => {
        if (!cancelado) setErrorCatalogos(`No se pudieron cargar los productos: ${err.message}`)
      })

    return () => {
      cancelado = true
    }
  }, [])

  useEffect(() => {
    let cancelado = false

    reportesService
      .ventasPorProducto(filtrosAplicados)
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
    setFiltrosAplicados({ ...filtros })
  }

  const exportar = async (formato) => {
    setExportando(formato)
    setErrorExportacion(null)
    try {
      const { blob, nombreArchivo } = await reportesService.exportar({
        reporte: 'ventas-por-producto',
        formato,
        ...filtrosAplicados,
      })
      descargarArchivo(
        blob,
        nombreArchivo
          || `ventas-por-producto_${filtrosAplicados.desde}_${filtrosAplicados.hasta}.${formato}`,
      )
    } catch (err) {
      setErrorExportacion(err.message)
    } finally {
      setExportando(null)
    }
  }

  const cantidadProductos = reporte?.filas.length ?? 0

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Ventas por producto</h1>
          <p className="page-subtitle">
            Cantidad y total vendido por producto de las ventas confirmadas, con ranking de los más
            vendidos.
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
            <input type="date" value={filtros.desde} onChange={cambiarFiltro('desde')} required />
          </label>
          <label className="toolbar-campo">
            <span>Hasta</span>
            <input type="date" value={filtros.hasta} onChange={cambiarFiltro('hasta')} required />
          </label>
          <label className="toolbar-campo">
            <span>Producto</span>
            <select value={filtros.id_producto} onChange={cambiarFiltro('id_producto')}>
              <option value="">Todos</option>
              {productos.map((producto) => (
                <option key={producto.id_producto} value={producto.id_producto}>
                  {producto.nombre}
                </option>
              ))}
            </select>
          </label>
          <label className="toolbar-campo">
            <span>Categoría</span>
            <select value={filtros.categoria} onChange={cambiarFiltro('categoria')}>
              <option value="">Todas</option>
              {categorias.map((categoria) => (
                <option key={categoria} value={categoria}>
                  {categoria}
                </option>
              ))}
            </select>
          </label>
          <label className="toolbar-campo">
            <span>Ranking</span>
            <select value={filtros.top} onChange={cambiarFiltro('top')}>
              {TOPES_RANKING.map((tope) => (
                <option key={tope} value={tope}>
                  Top {tope}
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
              <h2>Productos más vendidos (top {reporte.top})</h2>
              <span className="badge">
                {reporte.desde} a {reporte.hasta}
              </span>
            </div>
            {reporte.ranking.length > 0 ? (
              <ResponsiveContainer
                width="100%"
                height={Math.max(220, reporte.ranking.length * 40 + 60)}
              >
                <BarChart
                  data={reporte.ranking}
                  layout="vertical"
                  margin={{ top: 8, right: 24, bottom: 8, left: 8 }}
                >
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis
                    type="number"
                    tickFormatter={(valor) => formatoNumero(valor, 0)}
                    tick={{ fontSize: 11 }}
                  />
                  <YAxis type="category" dataKey="producto" width={170} tick={{ fontSize: 11 }} />
                  <Tooltip formatter={(valor) => `$ ${formatoNumero(valor)}`} />
                  <Legend />
                  <Bar dataKey="totalVendido" name="Total vendido" fill="#2563eb" />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <p className="loading">No hay productos con ventas confirmadas en el rango.</p>
            )}
          </div>

          <div className="card">
            <div className="table-wrapper">
              <table className="table">
                <thead>
                  <tr>
                    <th>Producto</th>
                    <th>Categoría</th>
                    <th className="reporte-numero">Cantidad vendida</th>
                    <th className="reporte-numero">Total vendido</th>
                  </tr>
                </thead>
                <tbody>
                  {reporte.filas.map((fila) => (
                    <tr key={fila.id_producto}>
                      <td>{fila.producto}</td>
                      <td>{fila.categoria}</td>
                      <td className="reporte-numero">{formatoNumero(fila.cantidadVendida, 3)}</td>
                      <td className="reporte-numero">$ {formatoNumero(fila.totalVendido)}</td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr>
                    <th colSpan="2">
                      Totales ({cantidadProductos} {cantidadProductos === 1 ? 'producto' : 'productos'})
                    </th>
                    <th className="reporte-numero">
                      {formatoNumero(reporte.totales.cantidadVendida, 3)}
                    </th>
                    <th className="reporte-numero">$ {formatoNumero(reporte.totales.totalVendido)}</th>
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

export default ReporteVentasPorProductoPage
