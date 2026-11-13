import { api } from './api'

const INSUMOS = '/catalogo/insumos'
const PRODUCTOS = '/catalogo/productos'

export const insumosService = {
  listar: () => api.get(INSUMOS),
  obtener: (id_insumo) => api.get(`${INSUMOS}/${id_insumo}`),
  crear: (dto) => api.post(INSUMOS, dto),
  actualizar: (id_insumo, dto) => api.put(`${INSUMOS}/${id_insumo}`, dto),
  eliminar: (id_insumo) => api.delete(`${INSUMOS}/${id_insumo}`),
}

export const productosService = {
  listar: () => api.get(PRODUCTOS),
  obtener: (id_producto) => api.get(`${PRODUCTOS}/${id_producto}`),
  crear: (dto) => api.post(PRODUCTOS, dto),
  actualizar: (id_producto, dto) => api.put(`${PRODUCTOS}/${id_producto}`, dto),
  eliminar: (id_producto) => api.delete(`${PRODUCTOS}/${id_producto}`),

  listarInsumos: (id_producto) => api.get(`${PRODUCTOS}/${id_producto}/insumos`),
  asignarInsumo: (id_producto, dto) => api.post(`${PRODUCTOS}/${id_producto}/insumos`, dto),
  actualizarInsumo: (id_producto, id_producto_insumo, dto) =>
    api.put(`${PRODUCTOS}/${id_producto}/insumos/${id_producto_insumo}`, dto),
  eliminarInsumo: (id_producto, id_producto_insumo) =>
    api.delete(`${PRODUCTOS}/${id_producto}/insumos/${id_producto_insumo}`),
}
