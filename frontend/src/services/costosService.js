import { api } from './api.js'

const COSTOS = '/costos'
const TIPOS_COSTO = '/tipos-costo'

function paramsListado({ id_tipo_costo = '', mes = '', anio = '', incluirAnulados = false, page = 0, size = 10 } = {}) {
  const params = new URLSearchParams({ page, size, incluirAnulados })

  if (id_tipo_costo !== '') params.set('id_tipo_costo', id_tipo_costo)
  if (mes !== '') params.set('mes', mes)
  if (anio !== '') params.set('anio', anio)

  return params
}

function paramsTotal({ id_tipo_costo = '', mes = '', anio = '' } = {}) {
  const params = new URLSearchParams()

  if (id_tipo_costo !== '') params.set('id_tipo_costo', id_tipo_costo)
  if (mes !== '') params.set('mes', mes)
  if (anio !== '') params.set('anio', anio)

  return params
}

export const costosService = {
  listar: (filtros = {}) => api.get(`${COSTOS}?${paramsListado(filtros)}`),

  total: (filtros = {}) => {
    const params = paramsTotal(filtros).toString()
    return api.get(params ? `${COSTOS}/total?${params}` : `${COSTOS}/total`)
  },

  obtener: (id_costo) => api.get(`${COSTOS}/${id_costo}`),

  crear: (dto) => api.post(COSTOS, dto),

  actualizar: (id_costo, dto) => api.put(`${COSTOS}/${id_costo}`, dto),

  anular: (id_costo, motivo) => api.patch(`${COSTOS}/${id_costo}/anulacion`, { motivo }),

  listarTipos: ({ incluirInactivos = true } = {}) =>
    api.get(`${TIPOS_COSTO}?${new URLSearchParams({ incluirInactivos })}`),

  crearTipo: (dto) => api.post(TIPOS_COSTO, dto),

  actualizarTipo: (id_tipo_costo, dto) => api.put(`${TIPOS_COSTO}/${id_tipo_costo}`, dto),

  cambiarEstadoTipo: (id_tipo_costo, activo) =>
    api.patch(`${TIPOS_COSTO}/${id_tipo_costo}/estado`, { activo }),
}
