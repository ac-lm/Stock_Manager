import { api } from './api.js'

const AUDITORIA = '/auditoria'

export const auditoriaService = {
    listar: ({
        id_usuario = '',
        accion = '',
        tabla = '',
        desde = '',
        hasta = '',
        page = 0,
        size = 20,
    } = {}) => {
        const params = new URLSearchParams({ page, size })
        if (id_usuario !== '') params.set('id_usuario', id_usuario)
        if (accion) params.set('accion', accion)
        if (tabla.trim()) params.set('tabla', tabla.trim())
        if (desde) params.set('desde', desde)
        if (hasta) params.set('hasta', hasta)
        return api.get(`${AUDITORIA}?${params}`)
    },

    obtener: (id_auditoria) => api.get(`${AUDITORIA}/${id_auditoria}`),

    crear: (dto) => api.post(AUDITORIA, dto),

    corregir: ({ id_auditoria_corregida, detalle, cambios }) =>
        api.post(AUDITORIA, {
            accion: 'CORRECCION_AUDITORIA',
            tabla: 'Auditoria',
            id_registro: null,
            detalle,
            cambios,
            id_auditoria_corregida,
        }),
}
