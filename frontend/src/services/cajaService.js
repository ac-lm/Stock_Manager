import { api } from './api.js'

const CAJA = '/caja'

function paramsHistorial({
  estado = '',
  id_usuario = '',
  desde = '',
  hasta = '',
  page = 0,
  size = 10,
} = {}) {
  const params = new URLSearchParams({ page, size })

  if (estado !== '') params.set('estado', estado)
  if (id_usuario !== '') params.set('id_usuario', id_usuario)
  if (desde !== '') params.set('desde', desde)
  if (hasta !== '') params.set('hasta', hasta)

  return params
}

export const cajaService = {
  actual: () => api.get(`${CAJA}/actual`),

  abrir: (payload) => api.post(`${CAJA}/sesiones`, payload),

  registrarEgreso: (id_sesion, payload) =>
    api.post(`${CAJA}/sesiones/${id_sesion}/movimientos`, payload),

  cerrar: (id_sesion, payload) => api.post(`${CAJA}/sesiones/${id_sesion}/cierre`, payload),

  revisar: (id_sesion, payload) =>
    api.post(`${CAJA}/sesiones/${id_sesion}/revision`, payload),

  listar: (filtros = {}) => api.get(`${CAJA}/sesiones?${paramsHistorial(filtros)}`),

  obtener: (id_sesion) => api.get(`${CAJA}/sesiones/${id_sesion}`),
}
