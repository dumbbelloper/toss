# 디자인 토큰 (레퍼런스)

> 코드의 토큰 정의와 **미러**되는 문서. 값의 의미·사용처를 설명한다.
> 실제 값의 단일 출처는 코드(공유 토큰 패키지). 변경 시 양쪽을 함께 갱신.

## 카테고리 (채울 예정)

| 카테고리 | 예시 토큰 | 비고 |
|----------|-----------|------|
| color | `color.bg`, `color.text`, `color.profit`, `color.loss` | 수익/손실 색은 한국 관습 확인 후 확정 |
| typography | `font.size.*`, `font.weight.*`, `font.numeric` | 금액·% 는 tabular-nums |
| spacing | `space.1..N` | 4px or 8px 베이스? |
| radius | `radius.sm/md/lg` | |
| shadow/elevation | `shadow.*` | web box-shadow ↔ RN elevation 매핑 |
| motion | `motion.duration.*`, `motion.easing.*` | |

## TODO
- [ ] 베이스 단위(4 vs 8) 결정
- [ ] 라이트/다크 색 정의
- [ ] 토큰 → web(CSS var/Tailwind) · mobile(RN StyleSheet) 변환 방식
