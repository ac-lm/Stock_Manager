import { api } from './api.js'

const CLIENTES = '/clientes'

export const clientesService = {
    listar: ({ busqueda = '', incluirInactivos = false, page = 0, size = 10 } = {}) => {
        const params = new URLSearchParams({ page, size, incluirInactivos })
        if (busqueda.trim()) params.set('busqueda', busqueda.trim())
        return api.get(`${CLIENTES}?${params}`)
    },

    obtener: (id_cliente) => api.get(`${CLIENTES}/${id_cliente}`),

    similares: (nombre) => {
        const params = new URLSearchParams({ nombre })
        return api.get(`${CLIENTES}/similares?${params}`)
    },

    crear: (dto) => api.post(CLIENTES, dto),
    actualizar: (id_cliente, dto) => api.put(`${CLIENTES}/${id_cliente}`, dto),
    cambiarEstado: (id_cliente, activo) =>
        api.patch(`${CLIENTES}/${id_cliente}/estado`, { activo }),
}