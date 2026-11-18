import { api } from './api.js'

const USUARIOS = '/usuarios'

export const usuariosService = {
    listar: ({ busqueda = '', incluirInactivos = false, page = 0, size = 10 } = {}) => {
        const params = new URLSearchParams({ page, size, incluirInactivos })
        if (busqueda.trim()) params.set('busqueda', busqueda.trim())
        return api.get(`${USUARIOS}?${params}`)
    },

    obtener: (id_usuario) => api.get(`${USUARIOS}/${id_usuario}`),

    crear: (dto) => api.post(USUARIOS, dto),

    actualizar: (id_usuario, dto) => api.put(`${USUARIOS}/${id_usuario}`, dto),

    cambiarPassword: (id_usuario, password) =>
        api.put(`${USUARIOS}/${id_usuario}/password`, { password }),

    cambiarEstado: (id_usuario, activo) =>
        api.patch(`${USUARIOS}/${id_usuario}/estado`, { activo }),
}
