const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

export async function apiFetch(path, options = {}) {
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  })

  if (!res.ok) {
    const body = await res.json().catch(() => null)
    throw Object.assign(new Error(body?.message || res.statusText), { code: body?.code, status: res.status })
  }

  if (res.status === 204) return null
  return res.json()
}
