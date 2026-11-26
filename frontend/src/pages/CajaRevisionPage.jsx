import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Modal from '../components/Modal'
import MovimientosCajaTable from '../components/caja/MovimientosCajaTable'
import TotalesCaja from '../components/caja/TotalesCaja'
import { cajaService } from '../services/cajaService'
import {
  claseDiferencia,
  claseEstado,
  diferenciaConSigno,
  formatearFechaHora,
  formatearMonto,
} from '../utils/caja'

const TAMANIO_PAGINA = 10
const LARGO_MAXIMO_OBSERVACION = 150

function CajaRevisionPage() {
  const [sesiones, setSesiones] = useState([])
  const [pagina, setPagina] = useState(0)
  const [totalPaginas, setTotalPaginas] = useState(0)
  const [totalElementos, setTotalElementos] = useState(0)
  const [recarga, setRecarga] = useState(0)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [mensaje, setMensaje] = useState(null)

  const [modal, setModal] = useState(null)
  const [detalle, setDetalle] = useState(null)
  const [cargandoDetalle, setCargandoDetalle] = useState(false)
  const [observacion, setObservacion] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [errorForm, setErrorForm] = useState(null)
  const [erroresCampos, setErroresCampos] = useState({})

  useEffect(() => {
    let cancelado = false

    const cargar = async () => {
      setCargando(true)
      try {
        const data = await cajaService.listar({
          estado: 'PENDIENTE_REVISION',
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
  }, [pagina, recarga])

  const abrirRevision = async (id_sesion) => {
    setError(null)
    setMensaje(null)
    setDetalle(null)
    setObservacion('')
    setErrorForm(null)
    setErroresCampos({})
    setCargandoDetalle(true)
    setModal('revision')
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
    setObservacion('')
    setErrorForm(null)
    setErroresCampos({})
  }

  const revisar = async (event) => {
    event.preventDefault()
    setErrorForm(null)

    if (!observacion.trim()) {
      setErroresCampos({ observacion: 'La observación de revisión es obligatoria.' })
      return
    }
    if (observacion.trim().length > LARGO_MAXIMO_OBSERVACION) {
      setErroresCampos({
        observacion: `La observación admite hasta ${LARGO_MAXIMO_OBSERVACION} caracteres.`,
      })
      return
    }

    setGuardando(true)
    try {
      await cajaService.revisar(detalle.sesion.id_sesion, {
        observacion: observacion.trim(),
      })
      setMensaje(`Sesión #${detalle.sesion.id_sesion} revisada y cerrada.`)
      setModal(null)
      setRecarga((n) => n + 1)
    } catch (err) {
      if (err.campos?.observacion) {
        setErroresCampos({ observacion: err.campos.observacion })
        setErrorForm('Revisá la observación marcada en el formulario.')
      } else {
        setErrorForm(err.message)
      }
    } finally {
      setGuardando(false)
    }
  }

  const sesionDetalle = detalle?.sesion

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Revisión de caja</h1>
          <p className="page-subtitle">
            Sesiones cerradas con diferencia. La revisión deja constancia y las pasa a cerradas
            sin cambiar los montos.
          </p>
        </div>
        <div className="caja-acciones">
          <Link className="btn btn-secondary" to="/caja">
            Volver a la caja
          </Link>
          <Link className="btn btn-secondary" to="/caja/historial">
            Historial
          </Link>
        </div>
      </header>

      {error && <div className="banner banner-error">{error}</div>}
      {mensaje && <div className="banner banner-success">{mensaje}</div>}

      <div className="card">
        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>Sesión</th>
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
                  <td colSpan="9" className="empty-state">
                    No hay sesiones pendientes de revisión.
                  </td>
                </tr>
              )}
              {sesiones.map((sesion) => (
                <tr key={sesion.id_sesion}>
                  <td className="mono">#{sesion.id_sesion}</td>
                  <td>{formatearFechaHora(sesion.fechaCierre, sesion.horaCierre)}</td>
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
                      {sesion.estado === 'PENDIENTE_REVISION'
                        ? 'Pendiente de revisión'
                        : sesion.estado}
                    </span>
                  </td>
                  <td className="table-actions">
                    <button
                      type="button"
                      className="btn btn-primary btn-sm"
                      onClick={() => abrirRevision(sesion.id_sesion)}
                    >
                      Revisar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {cargando && <p className="loading">Cargando sesiones pendientes...</p>}
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
          Página {pagina + 1} de {Math.max(totalPaginas, 1)} · {totalElementos} pendientes
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

      {modal === 'revision' && (
        <Modal
          title={sesionDetalle ? `Revisar sesión #${sesionDetalle.id_sesion}` : 'Revisar sesión'}
          subtitle="Los montos declarados no se modifican en esta instancia."
          onClose={cerrarModal}
        >
          {errorForm && <div className="banner banner-error">{errorForm}</div>}

          {cargandoDetalle || !sesionDetalle ? (
            <p className="loading">Cargando sesión...</p>
          ) : (
            <>
              <div className="caja-estado-sesion">
                <span className={claseEstado(sesionDetalle.estado)}>
                  Pendiente de revisión
                </span>
                <span>
                  Abrió: <strong>{sesionDetalle.usuarioApertura}</strong>
                </span>
                <span>
                  Cerró: <strong>{sesionDetalle.usuarioCierre}</strong> el{' '}
                  {formatearFechaHora(
                    sesionDetalle.fechaCierre,
                    sesionDetalle.horaCierre,
                  )}
                </span>
              </div>

              <TotalesCaja totales={sesionDetalle.totales} />

              {sesionDetalle.observaciones && (
                <p>
                  <strong>Observación del cierre:</strong> {sesionDetalle.observaciones}
                </p>
              )}

              <div className="seccion-header">
                <h2>Movimientos</h2>
              </div>

              <MovimientosCajaTable movimientos={detalle.movimientos} />

              <form className="form" onSubmit={revisar} noValidate>
                <label className="field">
                  <span>Observación de la revisión *</span>
                  <input
                    type="text"
                    maxLength={LARGO_MAXIMO_OBSERVACION}
                    value={observacion}
                    onChange={(event) => {
                      setObservacion(event.target.value)
                      if (erroresCampos.observacion) setErroresCampos({})
                    }}
                    aria-invalid={Boolean(erroresCampos.observacion)}
                    placeholder="Por ejemplo: se verificó el faltante con el banco"
                    required
                    autoFocus
                  />
                  {erroresCampos.observacion && (
                    <small className="field-error">{erroresCampos.observacion}</small>
                  )}
                </label>

                <div className="modal-footer">
                  <button type="button" className="btn btn-secondary" onClick={cerrarModal}>
                    Cancelar
                  </button>
                  <button type="submit" className="btn btn-primary" disabled={guardando}>
                    {guardando ? 'Revisando...' : 'Confirmar revisión'}
                  </button>
                </div>
              </form>
            </>
          )}
        </Modal>
      )}
    </section>
  )
}

export default CajaRevisionPage
