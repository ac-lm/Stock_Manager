import { useEffect, useState } from 'react'
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
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
const MAX_ANIOS = 5
const CANTIDAD_ANIOS = 10
const COLORES = ['#2563eb', '#dc2626', '#059669', '#d97706', '#7c3aed']
const FORMATOS_EXPORTACION = [
  { formato: 'xlsx', etiqueta: 'Excel' },
  { formato: 'csv', etiqueta: 'CSV' },
  { formato: 'pdf', etiqueta: 'PDF' },
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
  const anioActual = Number(fechaHoyEnTaller().slice(0, 4))
  return {
    anios: [anioActual, anioActual - 1, anioActual - 2],
    id_producto: '',
    categoria: '',
  }
}

function validarFiltros({ anios }) {
  if (anios.length === 0) return 'Elegí al menos un año para comparar.'
  if (anios.length > MAX_ANIOS) return `Se pueden comparar hasta ${MAX_ANIOS} años.`
  return null
}

function textoVariacion(valor) {
  return valor == null ? '—' : `${formatoNumero(valor, 1)} %`
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

function ReporteComparacionEstacionalPage() {
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

  const anioActual = Number(fechaHoyEnTaller().slice(0, 4))
  const aniosDisponibles = Array.from({ length: CANTIDAD_ANIOS }, (_, indice) => anioActual - indice)

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
      .comparacionEstacional(filtrosAplicados)
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

  const alternarAnio = (anio) => {
    setFiltros((prev) => {
      const seleccionados = prev.anios.includes(anio)
        ? prev.anios.filter((seleccionado) => seleccionado !== anio)
        : [...prev.anios, anio]
      return { ...prev, anios: seleccionados.sort((a, b) => b - a) }
    })
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
    setFiltrosAplicados({ ...filtros, anios: [...filtros.anios] })
  }

  const exportar = async (formato) => {
    setExportando(formato)
    setErrorExportacion(null)
    try {
      const { blob, nombreArchivo } = await reportesService.exportar({
        reporte: 'evolucion-ventas',
        formato,
        ...filtrosAplicados,
      })
      descargarArchivo(
        blob,
        nombreArchivo
          || `evolucion-ventas_${reporte.desde}_${reporte.hasta}.${formato}`,
      )
    } catch (err) {
      setErrorExportacion(err.message)
    } finally {
      setExportando(null)
    }
  }

  const aniosDelReporte = reporte?.anios ?? []
  const aniosVariacion = aniosDelReporte.slice(1)
  const datosGrafico = reporte
    ? reporte.filas.map((fila) => {
      const punto = { etiqueta: fila.etiqueta }
      for (const anio of aniosDelReporte) {
        punto[String(anio)] = fila.totalesPorAnio[String(anio)] ?? 0
      }
      return punto
    })
    : []

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Evolución de ventas y comparación estacional</h1>
          <p className="page-subtitle">
            Total vendido por mes de las ventas confirmadas, con una línea por año. Los meses sin
            ventas valen 0 y los meses que todavía no pasaron no se muestran.
          </p>
        </div>
      </header>

      {errorCatalogos && <div className="banner banner-error">{errorCatalogos}</div>}
      {error && <div className="banner banner-error">{error}</div>}
      {errorForm && <div className="banner banner-error">{errorForm}</div>}
      {errorExportacion && <div className="banner banner-error">{errorExportacion}</div>}

      <form className="toolbar toolbar-auditoria" onSubmit={aplicarFiltros}>
        <div className="toolbar-grupo">
          <div className="toolbar-campo">
            <span>Años a comparar (máx. {MAX_ANIOS})</span>
            <div className="toolbar-anios">
              {aniosDisponibles.map((anio) => (
                <label key={anio}>
                  <input
                    type="checkbox"
                    checked={filtros.anios.includes(anio)}
                    disabled={filtros.anios.length >= MAX_ANIOS && !filtros.anios.includes(anio)}
                    onChange={() => alternarAnio(anio)}
                  />
                  {anio}
                </label>
              ))}
            </div>
          </div>
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
              <h2>Total vendido por mes y año</h2>
              <span className="badge">
                {reporte.desde} a {reporte.hasta}
              </span>
            </div>
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={datosGrafico} margin={{ top: 16, right: 16, bottom: 8, left: 8 }}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="etiqueta" tick={{ fontSize: 11 }} />
                <YAxis
                  tickFormatter={(valor) => formatoNumero(valor, 0)}
                  tick={{ fontSize: 11 }}
                  width={70}
                />
                <Tooltip formatter={(valor) => `$ ${formatoNumero(valor)}`} />
                <Legend />
                {aniosDelReporte.map((anio, indice) => (
                  <Line
                    key={anio}
                    type="monotone"
                    dataKey={String(anio)}
                    name={String(anio)}
                    stroke={COLORES[indice % COLORES.length]}
                    strokeWidth={2}
                    dot={{ r: 2 }}
                  />
                ))}
              </LineChart>
            </ResponsiveContainer>
          </div>

          <div className="card">
            <div className="table-wrapper">
              <table className="table">
                <thead>
                  <tr>
                    <th>Mes</th>
                    {aniosDelReporte.map((anio) => (
                      <th key={anio} className="reporte-numero">{anio}</th>
                    ))}
                    {aniosVariacion.map((anio, indice) => (
                      <th key={`variacion-${anio}`} className="reporte-numero">
                        Var. {anio}/{aniosDelReporte[indice]}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {reporte.filas.map((fila) => (
                    <tr key={fila.mes}>
                      <td>{fila.etiqueta}</td>
                      {aniosDelReporte.map((anio) => (
                        <td key={anio} className="reporte-numero">
                          $ {formatoNumero(fila.totalesPorAnio[String(anio)])}
                        </td>
                      ))}
                      {aniosVariacion.map((anio) => (
                        <td key={`variacion-${anio}`} className="reporte-numero">
                          {textoVariacion(fila.variacionesPorAnio[String(anio)])}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr>
                    <th>Totales</th>
                    {aniosDelReporte.map((anio) => (
                      <th key={anio} className="reporte-numero">
                        $ {formatoNumero(reporte.totales.totalesPorAnio[String(anio)])}
                      </th>
                    ))}
                    {aniosVariacion.map((anio) => (
                      <th key={`variacion-${anio}`} className="reporte-numero">
                        {textoVariacion(reporte.totales.variacionesPorAnio[String(anio)])}
                      </th>
                    ))}
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

export default ReporteComparacionEstacionalPage
