import { api } from './api.js'

const STOCK = '/stock'

function paramsHistorial({
  tipo = '', motivo = '', id_producto = '', id_insumo = '', id_usuario = '',
  desde = '', hasta = '', page = 0, size = 10,
} = {}) {
  const params = new URLSearchParams({ page, size })

  if (tipo) params.set('tipo', tipo)
  if (motivo) params.set('motivo', motivo)
  if (id_producto !== '') params.set('id_producto', id_producto)
  if (id_insumo !== '') params.set('id_insumo', id_insumo)
  if (id_usuario !== '') params.set('id_usuario', id_usuario)
  if (desde) params.set('desde', desde)
  if (hasta) params.set('hasta', hasta)

  return params
}

export const stockService = {
  listarInsumos: () => api.get(`${STOCK}/insumos`),
  listarProductos: () => api.get(`${STOCK}/productos`),
  registrarMovimiento: (entidad, payload) =>
    api.post(`${STOCK}/${entidad}/movimientos`, payload),
  kardex: (entidad, id) => api.get(`${STOCK}/${entidad}/${id}/movimientos`),
  historial: (filtros = {}) => api.get(`${STOCK}/movimientos?${paramsHistorial(filtros)}`),
}
