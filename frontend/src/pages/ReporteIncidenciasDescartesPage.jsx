import { useEffect, useState } from 'react'
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { insumosService, productosService } from '../services/catalogoService'
import { reportesService } from '../services/reportesService'
import { formatoNumero } from '../utils/format'

// Las fechas se eligen como fechas de calendario: el backend las aplica sin convertir zonas.
const ZONA_TALLER = 'America/Argentina/Buenos_Aires'
const FORMATOS_EXPORTACION = [
  { formato: 'xlsx', etiqueta: 'Excel' },
  { formato: 'csv', etiqueta: 'CSV' },
  { formato: 'pdf', etiqueta: 'PDF' },
]
const ESTADOS = [
  { value: 'TODOS', label: 'Todos' },
  { value: 'ABIERTA', label: 'Abiertas' },
  { value: 'CERRADA', label: 'Cerradas' },
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
    id_tipo_incidencia: '',
    estado: 'TODOS',
    articulo: '',
  }
}

// El tipo de artículo viaja con el id para no mezclar ids iguales de producto e insumo.
function aFiltrosApi(filtros) {
  const [tipoArticulo, idArticulo] = filtros.articulo
    ? filtros.articulo.split(':')
    : ['TODOS', '']
  return {
    desde: filtros.desde,
    hasta: filtros.hasta,
    id_tipo_incidencia: filtros.id_tipo_incidencia,
    estado: filtros.estado,
    tipo_articulo: tipoArticulo,
    id_articulo: idArticulo,
  }
}

function validarFiltros({ desde, hasta }) {
  if (!desde || !hasta) return 'Ingresá las dos fechas del rango.'
  if (desde > hasta) return 'La fecha "desde" no puede ser posterior a "hasta".'
  const [anioDesde, mesDesde, diaDesde] = desde.split('-').map(Number)
  const [anioHasta, mesHasta, diaHasta] = hasta.split('-').map(Number)
  if (Date.UTC(anioHasta, mesHasta - 1, diaHasta) >
      Date.UTC(anioDesde + 5, mesDesde - 1, diaDesde - 1)) {
    return 'El rango no puede superar los 5 años.'
  }
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

// El gráfico usa incidencias distintas por tipo; los descartes nunca se apilan ni se netean.
function datosGrafico(reporte) {
  return (reporte?.incidenciasPorTipo ?? []).map((punto) => ({
    etiqueta: punto.tipoIncidencia,
    incidencias: Number(punto.incidencias ?? 0),
  }))
}

function ReporteIncidenciasDescartesPage() {
  const [filtros, setFiltros] = useState(filtrosIniciales)
  const [filtrosAplicados, setFiltrosAplicados] = useState(() =>
    aFiltrosApi(filtrosIniciales()),
  )
  const [productos, setProductos] = useState([])
  const [insumos, setInsumos] = useState([])
  const [errorCatalogos, setErrorCatalogos] = useState(null)
  const [reporte, setReporte] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [errorForm, setErrorForm] = useState(null)
  const [errorExportacion, setErrorExportacion] = useState(null)
  const [exportando, setExportando] = useState(null)

  useEffect(() => {
    let cancelado = false

    Promise.all([productosService.listar(), insumosService.listar()])
      .then(([listaProductos, listaInsumos]) => {
        if (cancelado) return
        setProductos(listaProductos)
        setInsumos(listaInsumos)
      })
      .catch((err) => {
        if (!cancelado) setErrorCatalogos(`No se pudieron cargar los filtros: ${err.message}`)
      })

    return () => {
      cancelado = true
    }
  }, [])

  useEffect(() => {
    let cancelado = false

    reportesService
      .incidenciasDescartes(filtrosAplicados)
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
        reporte: 'incidencias-descartes',
        formato,
        ...filtrosAplicados,
      })
      descargarArchivo(blob, nombreArchivo || `incidencias-descartes.${formato}`)
    } catch (err) {
      setErrorExportacion(err.message)
    } finally {
      setExportando(null)
    }
  }

  const grafico = datosGrafico(reporte)
  const opciones = reporte?.tiposDisponibles ?? []
  const totales = reporte?.totales

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Incidencias y descartes</h1>
          <p className="page-subtitle">
            Incidencias registradas contra los descartes MERMA del mismo artículo, sin vínculo 1:1
            ni suma de cantidades entre unidades.
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
            <span>Tipo de incidencia</span>
            <select
              value={filtros.id_tipo_incidencia}
              onChange={cambiarFiltro('id_tipo_incidencia')}
            >
              <option value="">Todos</option>
              {opciones.map((tipo) => (
                <option key={tipo.id} value={tipo.id}>
                  {tipo.nombre}
                  {tipo.activo ? '' : ' (inactivo)'}
                </option>
              ))}
            </select>
          </label>
          <label className="toolbar-campo">
            <span>Estado</span>
            <select value={filtros.estado} onChange={cambiarFiltro('estado')}>
              {ESTADOS.map((estado) => (
                <option key={estado.value} value={estado.value}>
                  {estado.label}
                </option>
              ))}
            </select>
          </label>
          <label className="toolbar-campo">
            <span>Artículo</span>
            <select value={filtros.articulo} onChange={cambiarFiltro('articulo')}>
              <option value="">Todos</option>
              {productos.length > 0 && (
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
              {insumos.length > 0 && (
                <optgroup label="Insumos">
                  {insumos.map((insumo) => (
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
        </div>
        <div className="toolbar-acciones">
          <button type="submit" className="btn btn-primary btn-sm" disabled={cargando}>
            Generar reporte
          </button>
        </div>
      </form>

      <p className="aviso">
        El rango es obligatorio, inclusivo y no puede superar los 5 años. Se cuentan incidencias y
        movimientos; las cantidades se muestran por artículo y unidad, nunca se suman entre
        artículos ni se descuentan unas de otras.
      </p>
      {(reporte?.avisos ?? []).map((aviso) => (
        <p className="aviso" key={aviso}>
          {aviso}
        </p>
      ))}

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
              <h2>Incidencias por tipo</h2>
              <span className="badge">
                {reporte.desde} a {reporte.hasta}
              </span>
            </div>
            {grafico.length > 0 ? (
              <ResponsiveContainer width="100%" height={Math.max(260, grafico.length * 60)}>
                <BarChart data={grafico} margin={{ top: 8, right: 24, bottom: 8, left: 8 }}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="etiqueta" tick={{ fontSize: 11 }} />
                  <YAxis
                    allowDecimals={false}
                    tickFormatter={(valor) => formatoNumero(valor, 0)}
                    tick={{ fontSize: 11 }}
                  />
                  <Tooltip formatter={(valor) => formatoNumero(valor, 0)} />
                  <Bar dataKey="incidencias" name="Incidencias" fill="#2563eb" />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <p className="loading">No hay incidencias con estos filtros.</p>
            )}
          </div>

          <div className="card">
            <div className="table-wrapper">
              <table className="table table-stock">
                <thead>
                  <tr>
                    <th>Tipo de incidencia</th>
                    <th>Artículo</th>
                    <th>Unidad</th>
                    <th>Estado</th>
                    <th className="reporte-numero">Incidencias</th>
                    <th className="reporte-numero">Cantidad afectada</th>
                    <th className="reporte-numero">Abiertas</th>
                    <th className="reporte-numero">Cerradas</th>
                    <th className="reporte-numero">Movimientos MERMA</th>
                    <th className="reporte-numero">Cantidad descartada</th>
                    <th>Posible traslape</th>
                  </tr>
                </thead>
                <tbody>
                  {reporte.filas.map((fila, indice) => (
                    <tr
                      key={`${fila.id_tipo_incidencia ?? 'sin-tipo'}-${fila.tipoArticulo ?? 'na'}-${fila.idArticulo}-${fila.estado}-${indice}`}
                    >
                      <td>{fila.tipoIncidencia}</td>
                      <td>{fila.articulo}</td>
                      <td>{fila.unidad ?? '—'}</td>
                      <td>{fila.estado}</td>
                      <td className="reporte-numero">{formatoNumero(fila.incidencias, 0)}</td>
                      <td className="reporte-numero">
                        {fila.incidencias > 0 ? formatoNumero(fila.cantidadAfectada, 3) : '—'}
                      </td>
                      <td className="reporte-numero">{formatoNumero(fila.abiertas, 0)}</td>
                      <td className="reporte-numero">{formatoNumero(fila.cerradas, 0)}</td>
                      <td className="reporte-numero">
                        {formatoNumero(fila.movimientosMerma, 0)}
                      </td>
                      <td className="reporte-numero">
                        {fila.movimientosMerma > 0
                          ? formatoNumero(fila.cantidadDescartada, 3)
                          : '—'}
                      </td>
                      <td>{fila.posibleTraslape ? 'Sí' : 'No'}</td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr>
                    <th colSpan="4">
                      Totales ({totales.incidencias} incidencias, {totales.movimientosMerma}{' '}
                      movimientos MERMA, {totales.articulos} artículos)
                    </th>
                    <th className="reporte-numero">{formatoNumero(totales.incidencias, 0)}</th>
                    <th className="reporte-numero">—</th>
                    <th className="reporte-numero">{formatoNumero(totales.abiertas, 0)}</th>
                    <th className="reporte-numero">{formatoNumero(totales.cerradas, 0)}</th>
                    <th className="reporte-numero">
                      {formatoNumero(totales.movimientosMerma, 0)}
                    </th>
                    <th className="reporte-numero">—</th>
                    <th>—</th>
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

export default ReporteIncidenciasDescartesPage
