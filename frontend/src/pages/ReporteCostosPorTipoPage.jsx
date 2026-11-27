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
import { reportesService } from '../services/reportesService'
import { formatoNumero } from '../utils/format'

const ZONA_TALLER = 'America/Argentina/Buenos_Aires'
const FORMATOS_EXPORTACION = [
  { formato: 'xlsx', etiqueta: 'Excel' },
  { formato: 'csv', etiqueta: 'CSV' },
  { formato: 'pdf', etiqueta: 'PDF' },
]
const COLORES = [
  '#2563eb',
  '#16a34a',
  '#dc2626',
  '#f59e0b',
  '#7c3aed',
  '#0891b2',
  '#db2777',
  '#65a30d',
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
    id_tipo_costo: '',
  }
}

function aFiltrosApi(filtros) {
  return {
    desde: filtros.desde,
    hasta: filtros.hasta,
    id_tipo_costo: filtros.id_tipo_costo,
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
  const tipos = reporte?.tipos ?? []
  return (reporte?.filas ?? []).map((fila) => {
    const punto = { etiqueta: fila.etiqueta }
    tipos.forEach((tipo) => {
      punto[`tipo_${tipo.id}`] = Number(fila.valoresPorTipo?.[String(tipo.id)] ?? 0)
    })
    return punto
  })
}

function ReporteCostosPorTipoPage() {
  const [filtros, setFiltros] = useState(filtrosIniciales)
  const [filtrosAplicados, setFiltrosAplicados] = useState(() =>
    aFiltrosApi(filtrosIniciales()),
  )
  const [reporte, setReporte] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [errorForm, setErrorForm] = useState(null)
  const [errorExportacion, setErrorExportacion] = useState(null)
  const [exportando, setExportando] = useState(null)

  useEffect(() => {
    let cancelado = false

    reportesService
      .costosPorTipo(filtrosAplicados)
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
    setFiltrosAplicados(aFiltrosApi(filtros))
  }

  const exportar = async (formato) => {
    setExportando(formato)
    setErrorExportacion(null)
    try {
      const { blob, nombreArchivo } = await reportesService.exportar({
        reporte: 'costos-por-tipo',
        formato,
        ...filtrosAplicados,
      })
      descargarArchivo(blob, nombreArchivo || `costos-por-tipo.${formato}`)
    } catch (err) {
      setErrorExportacion(err.message)
    } finally {
      setExportando(null)
    }
  }

  const grafico = datosGrafico(reporte)
  const tipos = reporte?.tipos ?? []
  const opciones = reporte?.tiposDisponibles ?? []

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Costos por tipo y su evolución</h1>
          <p className="page-subtitle">
            Total por tipo y mes, con los meses sin costos completados en 0 y una línea por tipo.
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
          <label className="toolbar-campo">
            <span>Tipo de costo</span>
            <select value={filtros.id_tipo_costo} onChange={cambiarFiltro('id_tipo_costo')}>
              <option value="">Todos</option>
              {opciones.map((tipo) => (
                <option key={tipo.id} value={tipo.id}>
                  {tipo.nombre}
                  {tipo.reservado ? ' (del sistema)' : tipo.activo ? '' : ' (inactivo)'}
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
        Los costos se agrupan por mes y año (período), no por fecha de pago: un costo de octubre
        pagado en noviembre cuenta en octubre. Los anulados quedan excluidos. El rango es
        obligatorio y no puede superar los 36 meses.
      </p>
      {reporte?.avisoInsumos && <p className="aviso">{reporte.avisoInsumos}</p>}

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
              <h2>Evolución por tipo de costo</h2>
              <span className="badge">
                {reporte.desdePeriodo} a {reporte.hastaPeriodo}
              </span>
            </div>
            {grafico.length > 0 && tipos.length > 0 ? (
              <ResponsiveContainer width="100%" height={Math.max(260, tipos.length * 60 + 120)}>
                <LineChart data={grafico} margin={{ top: 8, right: 24, bottom: 8, left: 8 }}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="etiqueta" tick={{ fontSize: 11 }} />
                  <YAxis
                    tickFormatter={(valor) => formatoNumero(valor, 0)}
                    tick={{ fontSize: 11 }}
                  />
                  <Tooltip formatter={(valor) => formatoNumero(valor, 2)} />
                  <Legend />
                  {tipos.map((tipo, indice) => (
                    <Line
                      key={tipo.id}
                      type="monotone"
                      dataKey={`tipo_${tipo.id}`}
                      name={tipo.nombre}
                      stroke={COLORES[indice % COLORES.length]}
                      strokeWidth={2}
                      dot={{ r: 3 }}
                      connectNulls
                    />
                  ))}
                </LineChart>
              </ResponsiveContainer>
            ) : (
              <p className="loading">No hay costos con estos filtros.</p>
            )}
          </div>

          <div className="card">
            <div className="table-wrapper">
              <table className="table table-stock">
                <thead>
                  <tr>
                    <th>Período</th>
                    {tipos.map((tipo) => (
                      <th key={tipo.id} className="reporte-numero">
                        {tipo.nombre}
                        {tipo.reservado ? ' (del sistema)' : ''}
                      </th>
                    ))}
                    <th className="reporte-numero">Total</th>
                  </tr>
                </thead>
                <tbody>
                  {reporte.filas.map((fila) => (
                    <tr key={fila.periodo}>
                      <td>{fila.etiqueta}</td>
                      {tipos.map((tipo) => (
                        <td key={tipo.id} className="reporte-numero">
                          {formatoNumero(fila.valoresPorTipo?.[String(tipo.id)] ?? 0, 2)}
                        </td>
                      ))}
                      <td className="reporte-numero">{formatoNumero(fila.total, 2)}</td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr>
                    <th>Totales</th>
                    {tipos.map((tipo) => (
                      <th key={tipo.id} className="reporte-numero">
                        {formatoNumero(tipo.total, 2)}
                      </th>
                    ))}
                    <th className="reporte-numero">
                      {formatoNumero(reporte.totales.totalGeneral, 2)}
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

export default ReporteCostosPorTipoPage
