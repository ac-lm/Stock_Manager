import { api } from './api.js'

const REPORTES = '/reportes'

function paramsVentasPorPeriodo({ desde, hasta, agrupacion }) {
  const params = new URLSearchParams({ desde, hasta, agrupacion })
  return params.toString()
}

function paramsVentasPorProducto({ desde, hasta, id_producto, categoria, top }) {
  const params = new URLSearchParams({ desde, hasta, top })
  if (id_producto) params.set('id_producto', id_producto)
  if (categoria) params.set('categoria', categoria)
  return params.toString()
}

export const reportesService = {
  ventasPorPeriodo: (filtros) =>
    api.get(`${REPORTES}/ventas-por-periodo?${paramsVentasPorPeriodo(filtros)}`),

  ventasPorProducto: (filtros) =>
    api.get(`${REPORTES}/ventas-por-producto?${paramsVentasPorProducto(filtros)}`),

  exportar: ({ reporte, formato, ...filtros }) => {
    const params = new URLSearchParams({ reporte, formato, ...filtros })
    return api.getBlob(`${REPORTES}/exportar?${params.toString()}`)
  },
}
