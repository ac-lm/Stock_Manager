import { useEffect, useState } from 'react'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { reportesService } from '../services/reportesService'
import { usuariosService } from '../services/usuariosService'
import { claseEstado, etiquetaEstado, etiquetaMedio, ESTADOS_CAJA, MEDIOS_PAGO } from '../utils/caja'
import { formatoNumero } from '../utils/format'

const ZONA_TALLER = 'America/Argentina/Buenos_Aires'
const FORMATOS_EXPORTACION = [
  { formato: 'xlsx', etiqueta: 'Excel' },
  { formato: 'csv', etiqueta: 'CSV' },
  { formato: 'pdf', etiqueta: 'PDF' },
]
const TAMANO_LISTA_USUARIOS = 1000
const COLUMNAS_MEDIO = [
  { clave: 'esperado', titulo: 'Esperado' },
  { clave: 'declarado', titulo: 'Declarado' },
  { clave: 'diferencia', titulo: 'Diferencia' },
  { clave: 'ingresos', titulo: 'Ingresos' },
  { clave: 'egresos', titulo: 'Egresos' },
  { clave: 'faltante', titulo: 'Faltante' },
  { clave: 'sobrante', titulo: 'Sobrante' },
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
    id_usuario_cierre: '',
    estado: 'TODOS',
    medio_pago: 'TODOS',
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

function fechaHora(fecha, hora) {
  if (!fecha) return '—'
  return hora ? `${fecha} ${String(hora).slice(0, 5)}` : fecha
}

function sumaMedios(valoresPorMedio, medios, clave) {
  return medios.reduce((total, medio) => total + Number(valoresPorMedio?.[medio]?.[clave] ?? 0), 0)
}

function datosGrafico(filas, medios) {
  return filas.map((fila) => ({
    etiqueta: `Sesión ${fila.id_sesion}`,
    // Las faltantes se grafican hacia abajo para que el signo se lea de un vistazo.
    faltante: -sumaMedios(fila.valoresPorMedio, medios, 'faltante'),
    sobrante: sumaMedios(fila.valoresPorMedio, medios, 'sobrante'),
    pendiente: fila.pendiente,
  }))
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

function ReporteCierresCajaPage() {
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
      .cierresCaja(filtrosAplicados)
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
        reporte: 'cierres-caja',
        formato,
        ...filtrosAplicados,
      })
      descargarArchivo(
        blob,
        nombreArchivo
          || `cierres-caja_${filtrosAplicados.desde}_${filtrosAplicados.hasta}.${formato}`,
      )
    } catch (err) {
      setErrorExportacion(err.message)
    } finally {
      setExportando(null)
    }
  }

  const medios = reporte?.medios ?? []
  const grafico = reporte ? datosGrafico(reporte.filas, medios) : []
  const cantSesiones = reporte?.totales.cantidadSesiones ?? 0
  const estados = ESTADOS_CAJA.filter((estado) => estado.value !== 'ABIERTA')

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Cierres de caja</h1>
          <p className="page-subtitle">
            Sesiones cerradas y pendientes de revisión según fecha de apertura, con lo esperado,
            lo declarado, los movimientos y las diferencias por medio de pago.
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
            <span>Usuario que cerró</span>
            <select
              value={filtros.id_usuario_cierre}
              onChange={cambiarFiltro('id_usuario_cierre')}
            >
              <option value="">Todos (incluye inactivos)</option>
              {usuarios.map((usuario) => (
                <option key={usuario.id_usuario} value={usuario.id_usuario}>
                  {etiquetaUsuario(usuario)}
                </option>
              ))}
            </select>
          </label>
          <label className="toolbar-campo">
            <span>Estado</span>
            <select value={filtros.estado} onChange={cambiarFiltro('estado')}>
              <option value="TODOS">Todas (cerradas y pendientes)</option>
              {estados.map((estado) => (
                <option key={estado.value} value={estado.value}>{estado.label}</option>
              ))}
            </select>
          </label>
          <label className="toolbar-campo">
            <span>Medio de pago</span>
            <select value={filtros.medio_pago} onChange={cambiarFiltro('medio_pago')}>
              <option value="TODOS">Todos</option>
              {MEDIOS_PAGO.map((medio) => (
                <option key={medio.value} value={medio.value}>{medio.label}</option>
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
              <h2>Faltantes y sobrantes por sesión</h2>
              <span className="badge">{reporte.desde} a {reporte.hasta}</span>
              <span className="aviso">
                Las sesiones pendientes de revisión se resaltan en el gráfico y en la tabla.
              </span>
            </div>
            {grafico.length > 0 ? (
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={grafico} margin={{ top: 16, right: 16, bottom: 8, left: 8 }}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="etiqueta" tick={{ fontSize: 11 }} minTickGap={16} />
                  <YAxis
                    tickFormatter={(valor) => formatoNumero(valor, 0)}
                    tick={{ fontSize: 11 }}
                    width={70}
                  />
                  <Tooltip
                    formatter={(valor, nombre) => [`$ ${formatoNumero(Math.abs(valor))}`, nombre]}
                  />
                  <Legend />
                  <Bar dataKey="faltante" name="Faltante" stackId="diferencia" fill="#dc2626">
                    {grafico.map((punto) => (
                      <Cell
                        key={punto.etiqueta}
                        fill={punto.pendiente ? '#b45309' : '#dc2626'}
                        stroke={punto.pendiente ? '#92400e' : 'none'}
                        strokeWidth={punto.pendiente ? 2 : 0}
                      />
                    ))}
                  </Bar>
                  <Bar dataKey="sobrante" name="Sobrante" stackId="diferencia" fill="#16a34a">
                    {grafico.map((punto) => (
                      <Cell
                        key={punto.etiqueta}
                        fill={punto.pendiente ? '#b45309' : '#16a34a'}
                        stroke={punto.pendiente ? '#92400e' : 'none'}
                        strokeWidth={punto.pendiente ? 2 : 0}
                      />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <p className="loading">No hay cierres en el rango.</p>
            )}
          </div>

          <div className="card">
            <div className="table-wrapper">
              <table className="table table-cierres">
                <thead>
                  <tr>
                    <th>Sesión</th>
                    <th>Apertura</th>
                    <th>Cierre</th>
                    <th>Abrió</th>
                    <th>Cerró</th>
                    <th className="reporte-numero">Monto inicial</th>
                    <th>Estado</th>
                    {medios.flatMap((medio) => COLUMNAS_MEDIO.map((columna) => (
                      <th key={`${medio}-${columna.clave}`} className="reporte-numero">
                        {columna.titulo} {etiquetaMedio(medio)}
                      </th>
                    )))}
                  </tr>
                </thead>
                <tbody>
                  {reporte.filas.map((fila) => (
                    <tr
                      key={fila.id_sesion}
                      className={fila.pendiente ? 'fila-pendiente' : undefined}
                    >
                      <td>{fila.id_sesion}</td>
                      <td>{fechaHora(fila.fechaApertura, fila.horaApertura)}</td>
                      <td>{fechaHora(fila.fechaCierre, fila.horaCierre)}</td>
                      <td>{fila.abrio}</td>
                      <td>{fila.cerro ?? '—'}</td>
                      <td className="reporte-numero">$ {formatoNumero(fila.montoInicial)}</td>
                      <td>
                        <span className={claseEstado(fila.estado)}>
                          {etiquetaEstado(fila.estado)}
                        </span>
                      </td>
                      {medios.flatMap((medio) => COLUMNAS_MEDIO.map((columna) => {
                        const valor = Number(fila.valoresPorMedio?.[medio]?.[columna.clave] ?? 0)
                        return (
                          <td
                            key={`${medio}-${columna.clave}`}
                            className={`reporte-numero ${
                              columna.clave === 'diferencia' && valor !== 0
                                ? (valor < 0 ? 'monto-negativo' : 'monto-positivo')
                                : ''
                            }`}
                          >
                            $ {formatoNumero(valor)}
                          </td>
                        )
                      }))}
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr>
                    <th colSpan={7}>
                      Totales ({cantSesiones} {cantSesiones === 1 ? 'sesión' : 'sesiones'})
                    </th>
                    {medios.flatMap((medio) => COLUMNAS_MEDIO.map((columna) => (
                      <th key={`${medio}-${columna.clave}`} className="reporte-numero">
                        $ {formatoNumero(reporte.totales.porMedio?.[medio]?.[columna.clave] ?? 0)}
                      </th>
                    )))}
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

export default ReporteCierresCajaPage
