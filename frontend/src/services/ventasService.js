import { api } from './api.js'

const VENTAS = '/ventas'

function paramsListar({ estado = '', id_cliente = '', desde = '', hasta = '', page = 0, size = 10 } = {}) {
  const params = new URLSearchParams({ page, size })

  if (estado) params.set('estado', estado)
  if (id_cliente !== '') params.set('id_cliente', id_cliente)
  if (desde) params.set('desde', desde)
  if (hasta) params.set('hasta', hasta)

  return params
}

export const ventasService = {
  listar: (filtros = {}) => api.get(`${VENTAS}?${paramsListar(filtros)}`),

  obtener: (id_venta) => api.get(`${VENTAS}/${id_venta}`),

  crear: (dto) => api.post(VENTAS, dto),

  anular: (id_venta, motivo) => api.put(`${VENTAS}/${id_venta}/anular`, { motivo }),
}
