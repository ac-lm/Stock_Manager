import { api } from './api.js'

const REPORTES = '/reportes'

function paramsVentasPorPeriodo({ desde, hasta, agrupacion }) {
  const params = new URLSearchParams({ desde, hasta, agrupacion })
  return params.toString()
}

export const reportesService = {
  ventasPorPeriodo: (filtros) =>
    api.get(`${REPORTES}/ventas-por-periodo?${paramsVentasPorPeriodo(filtros)}`),
}
