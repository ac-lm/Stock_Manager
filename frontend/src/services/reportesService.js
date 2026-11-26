import { api } from './api.js'

const REPORTES = '/reportes'

function paramsVentasPorPeriodo({ desde, hasta, agrupacion }) {
  const params = new URLSearchParams({ desde, hasta, agrupacion })
  return params.toString()
}

export const reportesService = {
  ventasPorPeriodo: (filtros) =>
    api.get(`${REPORTES}/ventas-por-periodo?${paramsVentasPorPeriodo(filtros)}`),

  exportar: ({ reporte, formato, ...filtros }) => {
    const params = new URLSearchParams({ reporte, formato, ...filtros })
    return api.getBlob(`${REPORTES}/exportar?${params.toString()}`)
  },
}
