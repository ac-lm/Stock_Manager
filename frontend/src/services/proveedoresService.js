import { api } from './api'

const PROVEEDORES = '/proveedores'

export const proveedoresService = {
  listar: () => api.get(PROVEEDORES),
  obtener: (id_proveedor) => api.get(`${PROVEEDORES}/${id_proveedor}`),
  crear: (dto) => api.post(PROVEEDORES, dto),
  actualizar: (id_proveedor, dto) => api.put(`${PROVEEDORES}/${id_proveedor}`, dto),
  eliminar: (id_proveedor) => api.delete(`${PROVEEDORES}/${id_proveedor}`),
}
