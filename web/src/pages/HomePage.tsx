import { isUnauthorized, login, useMe } from '../lib/auth'

export function HomePage() {
  const me = useMe()

  if (me.isLoading) {
    return <p className="text-gray-400">불러오는 중…</p>
  }

  if (isUnauthorized(me.error)) {
    return (
      <section className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold">로그인이 필요합니다</h1>
          <p className="mt-2 text-gray-500">
            Keycloak 으로 로그인합니다. 인증은 백엔드(BFF)가 중개하며, 토큰은 브라우저에
            저장되지 않습니다.
          </p>
        </div>
        <button
          type="button"
          onClick={login}
          className="rounded-lg bg-accent px-5 py-3 font-semibold text-white hover:bg-accent-hover"
        >
          Keycloak 으로 로그인
        </button>
      </section>
    )
  }

  if (me.isError) {
    return <p className="text-red-500">오류: {String(me.error)}</p>
  }

  const user = me.data!
  return (
    <section className="space-y-6">
      <h1 className="text-2xl font-bold">
        안녕하세요, {user.name || user.username} 님
      </h1>
      <dl className="grid grid-cols-[7rem_1fr] gap-y-3 rounded-xl border border-gray-200 bg-white p-6 text-sm">
        <dt className="text-gray-400">username</dt>
        <dd className="font-medium">{user.username}</dd>
        <dt className="text-gray-400">email</dt>
        <dd className="font-medium">{user.email || '—'}</dd>
        <dt className="text-gray-400">roles</dt>
        <dd className="font-medium">{user.roles.length ? user.roles.join(', ') : '—'}</dd>
        <dt className="text-gray-400">subject</dt>
        <dd className="truncate font-mono text-xs text-gray-500">{user.subject}</dd>
      </dl>
    </section>
  )
}
