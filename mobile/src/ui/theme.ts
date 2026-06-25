// 곳간 디자인 토큰 — 방향 A(따뜻함). 단일 출처: docs/design/DESIGN.md (web index.css 와 미러).
// 폰트(Pretendard)는 네이티브 번들이 필요해 후속 작업.

export const colors = {
  bg: '#FAF7F1', // 페이지 배경 (웜 크림)
  surface: '#FFFFFF', // 카드
  ink: '#2A2521', // 본문
  muted: '#8A8175', // 보조·라벨
  line: '#EAE4DA', // 경계선
  accent: '#B5703C', // 브랜드 (clay)
  accentHover: '#9E6033',
  gain: '#E5494D', // 수익(빨강)
  loss: '#2F6FED', // 손실(파랑)
} as const;
