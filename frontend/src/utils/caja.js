import { formatoNumero } from './format'

export const ESTADOS_CAJA = [
  { value: 'ABIERTA', label: 'Abierta' },
  { value: 'PENDIENTE_REVISION', label: 'Pendiente de revisión' },
  { value: 'CERRADA', label: 'Cerrada' },
]

export const CONCEPTOS_EGRESO = [
  { value: 'RETIRO', label: 'Retiro' },
  { value: 'GASTO', label: 'Gasto' },
]

export const MEDIOS_PAGO = [
  { value: 'EFECTIVO', label: 'Efectivo' },
  { value: 'TRANSFERENCIA', label: 'Transferencia' },
]

const buscarEtiqueta = (opciones, valor) =>
  opciones.find((opcion) => opcion.value === valor)?.label ?? valor ?? '—'

export const etiquetaEstado = (estado) => buscarEtiqueta(ESTADOS_CAJA, estado)
export const etiquetaConcepto = (concepto) => buscarEtiqueta(CONCEPTOS_EGRESO, concepto)
export const etiquetaMedio = (medio) => buscarEtiqueta(MEDIOS_PAGO, medio)

export const claseEstado = (estado) => {
  if (estado === 'ABIERTA') return 'badge badge-abierta'
  if (estado === 'PENDIENTE_REVISION') return 'badge badge-pendiente'
  if (estado === 'CERRADA') return 'badge badge-cerrada'
  return 'badge badge-inactivo'
}

export const formatearFecha = (valor) => {
  if (!valor) return '—'
  const [anio, mes, dia] = valor.split('-')
  return dia && mes && anio ? `${dia}/${mes}/${anio}` : valor
}

export const formatearHora = (valor) => {
  if (!valor) return '—'
  return valor.slice(0, 5)
}

export const formatearFechaHora = (fecha, hora) => {
  if (!fecha) return '—'
  return `${formatearFecha(fecha)} ${hora ? formatearHora(hora) : ''}`.trim()
}

export const formatearFechaHoraISO = (valor) => {
  if (!valor) return '—'
  const [fecha, hora] = valor.split('T')
  return `${formatearFecha(fecha)} ${hora ? formatearHora(hora) : ''}`.trim()
}

export const formatearMonto = (valor) => `$${formatoNumero(valor)}`

export const diferenciaConSigno = (valor) => {
  const numero = Number(valor ?? 0)
  if (numero === 0) return formatearMonto(0)
  return `${numero > 0 ? '+' : '−'}${formatearMonto(Math.abs(numero))}`
}

export const claseDiferencia = (valor) => {
  const numero = Number(valor ?? 0)
  if (numero < 0) return 'mono monto-negativo'
  if (numero > 0) return 'mono monto-positivo'
  return 'mono'
}
