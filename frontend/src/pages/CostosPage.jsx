import { useEffect, useState } from 'react'
import Modal from '../components/Modal'
import { costosService } from '../services/costosService'
import { formatoNumero } from '../utils/format'

const TAMANIO_PAGINA = 10

const MESES = [
  { value: 1, label: 'Enero' },
  { value: 2, label: 'Febrero' },
  { value: 3, label: 'Marzo' },
  { value: 4, label: 'Abril' },
  { value: 5, label: 'Mayo' },
  { value: 6, label: 'Junio' },
  { value: 7, label: 'Julio' },
  { value: 8, label: 'Agosto' },
  { value: 9, label: 'Septiembre' },
  { value: 10, label: 'Octubre' },
  { value: 11, label: 'Noviembre' },
  { value: 12, label: 'Diciembre' },
]

const LARGO_MAXIMO_DESCRIPCION = 255
const LARGO_MAXIMO_OBSERVACIONES = 255
const LARGO_MAXIMO_NOMBRE_TIPO = 50
const MONTO_CON_DECIMALES = /^\d+(\.\d{1,2})?$/

const FORM_TIPO_VACIO = { nombre: '' }

function formVacioCosto() {
  const hoy = new Date()
  return {
    id_tipo_costo: '',
    descripcion: '',
    monto: '',
    fechaPago: '',
    periodoMes: String(hoy.getMonth() + 1),
    periodoAnio: String(hoy.getFullYear()),
    observaciones: '',
  }
}

const etiquetaMes = (valor) =>
  valor == null || valor === '' ? '—' : (MESES.find((m) => m.value === Number(valor))?.label ?? valor)

const formatearFecha = (valor) => {
  if (!valor) return '—'
  const [anio, mes, dia] = valor.split('-')
  return dia && mes && anio ? `${dia}/${mes}/${anio}` : valor
}

const formatearFechaHora = (valor) => {
  if (!valor) return '—'
  const [fecha, hora] = valor.split('T')
  return `${formatearFecha(fecha)}${hora ? ` ${hora.slice(0, 5)}` : ''}`
}

function CostosPage() {
  // Listado y total del filtro
  const [costos, setCostos] = useState([])
  const [pagina, setPagina] = useState(0)
  const [totalPaginas, setTotalPaginas] = useState(0)
  const [totalElementos, setTotalElementos] = useState(0)
  const [totalFiltro, setTotalFiltro] = useState(0)

  // Filtros
  const [id_tipo_costo, setIdTipoCosto] = useState('')
  const [mes, setMes] = useState('')
  const [anio, setAnio] = useState('')
  const [incluirAnulados, setIncluirAnulados] = useState(false)

  // Tipos de costo
  const [tipos, setTipos] = useState([])
  const [cargandoTipos, setCargandoTipos] = useState(true)

  const [recarga, setRecarga] = useState(0)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [mensaje, setMensaje] = useState(null)

  // Modales
  const [modal, setModal] = useState(null)
  const [seleccionado, setSeleccionado] = useState(null)
  const [form, setForm] = useState(formVacioCosto)
  const [formTipo, setFormTipo] = useState(FORM_TIPO_VACIO)
  const [motivo, setMotivo] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [errorForm, setErrorForm] = useState(null)
  const [erroresCampos, setErroresCampos] = useState({})

  useEffect(() => {
    let cancelado = false

    const cargar = async () => {
      setCargando(true)
      try {
        const data = await costosService.listar({
          id_tipo_costo,
          mes,
          anio,
          incluirAnulados,
          page: pagina,
          size: TAMANIO_PAGINA,
        })
        if (!cancelado) {
          setCostos(data.content)
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
  }, [id_tipo_costo, mes, anio, incluirAnulados, pagina, recarga])

  // El endpoint de total ignora incluirAnulados: los anulados nunca suman.
  useEffect(() => {
    let cancelado = false

    const cargarTotal = async () => {
      try {
        const data = await costosService.total({ id_tipo_costo, mes, anio })
        if (!cancelado) setTotalFiltro(Number(data ?? 0))
      } catch (err) {
        if (!cancelado) setError(err.message)
      }
    }

    cargarTotal()
    return () => {
      cancelado = true
    }
  }, [id_tipo_costo, mes, anio, recarga])

  useEffect(() => {
    let cancelado = false

    const cargarTipos = async () => {
      setCargandoTipos(true)
      try {
        const data = await costosService.listarTipos()
        if (!cancelado) {
          setTipos(data)
          setError(null)
        }
      } catch (err) {
        if (!cancelado) setError(err.message)
      } finally {
        if (!cancelado) setCargandoTipos(false)
      }
    }

    cargarTipos()
    return () => {
      cancelado = true
    }
  }, [recarga])

  const tiposParaCargar = tipos.filter((tipo) => tipo.activo && !tipo.reservado)
  const tipoActualNoDisponible =
    modal === 'editar' &&
    !!seleccionado?.id_tipo_costo &&
    !tiposParaCargar.some((tipo) => tipo.id_tipo_costo === seleccionado.id_tipo_costo)

  const cambiarFiltro = (setter) => (event) => {
    setter(event.target.value)
    setPagina(0)
  }

  const cambiarIncluirAnulados = (event) => {
    setIncluirAnulados(event.target.checked)
    setPagina(0)
  }

  const cerrarModal = () => {
    setModal(null)
    setErrorForm(null)
    setErroresCampos({})
  }

  const abrirCrear = () => {
    setSeleccionado(null)
    setForm(formVacioCosto())
    setErrorForm(null)
    setErroresCampos({})
    setError(null)
    setMensaje(null)
    setModal('crear')
  }

  const abrirEditar = (costo) => {
    setSeleccionado(costo)
    const tipoHabilitado = tipos.some(
      (tipo) =>
        tipo.id_tipo_costo === costo.id_tipo_costo && tipo.activo && !tipo.reservado,
    )
    setForm({
      id_tipo_costo: tipoHabilitado ? String(costo.id_tipo_costo) : '',
      descripcion: costo.descripcion ?? '',
      monto: costo.monto != null ? String(costo.monto) : '',
      fechaPago: costo.fechaPago ?? '',
      periodoMes: String(costo.periodoMes ?? ''),
      periodoAnio: String(costo.periodoAnio ?? ''),
      observaciones: costo.observaciones ?? '',
    })
    setErrorForm(null)
    setErroresCampos({})
    setError(null)
    setMensaje(null)
    setModal('editar')
  }

  const abrirAnulacion = (costo) => {
    setSeleccionado(costo)
    setMotivo('')
    setErrorForm(null)
    setErroresCampos({})
    setError(null)
    setMensaje(null)
    setModal('anulacion')
  }

  const abrirCrearTipo = () => {
    setFormTipo(FORM_TIPO_VACIO)
    setErrorForm(null)
    setErroresCampos({})
    setError(null)
    setMensaje(null)
    setModal('tipo-crear')
  }

  const abrirEditarTipo = (tipo) => {
    setSeleccionado(tipo)
    setFormTipo({ nombre: tipo.nombre })
    setErrorForm(null)
    setErroresCampos({})
    setError(null)
    setMensaje(null)
    setModal('tipo-editar')
  }

  const cambiarCampo = (campo) => (event) => {
    setForm((prev) => ({ ...prev, [campo]: event.target.value }))
    setErroresCampos((prev) => {
      if (!prev[campo]) return prev
      const resto = { ...prev }
      delete resto[campo]
      return resto
    })
  }

  const cambiarNombreTipo = (event) => {
    setFormTipo({ nombre: event.target.value })
    if (erroresCampos.nombre) setErroresCampos({})
  }

  const validarCosto = () => {
    const errores = {}
    const descripcion = form.descripcion.trim()
    const observaciones = form.observaciones.trim()
    const monto = Number(form.monto)
    const periodoMes = Number(form.periodoMes)
    const periodoAnio = Number(form.periodoAnio)

    if (!form.id_tipo_costo) errores.id_tipo_costo = 'Seleccioná un tipo de costo.'

    if (!descripcion) errores.descripcion = 'Ingresá la descripción.'
    else if (descripcion.length > LARGO_MAXIMO_DESCRIPCION)
      errores.descripcion = `La descripción no puede superar los ${LARGO_MAXIMO_DESCRIPCION} caracteres.`

    if (!form.monto.trim()) errores.monto = 'Ingresá el monto.'
    else if (!MONTO_CON_DECIMALES.test(form.monto.trim()) || monto <= 0)
      errores.monto = 'El monto debe ser mayor a 0 y admitir hasta 2 decimales.'

    if (!form.fechaPago) errores.fechaPago = 'Ingresá la fecha de pago.'

    if (!form.periodoMes) errores.periodoMes = 'Seleccioná el mes.'
    else if (!Number.isInteger(periodoMes) || periodoMes < 1 || periodoMes > 12)
      errores.periodoMes = 'El mes debe estar entre 1 y 12.'

    if (!form.periodoAnio) errores.periodoAnio = 'Ingresá el año.'
    else if (!Number.isInteger(periodoAnio) || periodoAnio < 2000 || periodoAnio > 2100)
      errores.periodoAnio = 'El año no es válido.'

    if (observaciones.length > LARGO_MAXIMO_OBSERVACIONES)
      errores.observaciones = `Las observaciones no pueden superar los ${LARGO_MAXIMO_OBSERVACIONES} caracteres.`

    return errores
  }

  const validarTipo = () => {
    const errores = {}
    const nombre = formTipo.nombre.trim()

    if (!nombre) errores.nombre = 'Ingresá el nombre del tipo.'
    else if (nombre.length > LARGO_MAXIMO_NOMBRE_TIPO)
      errores.nombre = `El nombre no puede superar los ${LARGO_MAXIMO_NOMBRE_TIPO} caracteres.`

    return errores
  }

  const guardarCosto = async (event) => {
    event.preventDefault()
    setErrorForm(null)

    const errores = validarCosto()
    setErroresCampos(errores)
    if (Object.keys(errores).length > 0) return

    const payload = {
      id_tipo_costo: Number(form.id_tipo_costo),
      descripcion: form.descripcion.trim(),
      monto: Number(form.monto),
      fechaPago: form.fechaPago,
      periodoMes: Number(form.periodoMes),
      periodoAnio: Number(form.periodoAnio),
      observaciones: form.observaciones.trim() || null,
    }

    setGuardando(true)
    try {
      if (modal === 'editar') {
        await costosService.actualizar(seleccionado.id_costo, payload)
        setMensaje('Costo actualizado correctamente.')
      } else {
        await costosService.crear(payload)
        setMensaje('Costo registrado correctamente.')
      }
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

  const guardarAnulacion = async (event) => {
    event.preventDefault()
    setErrorForm(null)

    if (!motivo.trim()) {
      setErroresCampos({ motivo: 'Ingresá el motivo de la anulación.' })
      return
    }

    setGuardando(true)
    try {
      await costosService.anular(seleccionado.id_costo, motivo.trim())
      setMensaje('Costo anulado correctamente.')
      setModal(null)
      setRecarga((n) => n + 1)
    } catch (err) {
      if (err.campos?.motivo) {
        setErroresCampos({ motivo: err.campos.motivo })
        setErrorForm('Revisá el motivo marcado en el formulario.')
      } else {
        setErrorForm(err.message)
      }
    } finally {
      setGuardando(false)
    }
  }

  const guardarTipo = async (event) => {
    event.preventDefault()
    setErrorForm(null)

    const errores = validarTipo()
    setErroresCampos(errores)
    if (Object.keys(errores).length > 0) return

    setGuardando(true)
    try {
      const payload = { nombre: formTipo.nombre.trim() }
      if (modal === 'tipo-editar') {
        await costosService.actualizarTipo(seleccionado.id_tipo_costo, payload)
        setMensaje('Tipo de costo actualizado correctamente.')
      } else {
        await costosService.crearTipo(payload)
        setMensaje('Tipo de costo registrado correctamente.')
      }
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

  const cambiarEstadoTipo = async (tipo) => {
    if (tipo.reservado) return

    if (
      tipo.activo &&
      !window.confirm(
        `¿Desactivar el tipo "${tipo.nombre}"? Dejará de estar disponible al cargar costos.`,
      )
    ) {
      return
    }

    setError(null)
    setMensaje(null)
    try {
      await costosService.cambiarEstadoTipo(tipo.id_tipo_costo, !tipo.activo)
      setMensaje(tipo.activo ? 'Tipo de costo desactivado.' : 'Tipo de costo activado.')
      setRecarga((n) => n + 1)
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Costos</h1>
          <p className="page-subtitle">Registro, filtros y anulación de los gastos del taller.</p>
        </div>
        <button type="button" className="btn btn-primary" onClick={abrirCrear}>
          Nuevo costo
        </button>
      </header>

      {error && <div className="banner banner-error">{error}</div>}
      {mensaje && <div className="banner banner-success">{mensaje}</div>}

      <div className="toolbar">
        <select
          value={id_tipo_costo}
          onChange={cambiarFiltro(setIdTipoCosto)}
          aria-label="Filtrar por tipo de costo"
        >
          <option value="">Todos los tipos</option>
          {tipos.map((tipo) => (
            <option key={tipo.id_tipo_costo} value={tipo.id_tipo_costo}>
              {tipo.nombre}
              {tipo.reservado ? ' (reservado)' : ''}
              {!tipo.activo ? ' (inactivo)' : ''}
            </option>
          ))}
        </select>

        <select value={mes} onChange={cambiarFiltro(setMes)} aria-label="Filtrar por mes">
          <option value="">Todos los meses</option>
          {MESES.map((opcion) => (
            <option key={opcion.value} value={opcion.value}>
              {opcion.label}
            </option>
          ))}
        </select>

        <input
          type="number"
          min="2000"
          max="2100"
          placeholder="Año"
          value={anio}
          onChange={cambiarFiltro(setAnio)}
          aria-label="Filtrar por año"
        />

        <label className="check">
          <input type="checkbox" checked={incluirAnulados} onChange={cambiarIncluirAnulados} />
          Incluir anulados
        </label>
      </div>

      <div className="total-filtro">
        <span>Total del filtro:</span>
        <strong>${formatoNumero(totalFiltro)}</strong>
        <small>No incluye costos anulados.</small>
      </div>

      <div className="card">
        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>Fecha de pago</th>
                <th>Tipo</th>
                <th>Descripción</th>
                <th>Período</th>
                <th>Monto</th>
                <th>Estado</th>
                <th className="table-actions">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {!cargando && costos.length === 0 && (
                <tr>
                  <td colSpan="7" className="empty-state">
                    No se encontraron costos.
                  </td>
                </tr>
              )}
              {costos.map((costo) => (
                <tr key={costo.id_costo} className={costo.anulado ? 'fila-inactiva' : ''}>
                  <td>{formatearFecha(costo.fechaPago)}</td>
                  <td>{costo.nombreTipoCosto ?? '—'}</td>
                  <td>{costo.descripcion}</td>
                  <td>
                    {etiquetaMes(costo.periodoMes)} {costo.periodoAnio ?? ''}
                  </td>
                  <td>${formatoNumero(costo.monto)}</td>
                  <td>
                    <span className={costo.anulado ? 'badge badge-inactivo' : 'badge badge-activo'}>
                      {costo.anulado ? 'Anulado' : 'Activo'}
                    </span>
                    {costo.generadoPorIngreso && (
                      <>
                        {' '}
                        <span className="badge badge-inactivo" title="Generado desde un ingreso de mercadería">
                          Ingreso
                        </span>
                      </>
                    )}
                  </td>
                  <td className="table-actions">
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      onClick={() => abrirEditar(costo)}
                      disabled={costo.anulado || costo.generadoPorIngreso}
                      title={
                        costo.generadoPorIngreso
                          ? 'Los costos generados por un ingreso no se editan'
                          : costo.anulado
                            ? 'Los costos anulados no se editan'
                            : undefined
                      }
                    >
                      Editar
                    </button>
                    {!costo.generadoPorIngreso && (
                      <button
                        type="button"
                        className={costo.anulado ? 'btn btn-secondary btn-sm' : 'btn btn-danger btn-sm'}
                        onClick={() => abrirAnulacion(costo)}
                      >
                        {costo.anulado ? 'Ver anulación' : 'Anular'}
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {cargando && <p className="loading">Cargando costos...</p>}
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
          Página {pagina + 1} de {Math.max(totalPaginas, 1)} · {totalElementos} costos
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

      <div className="seccion-header">
        <div>
          <h2>Tipos de costo</h2>
          <p className="page-subtitle">El tipo Insumos es del sistema: no se edita ni se desactiva.</p>
        </div>
        <button type="button" className="btn btn-secondary" onClick={abrirCrearTipo}>
          Nuevo tipo
        </button>
      </div>

      <div className="card">
        <div className="table-wrapper">
          <table className="table table-compact">
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Estado</th>
                <th>Origen</th>
                <th className="table-actions">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {!cargandoTipos && tipos.length === 0 && (
                <tr>
                  <td colSpan="4" className="empty-state">
                    No hay tipos de costo.
                  </td>
                </tr>
              )}
              {tipos.map((tipo) => (
                <tr key={tipo.id_tipo_costo} className={tipo.activo ? '' : 'fila-inactiva'}>
                  <td>{tipo.nombre}</td>
                  <td>
                    <span className={tipo.activo ? 'badge badge-activo' : 'badge badge-inactivo'}>
                      {tipo.activo ? 'Activo' : 'Inactivo'}
                    </span>
                  </td>
                  <td>{tipo.reservado ? <span className="mono">Reservado</span> : 'Manual'}</td>
                  <td className="table-actions">
                    {tipo.reservado ? (
                      <span className="mono">Del sistema</span>
                    ) : (
                      <>
                        <button
                          type="button"
                          className="btn btn-secondary btn-sm"
                          onClick={() => abrirEditarTipo(tipo)}
                        >
                          Editar
                        </button>
                        <button
                          type="button"
                          className={tipo.activo ? 'btn btn-danger btn-sm' : 'btn btn-secondary btn-sm'}
                          onClick={() => cambiarEstadoTipo(tipo)}
                        >
                          {tipo.activo ? 'Desactivar' : 'Activar'}
                        </button>
                      </>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {cargandoTipos && <p className="loading">Cargando tipos de costo...</p>}
      </div>

      {(modal === 'crear' || modal === 'editar') && (
        <Modal
          title={modal === 'crear' ? 'Nuevo costo' : 'Editar costo'}
          subtitle={
            modal === 'editar'
              ? `ID ${seleccionado.id_costo} · cargado por el usuario ${seleccionado.id_usuario}`
              : 'Completá los datos del gasto.'
          }
          onClose={cerrarModal}
        >
          {errorForm && <div className="banner banner-error">{errorForm}</div>}

          <form className="form" onSubmit={guardarCosto} noValidate>
            <label className="field">
              <span>Tipo de costo *</span>
              <select
                value={form.id_tipo_costo}
                onChange={cambiarCampo('id_tipo_costo')}
                aria-invalid={Boolean(erroresCampos.id_tipo_costo)}
                required
                autoFocus
              >
                <option value="">Seleccioná un tipo...</option>
                {tiposParaCargar.map((tipo) => (
                  <option key={tipo.id_tipo_costo} value={tipo.id_tipo_costo}>
                    {tipo.nombre}
                  </option>
                ))}
              </select>
              {erroresCampos.id_tipo_costo && (
                <small className="field-error">{erroresCampos.id_tipo_costo}</small>
              )}
            </label>

            <label className="field">
              <span>Descripción *</span>
              <input
                type="text"
                value={form.descripcion}
                onChange={cambiarCampo('descripcion')}
                maxLength={LARGO_MAXIMO_DESCRIPCION}
                aria-invalid={Boolean(erroresCampos.descripcion)}
                required
              />
              {erroresCampos.descripcion && (
                <small className="field-error">{erroresCampos.descripcion}</small>
              )}
            </label>

            <div className="form-row">
              <label className="field">
                <span>Monto *</span>
                <input
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={form.monto}
                  onChange={cambiarCampo('monto')}
                  aria-invalid={Boolean(erroresCampos.monto)}
                  required
                />
                {erroresCampos.monto && <small className="field-error">{erroresCampos.monto}</small>}
              </label>

              <label className="field">
                <span>Fecha de pago *</span>
                <input
                  type="date"
                  value={form.fechaPago}
                  onChange={cambiarCampo('fechaPago')}
                  aria-invalid={Boolean(erroresCampos.fechaPago)}
                  required
                />
                {erroresCampos.fechaPago && (
                  <small className="field-error">{erroresCampos.fechaPago}</small>
                )}
              </label>
            </div>

            <div className="form-row">
              <label className="field">
                <span>Mes del período *</span>
                <select
                  value={form.periodoMes}
                  onChange={cambiarCampo('periodoMes')}
                  aria-invalid={Boolean(erroresCampos.periodoMes)}
                  required
                >
                  <option value="">Seleccioná un mes...</option>
                  {MESES.map((opcion) => (
                    <option key={opcion.value} value={opcion.value}>
                      {opcion.label}
                    </option>
                  ))}
                </select>
                {erroresCampos.periodoMes && (
                  <small className="field-error">{erroresCampos.periodoMes}</small>
                )}
              </label>

              <label className="field">
                <span>Año del período *</span>
                <input
                  type="number"
                  min="2000"
                  max="2100"
                  step="1"
                  value={form.periodoAnio}
                  onChange={cambiarCampo('periodoAnio')}
                  aria-invalid={Boolean(erroresCampos.periodoAnio)}
                  required
                />
                {erroresCampos.periodoAnio && (
                  <small className="field-error">{erroresCampos.periodoAnio}</small>
                )}
              </label>
            </div>

            <label className="field">
              <span>Observaciones</span>
              <input
                type="text"
                value={form.observaciones}
                onChange={cambiarCampo('observaciones')}
                maxLength={LARGO_MAXIMO_OBSERVACIONES}
                aria-invalid={Boolean(erroresCampos.observaciones)}
              />
              {erroresCampos.observaciones && (
                <small className="field-error">{erroresCampos.observaciones}</small>
              )}
            </label>

            {modal === 'editar' && tipoActualNoDisponible && (
              <div className="aviso" role="alert">
                El tipo actual está desactivado o es del sistema. Elegí otro tipo para guardar.
              </div>
            )}

            {modal === 'editar' && (
              <div className="aviso" role="note">
                El usuario que cargó el costo no se puede cambiar.
              </div>
            )}

            <div className="modal-footer">
              <button type="button" className="btn btn-secondary" onClick={cerrarModal}>
                Cancelar
              </button>
              <button type="submit" className="btn btn-primary" disabled={guardando}>
                {guardando ? 'Guardando...' : 'Guardar'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {modal === 'anulacion' && (
        <Modal
          title={seleccionado.anulado ? 'Detalle de la anulación' : 'Anular costo'}
          subtitle={`ID ${seleccionado.id_costo} · ${seleccionado.descripcion}`}
          onClose={cerrarModal}
        >
          {errorForm && <div className="banner banner-error">{errorForm}</div>}

          {seleccionado.anulado ? (
            <div className="form">
              <div className="aviso" role="status">
                Este costo está anulado y no suma al total.
              </div>
              <p>
                <strong>Motivo:</strong> {seleccionado.motivoAnulacion ?? '—'}
              </p>
              <p>
                <strong>Usuario que anuló:</strong> {seleccionado.id_usuario_anulacion ?? '—'}
              </p>
              <p>
                <strong>Fecha:</strong> {formatearFechaHora(seleccionado.fechaAnulacion)}
              </p>
              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={cerrarModal}>
                  Cerrar
                </button>
              </div>
            </div>
          ) : (
            <form className="form" onSubmit={guardarAnulacion} noValidate>
              <div className="aviso" role="alert">
                La anulación es definitiva: el costo dejará de sumar al total.
              </div>

              <label className="field">
                <span>Motivo *</span>
                <input
                  type="text"
                  value={motivo}
                  onChange={(event) => {
                    setMotivo(event.target.value)
                    if (erroresCampos.motivo) setErroresCampos({})
                  }}
                  maxLength={LARGO_MAXIMO_DESCRIPCION}
                  aria-invalid={Boolean(erroresCampos.motivo)}
                  required
                  autoFocus
                />
                {erroresCampos.motivo && <small className="field-error">{erroresCampos.motivo}</small>}
              </label>

              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={cerrarModal}>
                  Cancelar
                </button>
                <button type="submit" className="btn btn-danger" disabled={guardando}>
                  {guardando ? 'Anulando...' : 'Anular costo'}
                </button>
              </div>
            </form>
          )}
        </Modal>
      )}

      {(modal === 'tipo-crear' || modal === 'tipo-editar') && (
        <Modal
          title={modal === 'tipo-crear' ? 'Nuevo tipo de costo' : 'Editar tipo de costo'}
          subtitle={modal === 'tipo-editar' ? `ID ${seleccionado.id_tipo_costo}` : 'Solo el nombre es editable.'}
          onClose={cerrarModal}
        >
          {errorForm && <div className="banner banner-error">{errorForm}</div>}

          <form className="form" onSubmit={guardarTipo} noValidate>
            <label className="field">
              <span>Nombre *</span>
              <input
                type="text"
                value={formTipo.nombre}
                onChange={cambiarNombreTipo}
                maxLength={LARGO_MAXIMO_NOMBRE_TIPO}
                aria-invalid={Boolean(erroresCampos.nombre)}
                required
                autoFocus
              />
              {erroresCampos.nombre && <small className="field-error">{erroresCampos.nombre}</small>}
            </label>

            <div className="modal-footer">
              <button type="button" className="btn btn-secondary" onClick={cerrarModal}>
                Cancelar
              </button>
              <button type="submit" className="btn btn-primary" disabled={guardando}>
                {guardando ? 'Guardando...' : 'Guardar'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </section>
  )
}

export default CostosPage
