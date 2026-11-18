const BASE_URL = '/api'

export const EVENTO_NO_AUTORIZADO = 'api:no-autorizado'

// Convierte "campo: mensaje" en un mapa para poder marcar cada input.
function extraerErroresPorCampo(detalles) {
  const campos = {}

  for (const detalle of detalles) {
    const separador = detalle.indexOf(':')
    if (separador <= 0) continue

    const campo = detalle.slice(0, separador).trim()
    const mensaje = detalle.slice(separador + 1).trim()
    if (campo && mensaje) campos[campo] = mensaje
  }

  return campos
}

async function construirError(response) {
  let data = null
  try {
    data = await response.json()
  } catch {
    // La respuesta no trae JSON (por ejemplo, un 500 sin cuerpo).
  }

  const detalles = Array.isArray(data?.detalles) ? data.detalles : []
  const mensajeBase =
    data?.mensaje ??
    data?.message ??
    data?.error ??
    `Error ${response.status}${response.statusText ? ` (${response.statusText})` : ''}`

  const error = new Error(detalles.length ? `${mensajeBase}: ${detalles.join('. ')}` : mensajeBase)
  error.status = response.status
  error.detalles = detalles
  error.campos = extraerErroresPorCampo(detalles)
  return error
}

async function request(path, { body, headers, silenciarNoAutorizado = false, ...options } = {}) {
  const response = await fetch(`${BASE_URL}${path}`, {
    ...options,
    // La sesión viaja en una cookie HttpOnly; hay que pedirle al navegador que la envíe.
    credentials: 'include',
    headers: {
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      ...headers,
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  // En el login y en la consulta inicial de sesión el 401 es esperado y no debe
  // disparar la redirección global al login.
  if (response.status === 401 && !silenciarNoAutorizado) {
    window.dispatchEvent(new Event(EVENTO_NO_AUTORIZADO))
  }

  if (!response.ok) {
    throw await construirError(response)
  }

  if (response.status === 204) return null

  const texto = await response.text()
  return texto ? JSON.parse(texto) : null
}

export const api = {
  get: (path, options) => request(path, { ...options, method: 'GET' }),
  post: (path, body, options) => request(path, { ...options, method: 'POST', body }),
  put: (path, body, options) => request(path, { ...options, method: 'PUT', body }),
  delete: (path, options) => request(path, { ...options, method: 'DELETE' }),
  patch: (path, body, options) => request(path, { ...options, method: 'PATCH', body }),
}
