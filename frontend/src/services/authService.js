import { api } from './api.js'

const AUTH = '/auth'

export const authService = {
    login: (email, password) =>
        api.post(`${AUTH}/login`, { email, password }, { silenciarNoAutorizado: true }),

    me: () => api.get(`${AUTH}/me`, { silenciarNoAutorizado: true }),

    logout: () => api.post(`${AUTH}/logout`),
}
