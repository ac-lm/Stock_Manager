import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Modal from '../components/Modal'
import MovimientosCajaTable from '../components/caja/MovimientosCajaTable'
import TotalesCaja from '../components/caja/TotalesCaja'
import { cajaService } from '../services/cajaService'
import {
  ESTADOS_CAJA,
  claseDiferencia,
  claseEstado,
  diferenciaConSigno,
  etiquetaEstado,
  formatearFechaHora,
  formatearFechaHoraISO,
  formatearMonto,
} from '../utils/caja'

const TAMANIO_PAGINA = 10

function CajaHistorialPage() {
  const [sesiones, setSesiones] = useState([])
  const [pagina, setPagina] = useState(0)
  const [totalPaginas, setTotalPaginas] = useState(0)
  const [totalElementos, setTotalElementos] = useState(0)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)

  const [estado, setEstado] = useState('')
  const [id_usuario, setIdUsuario] = useState('')
  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')

  const [detalle, setDetalle] = useState(null)
  const [modal, setModal] = useState(null)
  const [cargandoDetalle, setCargandoDetalle] = useState(false)

  useEffect(() => {
    let cancelado = false

    const cargar = async () => {
      setCargando(true)
      try {
        const data = await cajaService.listar({
          estado,
          id_usuario,
          desde,
          hasta,
          page: pagina,
          size: TAMANIO_PAGINA,
        })
        if (!cancelado) {
          setSesiones(data.content)
          setTotalPaginas(data.totalPages)
          setTotalElementos(data.totalElements)
          setError(null)
        }
      } catch (err) {
        if (!cancelado) setError(err.message)
      } finally {
        if (!cancelado) setCargando(false)
      }
    }

    cargar()
    return () => {
      cancelado = true
    }
  }, [estado, id_usuario, desde, hasta, pagina])

  const cambiarFiltro = (setter) => (event) => {
    setter(event.target.value)
    setPagina(0)
  }

  const limpiarFiltros = () => {
    setEstado('')
    setIdUsuario('')
    setDesde('')
    setHasta('')
    setPagina(0)
  }

  const abrirDetalle = async (id_sesion) => {
    setError(null)
    setDetalle(null)
    setCargandoDetalle(true)
    setModal('detalle')
    try {
      setDetalle(await cajaService.obtener(id_sesion))
    } catch (err) {
      setError(err.message)
      setModal(null)
    } finally {
      setCargandoDetalle(false)
    }
  }

  const cerrarModal = () => {
    setModal(null)
    setDetalle(null)
  }

  const sesionDetalle = detalle?.sesion

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Historial de caja</h1>
          <p className="page-subtitle">
            Sesiones abiertas y cerradas. El filtro por usuario incluye a quien abrió y a quien
            cerró.
          </p>
        </div>
        <div className="caja-acciones">
          <Link className="btn btn-secondary" to="/caja">
            Volver a la caja
          </Link>
        </div>
      </header>

      {error && <div className="banner banner-error">{error}</div>}

      <div className="toolbar">
        <label className="field field-inline">
          <span>Desde</span>
          <input
            type="date"
            value={desde}
            onChange={cambiarFiltro(setDesde)}
            aria-label="Fecha de apertura desde"
          />
        </label>

        <label className="field field-inline">
          <span>Hasta</span>
          <input
            type="date"
            value={hasta}
            onChange={cambiarFiltro(setHasta)}
            aria-label="Fecha de apertura hasta"
          />
        </label>

        <select
          value={estado}
          onChange={cambiarFiltro(setEstado)}
          aria-label="Filtrar por estado"
        >
          <option value="">Todos los estados</option>
          {ESTADOS_CAJA.map((opcion) => (
            <option key={opcion.value} value={opcion.value}>
              {opcion.label}
            </option>
          ))}
        </select>

        <input
          type="number"
          min="1"
          placeholder="ID de usuario"
          value={id_usuario}
          onChange={cambiarFiltro(setIdUsuario)}
          aria-label="Filtrar por usuario que abrió o cerró"
          title="Coincide con quien abrió o quien cerró la sesión"
        />

        <button type="button" className="btn btn-secondary btn-sm" onClick={limpiarFiltros}>
          Limpiar filtros
        </button>
      </div>

      <div className="card">
        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>Sesión</th>
                <th>Apertura</th>
                <th>Cierre</th>
                <th>Abrió</th>
                <th>Cerró</th>
                <th>Esperado</th>
                <th>Declarado</th>
                <th>Diferencia</th>
                <th>Estado</th>
                <th className="table-actions">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {!cargando && sesiones.length === 0 && (
                <tr>
                  <td colSpan="10" className="empty-state">
                    No se encontraron sesiones de caja.
                  </td>
                </tr>
              )}
              {sesiones.map((sesion) => (
                <tr key={sesion.id_sesion}>
                  <td className="mono">#{sesion.id_sesion}</td>
                  <td>{formatearFechaHora(sesion.fechaApertura, sesion.horaApertura)}</td>
                  <td>
                    {sesion.fechaCierre
                      ? formatearFechaHora(sesion.fechaCierre, sesion.horaCierre)
                      : '—'}
                  </td>
                  <td>{sesion.usuarioApertura}</td>
                  <td>{sesion.usuarioCierre ?? '—'}</td>
                  <td className="mono">{formatearMonto(sesion.totales.totalEsperado)}</td>
                  <td className="mono">
                    {sesion.totales.totalDeclarado != null
                      ? formatearMonto(sesion.totales.totalDeclarado)
                      : '—'}
                  </td>
                  <td className={claseDiferencia(sesion.totales.diferencia)}>
                    {sesion.totales.diferencia != null
                      ? diferenciaConSigno(sesion.totales.diferencia)
                      : '—'}
                  </td>
                  <td>
                    <span className={claseEstado(sesion.estado)}>
                      {etiquetaEstado(sesion.estado)}
                    </span>
                  </td>
                  <td className="table-actions">
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      onClick={() => abrirDetalle(sesion.id_sesion)}
                    >
                      Ver
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {cargando && <p className="loading">Cargando sesiones...</p>}
      </div>

      <div className="paginacion">
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          disabled={pagina === 0}
          onClick={() => setPagina(pagina - 1)}
        >
          Anterior
        </button>
        <span>
          Página {pagina + 1} de {Math.max(totalPaginas, 1)} · {totalElementos} sesiones
        </span>
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          disabled={pagina + 1 >= totalPaginas}
          onClick={() => setPagina(pagina + 1)}
        >
          Siguiente
        </button>
      </div>

      {modal === 'detalle' && (
        <Modal
          title={sesionDetalle ? `Sesión #${sesionDetalle.id_sesion}` : 'Sesión de caja'}
          subtitle={
            sesionDetalle
              ? `${formatearFechaHora(
                  sesionDetalle.fechaApertura,
                  sesionDetalle.horaApertura,
                )} · ${etiquetaEstado(sesionDetalle.estado)}`
              : ''
          }
          onClose={cerrarModal}
        >
          {cargandoDetalle || !sesionDetalle ? (
            <p className="loading">Cargando sesión...</p>
          ) : (
            <>
              <div className="caja-estado-sesion">
                <span className={claseEstado(sesionDetalle.estado)}>
                  {etiquetaEstado(sesionDetalle.estado)}
                </span>
                <span>
                  Abrió: <strong>{sesionDetalle.usuarioApertura}</strong>
                </span>
                {sesionDetalle.usuarioCierre && (
                  <span>
                    Cerró: <strong>{sesionDetalle.usuarioCierre}</strong> el{' '}
                    {formatearFechaHora(
                      sesionDetalle.fechaCierre,
                      sesionDetalle.horaCierre,
                    )}
                  </span>
                )}
                <span>
                  Monto inicial: <strong>{formatearMonto(sesionDetalle.montoInicial)}</strong>
                </span>
              </div>

              {sesionDetalle.motivoAjusteInicial && (
                <div className="aviso" role="note">
                  Motivo del ajuste del monto inicial: {sesionDetalle.motivoAjusteInicial}
                </div>
              )}

              <TotalesCaja totales={sesionDetalle.totales} />

              {sesionDetalle.observaciones && (
                <p>
                  <strong>Observación del cierre:</strong> {sesionDetalle.observaciones}
                </p>
              )}

              {sesionDetalle.fechaRevision ? (
                <div className="aviso" role="note">
                  <strong>Revisión de {sesionDetalle.usuarioRevision}:</strong>{' '}
                  {formatearFechaHoraISO(sesionDetalle.fechaRevision)}
                  <br />
                  {sesionDetalle.observacionRevision ?? 'Sin observación.'}
                </div>
              ) : (
                sesionDetalle.estado === 'PENDIENTE_REVISION' && (
                  <div className="aviso" role="status">
                    La sesión está pendiente de revisión de un administrador.
                  </div>
                )
              )}

              <div className="seccion-header">
                <h2>Movimientos</h2>
              </div>

              <MovimientosCajaTable movimientos={detalle.movimientos} />

              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={cerrarModal}>
                  Cerrar
                </button>
              </div>
            </>
          )}
        </Modal>
      )}
    </section>
  )
}

export default CajaHistorialPage
