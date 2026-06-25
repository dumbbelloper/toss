// 곳간 로고 마크. 창고 지붕(삼각) + 갈무리되는 자산(상승 막대).
// mobile/src/ui/Logo.tsx 와 동일한 path 데이터를 공유한다(통일 브랜딩).

export function LogoMark({ size = 24, color = '#b5703c' }: { size?: number; color?: string }) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" fill={color} aria-hidden="true">
      {/* 창고 지붕 */}
      <path d="M16 3 L29 12 L3 12 Z" />
      {/* 갈무리되는 자산(상승 막대) */}
      <rect x="8" y="18" width="4" height="8" rx="1" />
      <rect x="14" y="16" width="4" height="10" rx="1" />
      <rect x="20" y="14" width="4" height="12" rx="1" />
    </svg>
  )
}
