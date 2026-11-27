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
import { clientesService } from '../services/clientesService'
import { reportesService } from '../services/reportesService'
import { formatoNumero } from '../utils/format'

// Las ventas se guardan con fecha de calendario; la zona define qué día es "hoy" en el taller.
const ZONA_TALLER = 'America/Argentina/Buenos_Aires'
const FORMATOS_EXPORTACION = [
  { formato: 'xlsx', etiqueta: 'Excel' },
  { formato: 'csv', etiqueta: 'CSV' },
  { formato: 'pdf', etiqueta: 'PDF' },
]
const TIPOS_CLIENTE = [
  { valor: 'GRAN_CLIENTE', etiqueta: 'Gran cliente' },
  { valor: 'CONSUMIDOR_FINAL', etiqueta: 'Consumidor final' },
]
// El taller tiene pocos clientes: se cargan todos, incluidos los inactivos, sin paginar.
const TAMANO_LISTA_CLIENTES = 1000

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
    id_cliente: '',
    tipo_cliente: '',
  }
}

function validarFiltros({ desde, hasta }) {
  if (!desde || !hasta) return 'Ingresá las dos fechas del rango.'
  if (desde > hasta) return 'La fecha "desde" no puede ser posterior a "hasta".'
  return null
}

function etiquetaCliente(cliente) {
  const sufijos = []
  if (cliente.tipoCliente === 'GENERICO') sufijos.push('mostrador')
  if (!cliente.activo) sufijos.push('inactivo')
  return sufijos.length ? `${cliente.nombre} (${sufijos.join(', ')})` : cliente.nombre
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

function ReporteVentasPorClientePage() {
  const [filtros, setFiltros] = useState(filtrosIniciales)
  const [filtrosAplicados, setFiltrosAplicados] = useState(filtrosIniciales)
  const [clientes, setClientes] = useState([])
  const [errorCatalogos, setErrorCatalogos] = useState(null)
  const [reporte, setReporte] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [errorForm, setErrorForm] = useState(null)
  const [errorExportacion, setErrorExportacion] = useState(null)
  const [exportando, setExportando] = useState(null)

  useEffect(() => {
    let cancelado = false

    clientesService
      .listar({ size: TAMANO_LISTA_CLIENTES, incluirInactivos: true })
      .then((data) => {
        if (cancelado) return
        const ordenados = [...(data.content ?? [])].sort((a, b) =>
          a.nombre.localeCompare(b.nombre, 'es'),
        )
        setClientes(ordenados)
      })
      .catch((err) => {
        if (!cancelado) setErrorCatalogos(`No se pudieron cargar los clientes: ${err.message}`)
      })

    return () => {
      cancelado = true
    }
  }, [])

  useEffect(() => {
    let cancelado = false

    reportesService
      .ventasPorCliente(filtrosAplicados)
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
        reporte: 'ventas-por-cliente',
        formato,
        ...filtrosAplicados,
      })
      descargarArchivo(
        blob,
        nombreArchivo
          || `ventas-por-cliente_${filtrosAplicados.desde}_${filtrosAplicados.hasta}.${formato}`,
      )
    } catch (err) {
      setErrorExportacion(err.message)
    } finally {
      setExportando(null)
    }
  }

  const cantidadClientes = reporte?.filas.length ?? 0

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Ventas por cliente</h1>
          <p className="page-subtitle">
            Cantidad de compras y total vendido por cliente de las ventas confirmadas, con el resumen
            por tipo de cliente.
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
            <span>Cliente</span>
            <select value={filtros.id_cliente} onChange={cambiarFiltro('id_cliente')}>
              <option value="">Todos (incluye inactivos)</option>
              {clientes.map((cliente) => (
                <option key={cliente.id_cliente} value={cliente.id_cliente}>
                  {etiquetaCliente(cliente)}
                </option>
              ))}
            </select>
          </label>
          <label className="toolbar-campo">
            <span>Tipo de cliente</span>
            <select value={filtros.tipo_cliente} onChange={cambiarFiltro('tipo_cliente')}>
              <option value="">Todos</option>
              {TIPOS_CLIENTE.map(({ valor, etiqueta }) => (
                <option key={valor} value={valor}>
                  {etiqueta}
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
              <h2>Total por tipo de cliente</h2>
              <span className="badge">
                {reporte.desde} a {reporte.hasta}
              </span>
            </div>
            <div className="table-wrapper">
              <table className="table">
                <thead>
                  <tr>
                    <th>Tipo</th>
                    <th className="reporte-numero">Cantidad de compras</th>
                    <th className="reporte-numero">Total vendido</th>
                  </tr>
                </thead>
                <tbody>
                  {reporte.porTipo.map((tipo) => (
                    <tr key={tipo.tipo_cliente}>
                      <td>{tipo.tipo}</td>
                      <td className="reporte-numero">
                        {formatoNumero(tipo.cantidadCompras, 0)}
                      </td>
                      <td className="reporte-numero">$ {formatoNumero(tipo.totalVendido)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {reporte.porTipo.some((tipo) => Number(tipo.totalVendido) > 0) ? (
              <ResponsiveContainer width="100%" height={260}>
                <BarChart
                  data={reporte.porTipo}
                  margin={{ top: 16, right: 16, bottom: 8, left: 8 }}
                >
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="tipo" tick={{ fontSize: 11 }} />
                  <YAxis
                    tickFormatter={(valor) => formatoNumero(valor, 0)}
                    tick={{ fontSize: 11 }}
                  />
                  <Tooltip formatter={(valor) => `$ ${formatoNumero(valor)}`} />
                  <Legend />
                  <Bar dataKey="totalVendido" name="Total vendido" fill="#2563eb" />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <p className="loading">No hay ventas confirmadas en el rango.</p>
            )}
          </div>

          <div className="card">
            <div className="table-wrapper">
              <table className="table">
                <thead>
                  <tr>
                    <th>Cliente</th>
                    <th>Tipo</th>
                    <th className="reporte-numero">Cantidad de compras</th>
                    <th className="reporte-numero">Total vendido</th>
                  </tr>
                </thead>
                <tbody>
                  {reporte.filas.map((fila) => (
                    <tr key={fila.id_cliente}>
                      <td>{fila.cliente}</td>
                      <td>{fila.tipo}</td>
                      <td className="reporte-numero">
                        {formatoNumero(fila.cantidadCompras, 0)}
                      </td>
                      <td className="reporte-numero">$ {formatoNumero(fila.totalVendido)}</td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr>
                    <th colSpan="2">
                      Totales ({cantidadClientes} {cantidadClientes === 1 ? 'cliente' : 'clientes'})
                    </th>
                    <th className="reporte-numero">
                      {formatoNumero(reporte.totales.cantidadCompras, 0)}
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

export default ReporteVentasPorClientePage
