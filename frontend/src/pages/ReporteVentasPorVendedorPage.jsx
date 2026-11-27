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
import { usuariosService } from '../services/usuariosService'
import { formatoNumero } from '../utils/format'

// Las ventas se guardan con fecha de calendario; la zona define qué día es "hoy" en el taller.
const ZONA_TALLER = 'America/Argentina/Buenos_Aires'
const FORMATOS_EXPORTACION = [
  { formato: 'xlsx', etiqueta: 'Excel' },
  { formato: 'csv', etiqueta: 'CSV' },
  { formato: 'pdf', etiqueta: 'PDF' },
]
// El taller tiene pocos usuarios: se cargan todos, incluidos los inactivos, sin paginar.
const TAMANO_LISTA_USUARIOS = 1000

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
    id_vendedor: '',
  }
}

function validarFiltros({ desde, hasta }) {
  if (!desde || !hasta) return 'Ingresá las dos fechas del rango.'
  if (desde > hasta) return 'La fecha "desde" no puede ser posterior a "hasta".'
  return null
}

function etiquetaUsuario(usuario) {
  return `${usuario.nombre} (${usuario.rol}${usuario.activo ? '' : ', inactivo'})`
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

function ReporteVentasPorVendedorPage() {
  const [filtros, setFiltros] = useState(filtrosIniciales)
  const [filtrosAplicados, setFiltrosAplicados] = useState(filtrosIniciales)
  const [usuarios, setUsuarios] = useState([])
  const [errorCatalogos, setErrorCatalogos] = useState(null)
  const [reporte, setReporte] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [errorForm, setErrorForm] = useState(null)
  const [errorExportacion, setErrorExportacion] = useState(null)
  const [exportando, setExportando] = useState(null)

  useEffect(() => {
    let cancelado = false

    usuariosService
      .listar({ size: TAMANO_LISTA_USUARIOS, incluirInactivos: true })
      .then((data) => {
        if (cancelado) return
        const ordenados = [...(data.content ?? [])].sort((a, b) =>
          a.nombre.localeCompare(b.nombre, 'es'),
        )
        setUsuarios(ordenados)
      })
      .catch((err) => {
        if (!cancelado) setErrorCatalogos(`No se pudieron cargar los usuarios: ${err.message}`)
      })

    return () => {
      cancelado = true
    }
  }, [])

  useEffect(() => {
    let cancelado = false

    reportesService
      .ventasPorVendedor(filtrosAplicados)
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
        reporte: 'ventas-por-vendedor',
        formato,
        ...filtrosAplicados,
      })
      descargarArchivo(
        blob,
        nombreArchivo
          || `ventas-por-vendedor_${filtrosAplicados.desde}_${filtrosAplicados.hasta}.${formato}`,
      )
    } catch (err) {
      setErrorExportacion(err.message)
    } finally {
      setExportando(null)
    }
  }

  const cantidadVendedores = reporte?.filas.length ?? 0

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Ventas por vendedor</h1>
          <p className="page-subtitle">
            Cantidad de ventas, total vendido y ticket promedio por usuario que registró la venta,
            solo para las ventas confirmadas del rango.
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
            <span>Vendedor</span>
            <select value={filtros.id_vendedor} onChange={cambiarFiltro('id_vendedor')}>
              <option value="">Todos (incluye inactivos)</option>
              {usuarios.map((usuario) => (
                <option key={usuario.id_usuario} value={usuario.id_usuario}>
                  {etiquetaUsuario(usuario)}
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
              <h2>Total vendido por vendedor</h2>
              <span className="badge">
                {reporte.desde} a {reporte.hasta}
              </span>
            </div>
            {reporte.filas.length > 0 ? (
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={reporte.filas} margin={{ top: 16, right: 16, bottom: 8, left: 8 }}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="vendedor" tick={{ fontSize: 11 }} minTickGap={24} />
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
            ) : (
              <p className="loading">No hay ventas confirmadas en el rango.</p>
            )}
          </div>

          <div className="card">
            <div className="table-wrapper">
              <table className="table">
                <thead>
                  <tr>
                    <th>Vendedor</th>
                    <th className="reporte-numero">Cantidad de ventas</th>
                    <th className="reporte-numero">Total vendido</th>
                    <th className="reporte-numero">Ticket promedio</th>
                  </tr>
                </thead>
                <tbody>
                  {reporte.filas.map((fila) => (
                    <tr key={fila.id_usuario}>
                      <td>{fila.vendedor}</td>
                      <td className="reporte-numero">{formatoNumero(fila.cantidadVentas, 0)}</td>
                      <td className="reporte-numero">$ {formatoNumero(fila.totalVendido)}</td>
                      <td className="reporte-numero">$ {formatoNumero(fila.ticketPromedio)}</td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr>
                    <th>
                      Totales ({cantidadVendedores}{' '}
                      {cantidadVendedores === 1 ? 'vendedor' : 'vendedores'})
                    </th>
                    <th className="reporte-numero">
                      {formatoNumero(reporte.totales.cantidadVentas, 0)}
                    </th>
                    <th className="reporte-numero">$ {formatoNumero(reporte.totales.totalVendido)}</th>
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

export default ReporteVentasPorVendedorPage
