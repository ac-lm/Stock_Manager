import { useEffect, useState } from 'react'
import Modal from '../components/Modal'
import { auditoriaService } from '../services/auditoriaService'

const TAMANIO_PAGINA = 20

const ACCIONES = [
  { value: 'ALTA', label: 'Alta' },
  { value: 'MODIFICACION', label: 'Modificación' },
  { value: 'BAJA', label: 'Baja' },
  { value: 'LOGIN', label: 'Inicio de sesión' },
  { value: 'APERTURA_CAJA', label: 'Apertura de caja' },
  { value: 'CIERRE_CAJA', label: 'Cierre de caja' },
  { value: 'CORRECCION_CAJA', label: 'Corrección de caja' },
  { value: 'ANULACION_VENTA', label: 'Anulación de venta' },
  { value: 'AJUSTE_STOCK', label: 'Ajuste de stock' },
  { value: 'CORRECCION_AUDITORIA', label: 'Corrección de auditoría' },
  { value: 'OTRO', label: 'Otro' },
]

// Las altas, bajas, modificaciones y los logins son automáticos. Las correcciones se
// hacen desde el botón "Corregir" de cada fila, no desde el formulario general.
const ACCIONES_MANUALES = ACCIONES.filter((accion) =>
  !['ALTA', 'MODIFICACION', 'BAJA', 'LOGIN', 'CORRECCION_AUDITORIA'].includes(accion.value),
)

const ACCIONES_AUTOMATICAS = ['ALTA', 'MODIFICACION', 'BAJA', 'LOGIN']

const etiquetaAccion = (accion) => ACCIONES.find((item) => item.value === accion)?.label ?? accion

const FILTROS_VACIOS = {
  id_usuario: '',
  accion: '',
  tabla: '',
  periodo: 'cualquiera',
  desde: '',
  hasta: '',
}

const crearFilaVacia = () => ({ campo: '', anterior: '', nuevo: '' })

const crearFormVacio = () => ({
  accion: 'OTRO',
  tabla: '',
  id_registro: '',
  detalle: '',
  cambios: [crearFilaVacia()],
})

const fechaLegible = (fechaHora) =>
  fechaHora ? String(fechaHora).replace('T', ' ').slice(0, 19) : '—'

const fechaISO = (fecha) =>
  `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}-${String(
    fecha.getDate(),
  ).padStart(2, '0')}`

// La API espera fechas completas; los presets las arman solos y el rango usa días enteros.
function rangoDeFechas(periodo, desde, hasta) {
  if (periodo === 'rango') {
    return {
      desde: desde ? `${desde}T00:00:00` : '',
      hasta: hasta ? `${hasta}T23:59:59` : '',
    }
  }

  const hoy = new Date()
  if (periodo === 'hoy') {
    return { desde: `${fechaISO(hoy)}T00:00:00`, hasta: `${fechaISO(hoy)}T23:59:59` }
  }

  const dias = Number(periodo)
  if (dias > 0) {
    const inicio = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate() - (dias - 1))
    return { desde: `${fechaISO(inicio)}T00:00:00`, hasta: `${fechaISO(hoy)}T23:59:59` }
  }

  return { desde: '', hasta: '' }
}

function textoValor(valor) {
  if (valor === null || valor === undefined || valor === '') return '—'
  if (typeof valor === 'object') return JSON.stringify(valor)
  return String(valor)
}

// La API guarda { campo: { anterior, nuevo } }; acá se convierte en filas legibles.
function filasDeCambios(cambios) {
  if (!cambios || typeof cambios !== 'object' || Array.isArray(cambios)) return []
  return Object.entries(cambios).map(([campo, valor]) => {
    if (
      valor &&
      typeof valor === 'object' &&
      !Array.isArray(valor) &&
      ('anterior' in valor || 'nuevo' in valor)
    ) {
      return {
        campo,
        anterior: textoValor(valor.anterior),
        nuevo: textoValor(valor.nuevo),
      }
    }
    return { campo, anterior: '—', nuevo: textoValor(valor) }
  })
}

function resumenCambios(cambios) {
  const cantidad = filasDeCambios(cambios).length
  return `${cantidad} ${cantidad === 1 ? 'campo' : 'campos'}`
}

function valorDelTexto(texto) {
  const limpio = texto.trim()
  if (limpio === '') return null
  if (limpio.toLowerCase() === 'true') return true
  if (limpio.toLowerCase() === 'false') return false
  if (/^-?\d+(\.\d+)?$/.test(limpio)) return Number(limpio)
  return limpio
}

function EditorCambios({ titulo, ayuda, filas, onChange, onAgregar, onQuitar }) {
  return (
    <div className="field">
      <span>{titulo}</span>
      <small className="field-help">{ayuda}</small>
      <div className="filas-cambios">
        {filas.map((fila, indice) => (
          <div className="fila-cambio" key={`fila-${indice}`}>
            <input
              type="text"
              value={fila.campo}
              onChange={onChange(indice, 'campo')}
              placeholder="Campo (ej. precio)"
              aria-label={`Campo ${indice + 1}`}
            />
            <input
              type="text"
              value={fila.anterior}
              onChange={onChange(indice, 'anterior')}
              placeholder="Antes"
              aria-label={`Valor anterior ${indice + 1}`}
            />
            <input
              type="text"
              value={fila.nuevo}
              onChange={onChange(indice, 'nuevo')}
              placeholder="Ahora"
              aria-label={`Valor nuevo ${indice + 1}`}
            />
            <button
              type="button"
              className="btn btn-ghost"
              onClick={() => onQuitar(indice)}
              disabled={filas.length === 1}
              aria-label={`Quitar cambio ${indice + 1}`}
            >
              ×
            </button>
          </div>
        ))}
      </div>
      <button type="button" className="btn btn-secondary btn-sm" onClick={onAgregar}>
        + Agregar otro campo
      </button>
    </div>
  )
}

function AuditoriaPage() {
  const [filtros, setFiltros] = useState(FILTROS_VACIOS)
  const [filtrosAplicados, setFiltrosAplicados] = useState(FILTROS_VACIOS)
  const [registros, setRegistros] = useState([])
  const [pagina, setPagina] = useState(0)
  const [totalPaginas, setTotalPaginas] = useState(0)
  const [totalElementos, setTotalElementos] = useState(0)
  const [recarga, setRecarga] = useState(0)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)

  const [modal, setModal] = useState(null)
  const [seleccionado, setSeleccionado] = useState(null)
  const [corregido, setCorregido] = useState(null)
  const [form, setForm] = useState(crearFormVacio)
  const [guardando, setGuardando] = useState(false)
  const [errorForm, setErrorForm] = useState(null)

  useEffect(() => {
    let cancelado = false

    const cargar = async () => {
      setCargando(true)
      try {
        const data = await auditoriaService.listar({
          ...filtrosAplicados,
          page: pagina,
          size: TAMANIO_PAGINA,
        })
        if (!cancelado) {
          setRegistros(data.content)
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
  }, [filtrosAplicados, pagina, recarga])

  const cambiarFiltro = (campo) => (event) => {
    setFiltros((prev) => ({ ...prev, [campo]: event.target.value }))
  }

  const aplicarFiltros = (event) => {
    event.preventDefault()
    setPagina(0)
    setFiltrosAplicados({ ...filtros, ...rangoDeFechas(filtros.periodo, filtros.desde, filtros.hasta) })
  }

  const limpiarFiltros = () => {
    setFiltros(FILTROS_VACIOS)
    setFiltrosAplicados(FILTROS_VACIOS)
    setPagina(0)
  }

  const cerrarModal = () => {
    setModal(null)
    setCorregido(null)
    setErrorForm(null)
  }

  const abrirDetalle = (registro) => {
    setSeleccionado(registro)
    setModal('detalle')
  }

  const abrirCrear = () => {
    setForm(crearFormVacio())
    setErrorForm(null)
    setModal('crear')
  }

  const abrirCorregir = (registro) => {
    const filas = filasDeCambios(registro.cambios)
    setCorregido(registro)
    setForm({
      accion: 'CORRECCION_AUDITORIA',
      tabla: 'Auditoria',
      id_registro: '',
      detalle: '',
      cambios: filas.length
        ? filas.map((fila) => ({
            campo: fila.campo,
            anterior: fila.nuevo === '—' ? '' : fila.nuevo,
            nuevo: '',
          }))
        : [crearFilaVacia()],
    })
    setErrorForm(null)
    setModal('corregir')
  }

  const cambiarFila = (indice, campo) => (event) => {
    setForm((prev) => ({
      ...prev,
      cambios: prev.cambios.map((fila, i) =>
        i === indice ? { ...fila, [campo]: event.target.value } : fila,
      ),
    }))
  }

  const agregarFila = () => {
    setForm((prev) => ({ ...prev, cambios: [...prev.cambios, crearFilaVacia()] }))
  }

  const quitarFila = (indice) => {
    setForm((prev) => ({
      ...prev,
      cambios: prev.cambios.filter((_, i) => i !== indice),
    }))
  }

  const guardar = async (event) => {
    event.preventDefault()
    setErrorForm(null)

    const cambios = {}
    form.cambios.forEach((fila) => {
      const campo = fila.campo.trim()
      if (!campo) return
      cambios[campo] = {
        anterior: valorDelTexto(fila.anterior),
        nuevo: valorDelTexto(fila.nuevo),
      }
    })

    if (modal === 'corregir') {
      if (!form.detalle.trim()) {
        setErrorForm('Contá el motivo de la corrección.')
        return
      }
      if (Object.keys(cambios).length === 0) {
        setErrorForm('Cargá al menos un campo con su valor corregido.')
        return
      }
    }

    setGuardando(true)
    try {
      if (modal === 'corregir') {
        await auditoriaService.corregir({
          id_auditoria_corregida: corregido.id_auditoria,
          detalle: form.detalle.trim(),
          cambios,
        })
      } else {
        await auditoriaService.crear({
          accion: form.accion,
          tabla: form.tabla.trim() || null,
          id_registro: form.id_registro === '' ? null : Number(form.id_registro),
          detalle: form.detalle.trim() || null,
          cambios,
        })
      }
      setModal(null)
      setCorregido(null)
      setRecarga((n) => n + 1)
    } catch (err) {
      setErrorForm(err.message)
    } finally {
      setGuardando(false)
    }
  }

  const filasDetalle = seleccionado ? filasDeCambios(seleccionado.cambios) : []

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Auditoría</h1>
          <p className="page-subtitle">
            Historial de altas, cambios, bajas, accesos y operaciones de caja. No se editan ni se borran.
          </p>
        </div>
        <button type="button" className="btn btn-primary" onClick={abrirCrear}>
          Registrar operación
        </button>
      </header>

      {error && <div className="banner banner-error">{error}</div>}

      <form className="toolbar toolbar-auditoria" onSubmit={aplicarFiltros}>
        <div className="toolbar-grupo">
          <label className="toolbar-campo">
            <span>Tabla</span>
            <input
              type="search"
              placeholder="Ej. Cliente"
              value={filtros.tabla}
              onChange={cambiarFiltro('tabla')}
            />
          </label>
          <label className="toolbar-campo">
            <span>Acción</span>
            <select value={filtros.accion} onChange={cambiarFiltro('accion')}>
              <option value="">Todas</option>
              {ACCIONES.map((accion) => (
                <option key={accion.value} value={accion.value}>
                  {accion.label}
                </option>
              ))}
            </select>
          </label>
          <label className="toolbar-campo">
            <span>Usuario (id)</span>
            <input
              type="number"
              min="1"
              placeholder="Ej. 3"
              value={filtros.id_usuario}
              onChange={cambiarFiltro('id_usuario')}
            />
          </label>
        </div>

        <div className="toolbar-grupo">
          <label className="toolbar-campo">
            <span>Fecha</span>
            <select value={filtros.periodo} onChange={cambiarFiltro('periodo')}>
              <option value="cualquiera">Cualquier fecha</option>
              <option value="hoy">Hoy</option>
              <option value="7">Últimos 7 días</option>
              <option value="30">Últimos 30 días</option>
              <option value="rango">Elegir un rango</option>
            </select>
          </label>
          {filtros.periodo === 'rango' && (
            <>
              <label className="toolbar-campo">
                <span>Desde</span>
                <input
                  type="date"
                  value={filtros.desde}
                  onChange={cambiarFiltro('desde')}
                />
              </label>
              <label className="toolbar-campo">
                <span>Hasta</span>
                <input
                  type="date"
                  value={filtros.hasta}
                  onChange={cambiarFiltro('hasta')}
                />
              </label>
              <small className="field-help toolbar-nota">
                Dejá una fecha vacía para no limitar por ese lado.
              </small>
            </>
          )}
        </div>

        <div className="toolbar-acciones">
          <button type="submit" className="btn btn-primary btn-sm">
            Filtrar
          </button>
          <button type="button" className="btn btn-secondary btn-sm" onClick={limpiarFiltros}>
            Limpiar
          </button>
        </div>
      </form>

      <div className="card">
        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>Fecha y hora</th>
                <th>Usuario</th>
                <th>Acción</th>
                <th>Tabla</th>
                <th>Id registro</th>
                <th>Cambios</th>
                <th>Detalle</th>
                <th className="table-actions">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {!cargando && registros.length === 0 && (
                <tr>
                  <td colSpan="8" className="empty-state">
                    No se encontraron registros de auditoría.
                  </td>
                </tr>
              )}
              {registros.map((registro) => (
                <tr key={registro.id_auditoria}>
                  <td>{fechaLegible(registro.fecha_hora)}</td>
                  <td>{registro.usuario ?? '—'}</td>
                  <td>
                    <span className="badge">{etiquetaAccion(registro.accion)}</span>
                  </td>
                  <td>{registro.tabla ?? '—'}</td>
                  <td>{registro.id_registro ?? '—'}</td>
                  <td>{registro.cambios ? resumenCambios(registro.cambios) : '—'}</td>
                  <td>{registro.detalle ?? '—'}</td>
                  <td className="table-actions">
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      onClick={() => abrirDetalle(registro)}
                    >
                      Ver detalle
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {cargando && <p className="loading">Cargando auditoría...</p>}
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
          Página {pagina + 1} de {Math.max(totalPaginas, 1)} · {totalElementos} registros
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
          title="Detalle de la auditoría"
          subtitle={`Registro ${seleccionado.id_auditoria} · ${fechaLegible(seleccionado.fecha_hora)}`}
          onClose={cerrarModal}
        >
          <dl className="detalle-lista">
            <dt>Acción</dt>
            <dd>{etiquetaAccion(seleccionado.accion)}</dd>
            <dt>Usuario</dt>
            <dd>
              {seleccionado.usuario ?? '—'}
              {seleccionado.id_usuario ? ` (id ${seleccionado.id_usuario})` : ''}
            </dd>
            <dt>Tabla</dt>
            <dd>{seleccionado.tabla ?? '—'}</dd>
            <dt>Id registro</dt>
            <dd>{seleccionado.id_registro ?? '—'}</dd>
            {seleccionado.id_auditoria_corregida && (
              <>
                <dt>Corrige a</dt>
                <dd>Registro #{seleccionado.id_auditoria_corregida}</dd>
              </>
            )}
            <dt>Origen</dt>
            <dd>{seleccionado.origen === 'MANUAL' ? 'Carga manual' : 'Automático'}</dd>
            <dt>Detalle</dt>
            <dd>{seleccionado.detalle ?? '—'}</dd>
          </dl>
          <div className="auditoria-cambios">
            <h3>Cambios registrados</h3>
            {filasDetalle.length === 0 ? (
              <p className="field-help">No se registraron cambios de campos en esta operación.</p>
            ) : (
              <div className="table-wrapper">
                <table className="table table-compact">
                  <thead>
                    <tr>
                      <th>Campo</th>
                      <th>Valor anterior</th>
                      <th>Valor nuevo</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filasDetalle.map((fila, indice) => (
                      <tr key={`${fila.campo}-${indice}`}>
                        <td>{fila.campo}</td>
                        <td>{fila.anterior}</td>
                        <td>{fila.nuevo}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
          <div className="modal-footer">
            {seleccionado.origen === 'MANUAL' && !ACCIONES_AUTOMATICAS.includes(seleccionado.accion) && (
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => abrirCorregir(seleccionado)}
              >
                Corregir
              </button>
            )}
            <button type="button" className="btn btn-secondary" onClick={cerrarModal}>
              Cerrar
            </button>
          </div>
        </Modal>
      )}

      {modal === 'crear' && (
        <Modal
          title="Registrar operación"
          subtitle="La fecha y el usuario se toman de la sesión actual."
          onClose={cerrarModal}
        >
          {errorForm && <div className="banner banner-error">{errorForm}</div>}
          <form className="form" onSubmit={guardar}>
            <label className="field">
              <span>Acción *</span>
              <select
                value={form.accion}
                onChange={(event) => setForm((prev) => ({ ...prev, accion: event.target.value }))}
                required
              >
                {ACCIONES_MANUALES.map((accion) => (
                  <option key={accion.value} value={accion.value}>
                    {accion.label}
                  </option>
                ))}
              </select>
            </label>
            <div className="form-row">
              <label className="field">
                <span>Tabla o recurso</span>
                <input
                  type="text"
                  maxLength={50}
                  value={form.tabla}
                  onChange={(event) => setForm((prev) => ({ ...prev, tabla: event.target.value }))}
                />
              </label>
              <label className="field">
                <span>Id registro</span>
                <input
                  type="number"
                  min="1"
                  value={form.id_registro}
                  onChange={(event) =>
                    setForm((prev) => ({ ...prev, id_registro: event.target.value }))
                  }
                />
              </label>
            </div>
            <label className="field">
              <span>Detalle</span>
              <input
                type="text"
                maxLength={255}
                value={form.detalle}
                onChange={(event) => setForm((prev) => ({ ...prev, detalle: event.target.value }))}
              />
            </label>
            <EditorCambios
              titulo="Cambios"
              ayuda="Escribí el campo que cambió, su valor anterior y el valor nuevo. Podés agregar varios."
              filas={form.cambios}
              onChange={cambiarFila}
              onAgregar={agregarFila}
              onQuitar={quitarFila}
            />
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

      {modal === 'corregir' && (
        <Modal
          title="Corregir registro"
          subtitle={`Registro #${corregido?.id_auditoria} · el original se conserva`}
          onClose={cerrarModal}
        >
          {errorForm && <div className="banner banner-error">{errorForm}</div>}
          <p className="aviso">
            Se va a agregar un registro nuevo explicando qué estaba mal. El original no se
            modifica ni se borra.
          </p>
          <form className="form" onSubmit={guardar}>
            <EditorCambios
              titulo="Valores corregidos"
              ayuda="El valor anterior es el que estaba cargado. Completá el nuevo con el valor correcto."
              filas={form.cambios}
              onChange={cambiarFila}
              onAgregar={agregarFila}
              onQuitar={quitarFila}
            />
            <label className="field">
              <span>Motivo de la corrección *</span>
              <input
                type="text"
                maxLength={255}
                value={form.detalle}
                onChange={(event) => setForm((prev) => ({ ...prev, detalle: event.target.value }))}
                placeholder="Ej. Monto cargado por error"
                required
              />
            </label>
            <div className="modal-footer">
              <button type="button" className="btn btn-secondary" onClick={cerrarModal}>
                Cancelar
              </button>
              <button type="submit" className="btn btn-primary" disabled={guardando}>
                {guardando ? 'Guardando...' : 'Guardar corrección'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </section>
  )
}

export default AuditoriaPage
