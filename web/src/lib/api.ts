// 백엔드(BFF) 호출 래퍼. 동일 출처(Vite proxy)이므로 쿠키를 동봉하고,
// 변경 요청(POST 등)에는 XSRF-TOKEN 쿠키를 X-XSRF-TOKEN 헤더로 실어 CSRF 를 통과한다.

export function readCookie(name: string): string | undefined {
  const hit = document.cookie.split('; ').find((c) => c.startsWith(name + '='))
  return hit ? decodeURIComponent(hit.slice(name.length + 1)) : undefined
}

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

const SAFE = new Set(['GET', 'HEAD', 'OPTIONS'])

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? 'GET').toUpperCase()
  const headers = new Headers(init.headers)
  if (!SAFE.has(method)) {
    const xsrf = readCookie('XSRF-TOKEN')
    if (xsrf) headers.set('X-XSRF-TOKEN', xsrf)
  }

  const res = await fetch(path, { ...init, method, headers, credentials: 'include' })
  if (!res.ok) {
    throw new ApiError(res.status, `${method} ${path} → ${res.status}`)
  }
  const text = await res.text()
  return (text ? JSON.parse(text) : undefined) as T
}
