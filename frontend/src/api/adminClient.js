import { apiFetch } from './client'

const TOKEN_KEY = 'kokbaejeong_admin_token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export function isLoggedIn() {
  return !!getToken()
}

export function adminFetch(path, options = {}) {
  return apiFetch(path, {
    ...options,
    headers: { Authorization: `Bearer ${getToken()}`, ...options.headers },
  })
}
