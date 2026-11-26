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
import { reportesService } from '../services/reportesService'
import { formatoNumero } from '../utils/format'

// Las ventas se guardan con fecha de calendario; la zona define qué día es "hoy" en el taller.
const ZONA_TALLER = 'America/Argentina/Buenos_Aires'
const MAX_ANIOS_DIARIOS = 5

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
  return { desde: `${hoy.slice(0, 7)}-01`, hasta: hoy, agrupacion: 'dia' }
}

function dentroDelTopeDiario(desde, hasta) {
  const inicio = new Date(`${desde}T00:00:00Z`)
  inicio.setUTCFullYear(inicio.getUTCFullYear() + MAX_ANIOS_DIARIOS)
  return inicio.toISOString().slice(0, 10) >= hasta
}

function validarFiltros({ desde, hasta, agrupacion }) {
  if (!desde || !hasta) return 'Ingresá las dos fechas del rango.'
  if (desde > hasta) return 'La fecha "desde" no puede ser posterior a "hasta".'
  if (agrupacion === 'dia' && !dentroDelTopeDiario(desde, hasta)) {
    return 'El rango diario no puede superar los 5 años. Agrupá por semana o por mes.'
  }
  return null
}

function etiquetaPeriodo(periodo, agrupacion) {
  if (agrupacion === 'mes') {
    return new Intl.DateTimeFormat('es-AR', {
      month: 'short',
      year: 'numeric',
      timeZone: 'UTC',
    }).format(new Date(`${periodo}-01T00:00:00Z`))
  }
  if (agrupacion === 'semana') return `Semana del ${periodo}`
  return periodo
}

function grafico(fila, agrupacion) {
  return { ...fila, etiqueta: etiquetaPeriodo(fila.periodo, agrupacion) }
}

function ReporteVentasPorPeriodoPage() {
  const [filtros, setFiltros] = useState(filtrosIniciales)
  const [filtrosAplicados, setFiltrosAplicados] = useState(filtrosIniciales)
  const [reporte, setReporte] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [errorForm, setErrorForm] = useState(null)

  useEffect(() => {
    let cancelado = false

    reportesService
      .ventasPorPeriodo(filtrosAplicados)
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
    setCargando(true)
    setFiltrosAplicados({ ...filtros })
  }

  const filasGrafico = reporte ? reporte.filas.map((fila) => grafico(fila, reporte.agrupacion)) : []
  const totalFilas = reporte?.filas.length ?? 0

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Ventas por período</h1>
          <p className="page-subtitle">
            Total vendido, cantidad de ventas y ticket promedio de las ventas confirmadas.
          </p>
        </div>
      </header>

      {error && <div className="banner banner-error">{error}</div>}
      {errorForm && <div className="banner banner-error">{errorForm}</div>}

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
            <span>Agrupar por</span>
            <select value={filtros.agrupacion} onChange={cambiarFiltro('agrupacion')}>
              <option value="dia">Día</option>
              <option value="semana">Semana (desde el lunes)</option>
              <option value="mes">Mes</option>
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
          <div className="card reporte-grafico">
            <div className="reporte-grafico-titulo">
              <h2>Total vendido por {reporte.agrupacion === 'dia' ? 'día' : reporte.agrupacion === 'semana' ? 'semana' : 'mes'}</h2>
              <span className="badge">
                {reporte.desde} a {reporte.hasta}
              </span>
            </div>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={filasGrafico} margin={{ top: 16, right: 16, bottom: 8, left: 8 }}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="etiqueta" tick={{ fontSize: 11 }} minTickGap={24} />
                <YAxis
                  tickFormatter={(valor) => formatoNumero(valor, 0)}
                  tick={{ fontSize: 11 }}
                  width={70}
                />
                <Tooltip formatter={(valor) => `$ ${formatoNumero(valor)}`} />
                <Legend />
                <Bar dataKey="totalVendido" name="Total vendido" fill="#2563eb" />
              </BarChart>
            </ResponsiveContainer>
          </div>

          <div className="card">
            <div className="table-wrapper">
              <table className="table">
                <thead>
                  <tr>
                    <th>Período</th>
                    <th className="reporte-numero">Total vendido</th>
                    <th className="reporte-numero">Cantidad de ventas</th>
                    <th className="reporte-numero">Ticket promedio</th>
                  </tr>
                </thead>
                <tbody>
                  {reporte.filas.map((fila) => (
                    <tr key={fila.periodo}>
                      <td>{etiquetaPeriodo(fila.periodo, reporte.agrupacion)}</td>
                      <td className="reporte-numero">$ {formatoNumero(fila.totalVendido)}</td>
                      <td className="reporte-numero">{fila.cantidadVentas}</td>
                      <td className="reporte-numero">$ {formatoNumero(fila.ticketPromedio)}</td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr>
                    <th>Totales ({totalFilas} {totalFilas === 1 ? 'período' : 'períodos'})</th>
                    <th className="reporte-numero">$ {formatoNumero(reporte.totales.totalVendido)}</th>
                    <th className="reporte-numero">{reporte.totales.cantidadVentas}</th>
                    <th className="reporte-numero">$ {formatoNumero(reporte.totales.ticketPromedio)}</th>
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

export default ReporteVentasPorPeriodoPage
