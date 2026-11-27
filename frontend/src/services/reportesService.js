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

function paramsVentasPorCliente({ desde, hasta, id_cliente, tipo_cliente }) {
  const params = new URLSearchParams({ desde, hasta })
  if (id_cliente) params.set('id_cliente', id_cliente)
  if (tipo_cliente) params.set('tipo_cliente', tipo_cliente)
  return params.toString()
}

function paramsVentasPorVendedor({ desde, hasta, id_vendedor }) {
  const params = new URLSearchParams({ desde, hasta })
  if (id_vendedor) params.set('id_vendedor', id_vendedor)
  return params.toString()
}

function paramsCierresCaja({ desde, hasta, id_usuario_cierre, estado, medio_pago }) {
  const params = new URLSearchParams({ desde, hasta })
  if (id_usuario_cierre) params.set('id_usuario_cierre', id_usuario_cierre)
  if (estado) params.set('estado', estado)
  if (medio_pago) params.set('medio_pago', medio_pago)
  return params.toString()
}

function paramsComparacionEstacional({ anios, id_producto, categoria }) {
  const params = new URLSearchParams()
  if (anios?.length) params.set('anios', anios.join(','))
  if (id_producto) params.set('id_producto', id_producto)
  if (categoria) params.set('categoria', categoria)
  return params.toString()
}

function paramsStockActual({ tipo, categoria, solo_bajo_minimo }) {
  const params = new URLSearchParams()
  if (tipo && tipo !== 'TODOS') params.set('tipo', tipo)
  if (categoria) params.set('categoria', categoria)
  params.set('solo_bajo_minimo', String(Boolean(solo_bajo_minimo)))
  return params.toString()
}

function paramsMovimientosStock({
  desde,
  hasta,
  tipo_articulo,
  id_articulo,
  tipo_movimiento,
  id_proveedor,
}) {
  const params = new URLSearchParams({ desde, hasta })
  if (tipo_articulo && tipo_articulo !== 'TODOS') params.set('tipo_articulo', tipo_articulo)
  if (id_articulo) params.set('id_articulo', id_articulo)
  if (tipo_movimiento && tipo_movimiento !== 'TODOS') params.set('tipo_movimiento', tipo_movimiento)
  if (id_proveedor) params.set('id_proveedor', id_proveedor)
  return params.toString()
}

function paramsCostosPorTipo({ desde, hasta, id_tipo_costo }) {
  const params = new URLSearchParams({ desde, hasta })
  if (id_tipo_costo) params.set('id_tipo_costo', id_tipo_costo)
  return params.toString()
}

export const reportesService = {
  ventasPorPeriodo: (filtros) =>
    api.get(`${REPORTES}/ventas-por-periodo?${paramsVentasPorPeriodo(filtros)}`),

  ventasPorProducto: (filtros) =>
    api.get(`${REPORTES}/ventas-por-producto?${paramsVentasPorProducto(filtros)}`),

  ventasPorCliente: (filtros) =>
    api.get(`${REPORTES}/ventas-por-cliente?${paramsVentasPorCliente(filtros)}`),

  ventasPorVendedor: (filtros) =>
    api.get(`${REPORTES}/ventas-por-vendedor?${paramsVentasPorVendedor(filtros)}`),

  cierresCaja: (filtros) =>
    api.get(`${REPORTES}/cierres-caja?${paramsCierresCaja(filtros)}`),

  comparacionEstacional: (filtros) =>
    api.get(`${REPORTES}/evolucion-ventas?${paramsComparacionEstacional(filtros)}`),

  stockActual: (filtros) =>
    api.get(`${REPORTES}/stock-actual?${paramsStockActual(filtros)}`),

  movimientosStock: (filtros) =>
    api.get(`${REPORTES}/movimientos-stock?${paramsMovimientosStock(filtros)}`),

  costosPorTipo: (filtros) =>
    api.get(`${REPORTES}/costos-por-tipo?${paramsCostosPorTipo(filtros)}`),

  exportar: ({ reporte, formato, ...filtros }) => {
    const params = new URLSearchParams({ reporte, formato, ...filtros })
    return api.getBlob(`${REPORTES}/exportar?${params.toString()}`)
  },
}
