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

const FORMATOS_EXPORTACION = [
  { formato: 'xlsx', etiqueta: 'Excel' },
  { formato: 'csv', etiqueta: 'CSV' },
  { formato: 'pdf', etiqueta: 'PDF' },
]
const TIPOS = [
  { value: 'TODOS', label: 'Todos' },
  { value: 'PRODUCTO', label: 'Productos' },
  { value: 'INSUMO', label: 'Insumos' },
]

function filtrosIniciales() {
  return { tipo: 'TODOS', categoria: '', solo_bajo_minimo: false }
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

function datosGrafico(filas) {
  return filas.map((fila) => ({
    etiqueta: fila.articulo,
    stockActual: Number(fila.stockActual ?? 0),
    stockMinimo: Number(fila.stockMinimo ?? 0),
  }))
}

function ReporteStockActualPage() {
  const [filtros, setFiltros] = useState(filtrosIniciales)
  const [filtrosAplicados, setFiltrosAplicados] = useState(filtrosIniciales)
  const [categorias, setCategorias] = useState([])
  const [errorCatalogos, setErrorCatalogos] = useState(null)
  const [reporte, setReporte] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [errorExportacion, setErrorExportacion] = useState(null)
  const [exportando, setExportando] = useState(null)

  useEffect(() => {
    let cancelado = false

    productosService
      .listar()
      .then((lista) => {
        if (cancelado) return
        setCategorias(
          [...new Set(lista.map((producto) => producto.categoria).filter(Boolean))]
            .sort((a, b) => a.localeCompare(b, 'es')),
        )
      })
      .catch((err) => {
        if (!cancelado) setErrorCatalogos(`No se pudieron cargar las categorías: ${err.message}`)
      })

    return () => {
      cancelado = true
    }
  }, [])

  useEffect(() => {
    let cancelado = false

    reportesService
      .stockActual(filtrosAplicados)
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
  }

  const aplicarFiltros = (evento) => {
    evento.preventDefault()
    setErrorExportacion(null)
    setCargando(true)
    setFiltrosAplicados({ ...filtros })
  }

  const exportar = async (formato) => {
    setExportando(formato)
    setErrorExportacion(null)
    try {
      const { blob, nombreArchivo } = await reportesService.exportar({
        reporte: 'stock-actual',
        formato,
        ...filtrosAplicados,
      })
      descargarArchivo(blob, nombreArchivo || `stock-actual.${formato}`)
    } catch (err) {
      setErrorExportacion(err.message)
    } finally {
      setExportando(null)
    }
  }

  const grafico = datosGrafico(reporte?.bajoMinimo ?? [])
  const cantidadArticulos = reporte?.totales.articulos ?? 0
  const cantidadBajoMinimo = reporte?.totales.bajoMinimo ?? 0

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Stock actual y bajo mínimo</h1>
          <p className="page-subtitle">
            Foto de hoy del saldo de productos e insumos, con los artículos que están bajo mínimo.
            No lleva rango de fechas.
          </p>
        </div>
      </header>

      {errorCatalogos && <div className="banner banner-error">{errorCatalogos}</div>}
      {error && <div className="banner banner-error">{error}</div>}
      {errorExportacion && <div className="banner banner-error">{errorExportacion}</div>}

      <form className="toolbar toolbar-auditoria" onSubmit={aplicarFiltros}>
        <div className="toolbar-grupo">
          <label className="toolbar-campo">
            <span>Tipo</span>
            <select value={filtros.tipo} onChange={cambiarFiltro('tipo')}>
              {TIPOS.map((tipo) => (
                <option key={tipo.value} value={tipo.value}>{tipo.label}</option>
              ))}
            </select>
          </label>
          <label className="toolbar-campo">
            <span>Categoría</span>
            <select value={filtros.categoria} onChange={cambiarFiltro('categoria')}>
              <option value="">Todas</option>
              {categorias.map((categoria) => (
                <option key={categoria} value={categoria}>{categoria}</option>
              ))}
            </select>
          </label>
          <label className="toolbar-campo toolbar-check">
            <span>Solo bajo mínimo</span>
            <input
              type="checkbox"
              checked={filtros.solo_bajo_minimo}
              onChange={(evento) =>
                setFiltros((prev) => ({ ...prev, solo_bajo_minimo: evento.target.checked }))
              }
            />
          </label>
        </div>
        <div className="toolbar-acciones">
          <button type="submit" className="btn btn-primary btn-sm" disabled={cargando}>
            Generar reporte
          </button>
        </div>
      </form>

      <p className="aviso">
        Los insumos no tienen categoría: al elegir una categoría se muestran solo productos. Los
        productos no tienen stock mínimo ni unidad cargados: se muestra — y nunca se marcan como
        &quot;bajo mínimo&quot;. Las cantidades no se suman entre unidades distintas.
      </p>

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
              <h2>Stock actual contra mínimo (solo bajo mínimo)</h2>
              <span className="badge">Foto al {reporte.desde}</span>
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
                  <YAxis type="category" dataKey="etiqueta" width={150} tick={{ fontSize: 11 }} />
                  <Tooltip formatter={(valor) => formatoNumero(valor, 3)} />
                  <Legend />
                  <Bar dataKey="stockActual" name="Stock actual" fill="#dc2626" />
                  <Bar dataKey="stockMinimo" name="Stock mínimo" fill="#2563eb" />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <p className="loading">No hay artículos bajo mínimo con estos filtros.</p>
            )}
          </div>

          <div className="card">
            <div className="table-wrapper">
              <table className="table table-stock">
                <thead>
                  <tr>
                    <th>Tipo</th>
                    <th>Artículo</th>
                    <th>Categoría</th>
                    <th>Unidad</th>
                    <th className="reporte-numero">Stock actual</th>
                    <th className="reporte-numero">Stock mínimo</th>
                    <th className="reporte-numero">Falta para el mínimo</th>
                  </tr>
                </thead>
                <tbody>
                  {reporte.filas.map((fila) => (
                    <tr
                      key={`${fila.tipo}-${fila.id_articulo}`}
                      className={fila.bajoMinimo ? 'fila-bajo-minimo' : undefined}
                    >
                      <td>{fila.tipo === 'PRODUCTO' ? 'Producto' : 'Insumo'}</td>
                      <td>
                        {fila.articulo}
                        {fila.bajoMinimo && (
                          <span className="badge badge-alerta">Bajo mínimo</span>
                        )}
                      </td>
                      <td>{fila.categoria ?? '—'}</td>
                      <td>{fila.unidad ?? '—'}</td>
                      <td className="reporte-numero">{formatoNumero(fila.stockActual, 3)}</td>
                      <td className="reporte-numero">
                        {fila.stockMinimo == null ? '—' : formatoNumero(fila.stockMinimo, 0)}
                      </td>
                      <td className="reporte-numero">
                        {fila.falta == null ? '—' : formatoNumero(fila.falta, 3)}
                      </td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr>
                    <th colSpan="4">
                      Totales ({cantidadArticulos}{' '}
                      {cantidadArticulos === 1 ? 'artículo' : 'artículos'}, {cantidadBajoMinimo}{' '}
                      bajo mínimo)
                    </th>
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

export default ReporteStockActualPage
