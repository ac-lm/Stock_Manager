import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Modal from '../components/Modal'
import MovimientosCajaTable from '../components/caja/MovimientosCajaTable'
import TotalesCaja from '../components/caja/TotalesCaja'
import { cajaService } from '../services/cajaService'
import {
  CONCEPTOS_EGRESO,
  MEDIOS_PAGO,
  claseEstado,
  etiquetaEstado,
  formatearFechaHora,
  formatearMonto,
} from '../utils/caja'

const LARGO_MAXIMO_OBSERVACIONES = 150

const FORM_APERTURA_VACIO = { montoInicial: '', motivoAjusteInicial: '' }
const FORM_EGRESO_VACIO = {
  concepto: 'RETIRO',
  medioPago: 'EFECTIVO',
  monto: '',
  observaciones: '',
}
const FORM_CIERRE_VACIO = {
  declaradoEfectivo: '',
  declaradoTransferencia: '',
  observaciones: '',
}

function CajaPage() {
  const [datos, setDatos] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [recarga, setRecarga] = useState(0)
  const [error, setError] = useState(null)
  const [mensaje, setMensaje] = useState(null)

  const [modal, setModal] = useState(null)
  const [guardando, setGuardando] = useState(false)
  const [errorForm, setErrorForm] = useState(null)
  const [erroresCampos, setErroresCampos] = useState({})

  const [formApertura, setFormApertura] = useState(FORM_APERTURA_VACIO)
  const [formEgreso, setFormEgreso] = useState(FORM_EGRESO_VACIO)
  const [formCierre, setFormCierre] = useState(FORM_CIERRE_VACIO)

  useEffect(() => {
    let cancelado = false

    const cargar = async () => {
      setCargando(true)
      try {
        const data = await cajaService.actual()
        if (!cancelado) {
          setDatos(data)
          if (!data.sesion) {
            setFormApertura({
              montoInicial: String(data.montoInicialSugerido ?? 0),
              motivoAjusteInicial: '',
            })
          }
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
  }, [recarga])

  const sesion = datos?.sesion
  const totales = sesion?.totales
  const abierta = sesion?.estado === 'ABIERTA'

  const cerrarModal = () => {
    setModal(null)
    setErrorForm(null)
    setErroresCampos({})
  }

  const cambiarCampo = (setter) => (campo) => (event) => {
    setter((prev) => ({ ...prev, [campo]: event.target.value }))
    setErroresCampos((prev) => {
      if (!prev[campo]) return prev
      const resto = { ...prev }
      delete resto[campo]
      return resto
    })
  }

  const abrirEgreso = () => {
    setFormEgreso(FORM_EGRESO_VACIO)
    setErrorForm(null)
    setErroresCampos({})
    setError(null)
    setMensaje(null)
    setModal('egreso')
  }

  const abrirCierre = () => {
    setFormCierre(FORM_CIERRE_VACIO)
    setErrorForm(null)
    setErroresCampos({})
    setError(null)
    setMensaje(null)
    setModal('cierre')
  }

  const abrirCaja = async (event) => {
    event.preventDefault()
    setErrorForm(null)

    const monto = Number(formApertura.montoInicial)
    const sugerido = Number(datos?.montoInicialSugerido ?? 0)
    const errores = {}

    if (formApertura.montoInicial.trim() === '' || Number.isNaN(monto)) {
      errores.montoInicial = 'Ingresá el monto inicial.'
    } else if (monto < 0) {
      errores.montoInicial = 'El monto inicial no puede ser negativo.'
    } else if (
      Math.abs(monto - sugerido) > 0.005 &&
      !formApertura.motivoAjusteInicial.trim()
    ) {
      errores.motivoAjusteInicial =
        'El motivo es obligatorio cuando el monto difiere del sugerido.'
    }

    setErroresCampos(errores)
    if (Object.keys(errores).length > 0) return

    setGuardando(true)
    try {
      await cajaService.abrir({
        montoInicial: monto,
        motivoAjusteInicial: formApertura.motivoAjusteInicial.trim() || null,
      })
      setMensaje('Caja abierta correctamente.')
      setRecarga((n) => n + 1)
    } catch (err) {
      setErrorForm(err.message)
    } finally {
      setGuardando(false)
    }
  }

  const registrarEgreso = async (event) => {
    event.preventDefault()
    setErrorForm(null)

    const monto = Number(formEgreso.monto)
    const errores = {}

    if (formEgreso.monto.trim() === '' || Number.isNaN(monto)) {
      errores.monto = 'Ingresá el monto del egreso.'
    } else if (monto <= 0) {
      errores.monto = 'El monto debe ser mayor a cero.'
    } else if (
      formEgreso.medioPago === 'EFECTIVO' &&
      monto > Number(totales?.esperadoEfectivo ?? 0)
    ) {
      errores.monto = `El efectivo esperado es ${formatearMonto(
        totales?.esperadoEfectivo,
      )} y no alcanza para ese egreso.`
    }

    if (formEgreso.observaciones.trim().length > LARGO_MAXIMO_OBSERVACIONES) {
      errores.observaciones = `La observación admite hasta ${LARGO_MAXIMO_OBSERVACIONES} caracteres.`
    }

    setErroresCampos(errores)
    if (Object.keys(errores).length > 0) return

    setGuardando(true)
    try {
      await cajaService.registrarEgreso(sesion.id_sesion, {
        concepto: formEgreso.concepto,
        medioPago: formEgreso.medioPago,
        monto,
        observaciones: formEgreso.observaciones.trim() || null,
      })
      setMensaje('Egreso registrado correctamente.')
      setModal(null)
      setRecarga((n) => n + 1)
    } catch (err) {
      if (err.campos && Object.keys(err.campos).length > 0) {
        setErroresCampos(err.campos)
        setErrorForm('Revisá los datos marcados en el formulario.')
      } else {
        setErrorForm(err.message)
      }
    } finally {
      setGuardando(false)
    }
  }

  const cerrarCaja = async (event) => {
    event.preventDefault()

    if (
      !window.confirm(
        '¿Cerrar la caja? Después del cierre no se pueden registrar más movimientos.',
      )
    ) {
      return
    }

    setErrorForm(null)

    const declaradoEfectivo = Number(formCierre.declaradoEfectivo)
    const declaradoTransferencia = Number(formCierre.declaradoTransferencia)
    const esperadoEfectivo = Number(totales?.esperadoEfectivo ?? 0)
    const esperadoTransferencia = Number(totales?.esperadoTransferencia ?? 0)
    const errores = {}

    if (formCierre.declaradoEfectivo.trim() === '' || Number.isNaN(declaradoEfectivo)) {
      errores.declaradoEfectivo = 'Ingresá lo contado en efectivo.'
    } else if (declaradoEfectivo < 0) {
      errores.declaradoEfectivo = 'El declarado no puede ser negativo.'
    }

    if (
      formCierre.declaradoTransferencia.trim() === '' ||
      Number.isNaN(declaradoTransferencia)
    ) {
      errores.declaradoTransferencia = 'Ingresá el neto por transferencia.'
    }

    const diferenciaEfectivo = declaradoEfectivo - esperadoEfectivo
    const diferenciaTransferencia = declaradoTransferencia - esperadoTransferencia
    const hayDiferencia =
      Math.abs(diferenciaEfectivo) > 0.005 || Math.abs(diferenciaTransferencia) > 0.005

    if (hayDiferencia && !formCierre.observaciones.trim()) {
      errores.observaciones =
        'La observación es obligatoria cuando alguna diferencia no es cero.'
    } else if (formCierre.observaciones.trim().length > 100) {
      errores.observaciones = 'La observación admite hasta 100 caracteres.'
    }

    setErroresCampos(errores)
    if (Object.keys(errores).length > 0) return

    setGuardando(true)
    try {
      const cerrada = await cajaService.cerrar(sesion.id_sesion, {
        declaradoEfectivo,
        declaradoTransferencia,
        observaciones: formCierre.observaciones.trim() || null,
      })
      setMensaje(
        cerrada.estado === 'PENDIENTE_REVISION'
          ? 'Caja cerrada con diferencia. Queda pendiente de revisión de un administrador.'
          : 'Caja cerrada sin diferencias.',
      )
      setModal(null)
      setRecarga((n) => n + 1)
    } catch (err) {
      if (err.campos && Object.keys(err.campos).length > 0) {
        setErroresCampos(err.campos)
        setErrorForm('Revisá los datos marcados en el formulario.')
      } else {
        setErrorForm(err.message)
      }
    } finally {
      setGuardando(false)
    }
  }

  const cambiarEgreso = cambiarCampo(setFormEgreso)
  const cambiarCierre = cambiarCampo(setFormCierre)

  const diferenciaCierre =
    Number(formCierre.declaradoEfectivo || 0) -
    Number(totales?.esperadoEfectivo ?? 0) +
    (Number(formCierre.declaradoTransferencia || 0) -
      Number(totales?.esperadoTransferencia ?? 0))

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Caja</h1>
          <p className="page-subtitle">
            {sesion
              ? `Sesión abierta por ${sesion.usuarioApertura} el ${formatearFechaHora(
                  sesion.fechaApertura,
                  sesion.horaApertura,
                )}.`
              : 'No hay una sesión de caja abierta.'}
          </p>
        </div>
        <div className="caja-acciones">
          <Link className="btn btn-secondary" to="/caja/historial">
            Historial
          </Link>
          {abierta && (
            <>
              <button type="button" className="btn btn-primary" onClick={abrirEgreso}>
                Registrar egreso
              </button>
              <button type="button" className="btn btn-secondary" onClick={abrirCierre}>
                Cerrar caja
              </button>
            </>
          )}
        </div>
      </header>

      {error && <div className="banner banner-error">{error}</div>}
      {mensaje && <div className="banner banner-success">{mensaje}</div>}

      {cargando && !datos && <p className="loading">Cargando la caja...</p>}

      {!cargando && !sesion && (
        <div className="card caja-apertura">
          <div className="seccion-header">
            <div>
              <h2>Abrir caja</h2>
              <p className="page-subtitle">
                El monto sugerido es el efectivo declarado en el último cierre.
              </p>
            </div>
            <span className="badge badge-inactivo">Sin sesión abierta</span>
          </div>

          {errorForm && <div className="banner banner-error">{errorForm}</div>}

          <form className="form" onSubmit={abrirCaja} noValidate>
            <div className="form-row">
              <label className="field">
                <span>Monto inicial en efectivo *</span>
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={formApertura.montoInicial}
                  onChange={cambiarCampo(setFormApertura)('montoInicial')}
                  aria-invalid={Boolean(erroresCampos.montoInicial)}
                  required
                  autoFocus
                />
                {erroresCampos.montoInicial ? (
                  <small className="field-error">{erroresCampos.montoInicial}</small>
                ) : (
                  <small className="field-help">
                    Sugerido: {formatearMonto(datos?.montoInicialSugerido)}
                  </small>
                )}
              </label>

              <label className="field">
                <span>Motivo del ajuste</span>
                <input
                  type="text"
                  maxLength="150"
                  value={formApertura.motivoAjusteInicial}
                  onChange={cambiarCampo(setFormApertura)('motivoAjusteInicial')}
                  aria-invalid={Boolean(erroresCampos.motivoAjusteInicial)}
                  placeholder="Obligatorio si cambiás el monto sugerido"
                />
                {erroresCampos.motivoAjusteInicial && (
                  <small className="field-error">
                    {erroresCampos.motivoAjusteInicial}
                  </small>
                )}
              </label>
            </div>

            <div className="modal-footer">
              <Link className="btn btn-secondary" to="/caja/historial">
                Ver historial
              </Link>
              <button type="submit" className="btn btn-primary" disabled={guardando}>
                {guardando ? 'Abriendo...' : 'Abrir caja'}
              </button>
            </div>
          </form>
        </div>
      )}

      {sesion && (
        <>
          <div className="caja-estado-sesion">
            <span className={claseEstado(sesion.estado)}>{etiquetaEstado(sesion.estado)}</span>
            <span className="mono">Sesión #{sesion.id_sesion}</span>
            <span>
              Monto inicial: <strong>{formatearMonto(sesion.montoInicial)}</strong>
            </span>
          </div>

          {sesion.motivoAjusteInicial && (
            <div className="aviso" role="note">
              El monto inicial se ajustó: {sesion.motivoAjusteInicial}
            </div>
          )}

          <TotalesCaja totales={totales} />

          <div className="seccion-header">
            <div>
              <h2>Movimientos</h2>
              <p className="page-subtitle">
                Los movimientos no se editan ni se borran. Se listan del más nuevo al más viejo.
              </p>
            </div>
          </div>

          <div className="card">
            <MovimientosCajaTable movimientos={datos.movimientos} />
          </div>
        </>
      )}

      {modal === 'egreso' && sesion && (
        <Modal
          title="Registrar egreso"
          subtitle={`Sesión #${sesion.id_sesion} · efectivo esperado ${formatearMonto(
            totales?.esperadoEfectivo,
          )}`}
          onClose={cerrarModal}
        >
          {errorForm && <div className="banner banner-error">{errorForm}</div>}

          <form className="form" onSubmit={registrarEgreso} noValidate>
            <div className="form-row">
              <label className="field">
                <span>Concepto *</span>
                <select
                  value={formEgreso.concepto}
                  onChange={cambiarEgreso('concepto')}
                  required
                >
                  {CONCEPTOS_EGRESO.map((opcion) => (
                    <option key={opcion.value} value={opcion.value}>
                      {opcion.label}
                    </option>
                  ))}
                </select>
              </label>

              <label className="field">
                <span>Medio de pago *</span>
                <select
                  value={formEgreso.medioPago}
                  onChange={cambiarEgreso('medioPago')}
                  required
                >
                  {MEDIOS_PAGO.map((opcion) => (
                    <option key={opcion.value} value={opcion.value}>
                      {opcion.label}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            <div className="form-row">
              <label className="field">
                <span>Monto *</span>
                <input
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={formEgreso.monto}
                  onChange={cambiarEgreso('monto')}
                  aria-invalid={Boolean(erroresCampos.monto)}
                  required
                  autoFocus
                />
                {erroresCampos.monto ? (
                  <small className="field-error">{erroresCampos.monto}</small>
                ) : (
                  <small className="field-help">
                    {formEgreso.medioPago === 'EFECTIVO'
                      ? 'No puede superar el efectivo esperado.'
                      : 'Las transferencias no tienen límite.'}
                  </small>
                )}
              </label>

              <label className="field">
                <span>Observaciones</span>
                <input
                  type="text"
                  maxLength={LARGO_MAXIMO_OBSERVACIONES}
                  value={formEgreso.observaciones}
                  onChange={cambiarEgreso('observaciones')}
                  aria-invalid={Boolean(erroresCampos.observaciones)}
                />
                {erroresCampos.observaciones && (
                  <small className="field-error">{erroresCampos.observaciones}</small>
                )}
              </label>
            </div>

            <div className="modal-footer">
              <button type="button" className="btn btn-secondary" onClick={cerrarModal}>
                Cancelar
              </button>
              <button type="submit" className="btn btn-primary" disabled={guardando}>
                {guardando ? 'Registrando...' : 'Registrar egreso'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {modal === 'cierre' && sesion && (
        <Modal
          title="Cerrar caja"
          subtitle={`Sesión #${sesion.id_sesion} · después del cierre no se pueden registrar más movimientos`}
          onClose={cerrarModal}
        >
          {errorForm && <div className="banner banner-error">{errorForm}</div>}

          <div className="aviso" role="note">
            Contá el efectivo y verificá las transferencias en el banco. El neto por transferencia
            es cobros menos pagos y puede ser negativo. Si alguna diferencia no es cero, la
            observación es obligatoria.
          </div>

          <form className="form" onSubmit={cerrarCaja} noValidate>
            <div className="form-row">
              <label className="field">
                <span>Declarado en efectivo *</span>
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={formCierre.declaradoEfectivo}
                  onChange={cambiarCierre('declaradoEfectivo')}
                  aria-invalid={Boolean(erroresCampos.declaradoEfectivo)}
                  required
                  autoFocus
                />
                {erroresCampos.declaradoEfectivo ? (
                  <small className="field-error">{erroresCampos.declaradoEfectivo}</small>
                ) : (
                  <small className="field-help">
                    Esperado: {formatearMonto(totales?.esperadoEfectivo)}
                  </small>
                )}
              </label>

              <label className="field">
                <span>Neto por transferencia (cobros − pagos) *</span>
                <input
                  type="number"
                  step="0.01"
                  value={formCierre.declaradoTransferencia}
                  onChange={cambiarCierre('declaradoTransferencia')}
                  aria-invalid={Boolean(erroresCampos.declaradoTransferencia)}
                  required
                />
                {erroresCampos.declaradoTransferencia ? (
                  <small className="field-error">{erroresCampos.declaradoTransferencia}</small>
                ) : (
                  <small className="field-help">
                    Puede ser negativo. Esperado: {formatearMonto(totales?.esperadoTransferencia)}
                  </small>
                )}
              </label>
            </div>

            <div className="total-filtro">
              <span>Diferencia estimada:</span>
              <strong className={diferenciaCierre < 0 ? 'monto-negativo' : 'monto-positivo'}>
                {formatearMonto(diferenciaCierre)}
              </strong>
              <small>Declarado menos esperado. Negativa es faltante.</small>
            </div>

            <label className="field">
              <span>Observaciones</span>
              <input
                type="text"
                maxLength="100"
                value={formCierre.observaciones}
                onChange={cambiarCierre('observaciones')}
                aria-invalid={Boolean(erroresCampos.observaciones)}
                placeholder="Obligatoria si hay diferencia"
              />
              {erroresCampos.observaciones && (
                <small className="field-error">{erroresCampos.observaciones}</small>
              )}
            </label>

            <div className="modal-footer">
              <button type="button" className="btn btn-secondary" onClick={cerrarModal}>
                Cancelar
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={guardando}
              >
                {guardando ? 'Cerrando...' : 'Cerrar caja'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </section>
  )
}

export default CajaPage
