const BASE_URL = '/api'

async function extraerMensajeDeError(response) {
  try {
    const data = await response.json()
    if (data?.message) return data.message
    if (data?.error) return data.error
  } catch {
    // La respuesta no trae JSON (por ejemplo, un 500 sin cuerpo).
  }
  return `Error ${response.status}${response.statusText ? ` (${response.statusText})` : ''}`
}

async function request(path, { body, headers, ...options } = {}) {
  const response = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: {
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      ...headers,
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  if (!response.ok) {
    throw new Error(await extraerMensajeDeError(response))
  }

  if (response.status === 204) return null

  const texto = await response.text()
  return texto ? JSON.parse(texto) : null
}

export const api = {
  get: (path) => request(path, { method: 'GET' }),
  post: (path, body) => request(path, { method: 'POST', body }),
  put: (path, body) => request(path, { method: 'PUT', body }),
  delete: (path) => request(path, { method: 'DELETE' }),
}
