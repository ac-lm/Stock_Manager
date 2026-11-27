import { useEffect, useState } from 'react'
import {
  Bar,
  CartesianGrid,
  ComposedChart,
  Legend,
  Line,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { reportesService } from '../services/reportesService'
import { formatoNumero } from '../utils/format'

const ZONA_TALLER = 'America/Argentina/Buenos_Aires'
const FORMATOS_EXPORTACION = [
  { formato: 'xlsx', etiqueta: 'Excel' },
  { formato: 'csv', etiqueta: 'CSV' },
  { formato: 'pdf', etiqueta: 'PDF' },
]

function mesActualEnTaller() {
  const partes = new Intl.DateTimeFormat('es-AR', {
    timeZone: ZONA_TALLER,
    year: 'numeric',
    month: '2-digit',
  }).formatToParts(new Date())
  const valor = (tipo) => partes.find((parte) => parte.type === tipo).value
  return `${valor('year')}-${valor('month')}`
}

function filtrosIniciales() {
  const mesActual = mesActualEnTaller()
  return {
    desde: `${mesActual.slice(0, 4)}-01`,
    hasta: mesActual,
  }
}

function validarFiltros({ desde, hasta }) {
  if (!desde || !hasta) return 'Ingresá los dos períodos del rango.'
  if (desde > hasta) return 'El período "desde" no puede ser posterior a "hasta".'
  const [anioDesde, mesDesde] = desde.split('-').map(Number)
  const [anioHasta, mesHasta] = hasta.split('-').map(Number)
  const cantidadMeses = (anioHasta - anioDesde) * 12 + (mesHasta - mesDesde) + 1
  if (cantidadMeses > 36) return 'El rango no puede superar los 36 meses.'
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

function datosGrafico(reporte) {
  return (reporte?.filas ?? []).map((fila) => ({
    etiqueta: fila.etiqueta,
    vendido: Number(fila.totalVendido ?? 0),
    costos: Number(fila.totalCostos ?? 0),
    resultado: Number(fila.resultado ?? 0),
  }))
}

function ReporteResultadoPeriodoPage() {
  const [filtros, setFiltros] = useState(filtrosIniciales)
  const [filtrosAplicados, setFiltrosAplicados] = useState(() => filtrosIniciales())
  const [reporte, setReporte] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [errorForm, setErrorForm] = useState(null)
  const [errorExportacion, setErrorExportacion] = useState(null)
  const [exportando, setExportando] = useState(null)

  useEffect(() => {
    let cancelado = false

    reportesService
      .resultadoPeriodo(filtrosAplicados)
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
        reporte: 'resultado-periodo',
        formato,
        ...filtrosAplicados,
      })
      descargarArchivo(blob, nombreArchivo || `resultado-periodo.${formato}`)
    } catch (err) {
      setErrorExportacion(err.message)
    } finally {
      setExportando(null)
    }
  }

  const grafico = datosGrafico(reporte)
  const totales = reporte?.totales

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Resultado del período (ventas menos costos)</h1>
          <p className="page-subtitle">
            Ventas confirmadas por fecha de venta contra costos del período, mes a mes.
          </p>
        </div>
      </header>

      {error && <div className="banner banner-error">{error}</div>}
      {errorForm && <div className="banner banner-error">{errorForm}</div>}
      {errorExportacion && <div className="banner banner-error">{errorExportacion}</div>}

      <form className="toolbar toolbar-auditoria" onSubmit={aplicarFiltros}>
        <div className="toolbar-grupo">
          <label className="toolbar-campo">
            <span>Desde</span>
            <input
              type="month"
              required
              value={filtros.desde}
              onChange={cambiarFiltro('desde')}
            />
          </label>
          <label className="toolbar-campo">
            <span>Hasta</span>
            <input type="month" required value={filtros.hasta} onChange={cambiarFiltro('hasta')} />
          </label>
        </div>
        <div className="toolbar-acciones">
          <button type="submit" className="btn btn-primary btn-sm" disabled={cargando}>
            Generar reporte
          </button>
        </div>
      </form>

      {reporte?.aclaracion && <div className="aviso">{reporte.aclaracion}</div>}
      {reporte?.avisoInsumos && <div className="aviso">{reporte.avisoInsumos}</div>}
      <p className="aviso">
        El rango es obligatorio y no puede superar los 36 meses. Las ventas se agrupan por fecha
        de venta (solo confirmadas) y los costos por mes y año del período, sin importar la fecha
        de pago; los anulados quedan excluidos. Los números coinciden con REP‑01 y REP‑10.
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
              <h2>Vendido contra costos, y resultado</h2>
              <span className="badge">
                {reporte.desdePeriodo} a {reporte.hastaPeriodo}
              </span>
            </div>
            {grafico.length > 0 ? (
              <ResponsiveContainer width="100%" height={340}>
                <ComposedChart data={grafico} margin={{ top: 8, right: 24, bottom: 8, left: 8 }}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="etiqueta" tick={{ fontSize: 11 }} />
                  <YAxis
                    tickFormatter={(valor) => formatoNumero(valor, 0)}
                    tick={{ fontSize: 11 }}
                  />
                  <Tooltip formatter={(valor) => formatoNumero(valor, 2)} />
                  <ReferenceLine y={0} stroke="#94a3b8" />
                  <Legend />
                  <Bar dataKey="vendido" name="Total vendido" fill="#2563eb" />
                  <Bar dataKey="costos" name="Total costos" fill="#f59e0b" />
                  <Line
                    type="monotone"
                    dataKey="resultado"
                    name="Resultado"
                    stroke="#dc2626"
                    strokeWidth={2}
                    dot={{ r: 3 }}
                  />
                </ComposedChart>
              </ResponsiveContainer>
            ) : (
              <p className="loading">No hay datos con estos filtros.</p>
            )}
          </div>

          <div className="card">
            <div className="table-wrapper">
              <table className="table table-stock">
                <thead>
                  <tr>
                    <th>Período</th>
                    <th className="reporte-numero">Total vendido</th>
                    <th className="reporte-numero">Total costos</th>
                    <th className="reporte-numero">Resultado</th>
                  </tr>
                </thead>
                <tbody>
                  {reporte.filas.map((fila) => (
                    <tr key={fila.periodo}>
                      <td>{fila.etiqueta}</td>
                      <td className="reporte-numero">{formatoNumero(fila.totalVendido, 2)}</td>
                      <td className="reporte-numero">{formatoNumero(fila.totalCostos, 2)}</td>
                      <td
                        className={`reporte-numero ${
                          Number(fila.resultado) < 0 ? 'monto-negativo' : ''
                        }`}
                      >
                        {formatoNumero(fila.resultado, 2)}
                      </td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr>
                    <th>Totales</th>
                    <th className="reporte-numero">{formatoNumero(totales.totalVendido, 2)}</th>
                    <th className="reporte-numero">{formatoNumero(totales.totalCostos, 2)}</th>
                    <th
                      className={`reporte-numero ${
                        Number(totales.resultado) < 0 ? 'monto-negativo' : ''
                      }`}
                    >
                      {formatoNumero(totales.resultado, 2)}
                    </th>
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

export default ReporteResultadoPeriodoPage
