# 데이터 소스 (백테스팅 · 시뮬레이터)

> 백테스팅 페이지(기둥 2)와 은퇴 시뮬레이터(기둥 3)가 쓰는 데이터의 출처·라이선스·적재 방식.
> 원칙: **실존 데이터만, 라이선스 준수, 로컬 적재.**

## 1. 무엇이 필요한가

| 종류 | 용도 | 필수 속성 |
|------|------|-----------|
| 일별 시세(OHLCV) | 백테스트 가격 | **수정종가(adj_close)**, 배당, 액면분할 |
| 배당 이력 | 세후 현금흐름·총수익 | 지급일·금액·세율 |
| 매크로 | 시나리오 컨텍스트 | 금리·CPI·환율 |
| 팩터 수익률 | factor analysis | 시장/규모/가치/모멘텀, 무위험수익률 |
| 세제/제도 파라미터 | 은퇴 시뮬 | 배당소득세·건보료율·국민연금 산식 |

## 2. 후보 소스

### 🇰🇷 국내(KRX) 시세·배당
| 소스 | 특징 | 라이선스 | 용도 |
|------|------|----------|------|
| **공공데이터포털 `금융위원회_주식시세정보`** (data.go.kr) | 공식·무료·REST, 일별 시세 | 재배포 가능(공공데이터) | **운영 1순위** |
| KRX 정보데이터시스템 (data.krx.co.kr) | 공식 무료, 지수·OHLCV | 스크래핑성 | 보조 |
| pykrx | KRX 래핑 파이썬, 편함 | 비공식 | 프로토타입 |

### 🇺🇸 미국/글로벌 시세·배당
| 소스 | 특징 | 라이선스 | 용도 |
|------|------|----------|------|
| **Stooq** | 무료 EOD CSV, 키 불필요 | 관대 | **프로토타입 1순위** |
| yfinance (Yahoo) | 무료·광범위·수정종가/배당/분할 | **비공식·재배포 금지** | 프로토타입만 |
| **Tiingo** | 저렴·깔끔 EOD+펀더멘털 | 소규모 SaaS 양호 | **운영 가성비 1순위** |
| Polygon.io | 유료·고품질 US | 상용 | 운영 확장 |
| EOD Historical Data | 유료·글로벌(KRX 포함) | 재배포 우호 | 운영 확장 |

### 📈 매크로 · 팩터 (무료·재배포 OK)
| 소스 | 내용 |
|------|------|
| **FRED** (stlouisfed) | 미 금리·CPI·실업 등. 무료 API, 관대한 라이선스 |
| **한국은행 ECOS** | 기준금리·환율·CPI. 무료 API |
| **Ken French Data Library** | 팩터 수익률·무위험수익률. 학술 표준(portfoliovisualizer류가 사용) |

## 3. ⚠️ 시니어 주의사항 (틀리면 백테스트가 거짓말함)

1. **라이선스/재배포** — yfinance·네이버 스크래핑은 개인/프로토타입 OK, **상용 SaaS 금지**.
   운영은 공공데이터(data.go.kr)·FRED·ECOS·유료(Tiingo/Polygon/EODHD)로.
2. **수정종가 필수** — 배당·분할 미반영 종가로 백테스트하면 결과가 틀림. total-return 사용.
3. **생존편향** — 상장폐지 종목 누락 → 수익률 과대평가. 정밀 백테스트는 point-in-time 유니버스(비쌈).
4. **기업행위(분할·합병·티커변경)** 처리 안 하면 시계열이 끊김.
5. **로컬 적재** — 백테스트마다 API 호출 금지. 일별 EOD 배치로 DB에 적재 → 단일 소스 종속 탈피.

## 4. 아키텍처 (권장)

```
[provider 어댑터]  ──ingest(일별 EOD 배치)──▶  [Postgres: price_daily]  ◀──read──  [백테스트 엔진]
  ├ Stooq / yfinance (proto)
  ├ data.go.kr / Tiingo (prod)
  └ ...                                   provider 교체 = 어댑터 1곳 수정
```

### 스키마 초안

```sql
-- 시세 (수정종가 포함)
price_daily (
  symbol      text,
  market      text,           -- KRX / NASDAQ / ...
  date        date,
  open        numeric,
  high        numeric,
  low         numeric,
  close       numeric,
  adj_close   numeric,         -- 배당·분할 반영
  volume      bigint,
  PRIMARY KEY (symbol, date)
);

-- 기업행위
corporate_action (
  symbol  text,
  date    date,
  type    text,                -- dividend | split
  value   numeric,             -- 배당금 or 분할비율
  PRIMARY KEY (symbol, date, type)
);

-- 매크로 시계열
macro_series (
  series  text,                -- FRED/ECOS series id
  date    date,
  value   numeric,
  PRIMARY KEY (series, date)
);
```

> 백엔드가 이미 **Postgres + Spring Data JDBC** 라 적재 잡(@Scheduled)·어댑터를 그대로 얹으면 됨.

## 5. 구현 (Phase 1 — 2026-06-25)

**결정 (정확성 우선):**
- **주 소스 = Yahoo Finance** (chart API). 이유: *배당 재투자 정확 구현*에 필요한 **원시 close +
  조정 close(분할+배당) + 배당 이벤트**를 한 번에 제공. Stooq 는 조정 방식 모호 → 보조/교체용.
  (`YahooHistoryProvider`, 교체형 어댑터)
- **백테스트는 DB(`price_daily`)만 읽는다.** 라이브 호출 X. 데이터는 배치로 적재.
- 통화 토글(USD ↔ KRW 그 당시 환율)·배당재투자 토글(close ↔ adj_close)을 위해
  **원시 close + adj_close + `fx_daily`(USDKRW)** 를 함께 보관.

**실제 스키마**: `price_daily(symbol,market,date,open,high,low,close,adj_close,volume,source)`
+ `fx_daily(pair,date,rate,source)` + `symbol_universe`. (마이그레이션 `V2__price_data.sql`)

**적재 코드**: `com.toss.history.*` — `YahooHistoryProvider`(파서) · `PriceDailyDao`(upsert) ·
`BackfillService` · `HistoryController`(`POST /api/history/backfill`, `GET /api/history/coverage`).

**검증된 커버리지** (초기 유니버스 17 ETF, 상장일~현재):
SPY 8,407봉(1993~) · QQQ 6,865(1999~) · DIA 7,151(1998~) · VTI 6,292(2001~) · GLD 5,432(2004~) ·
JEPI 1,530(2020~) 등 — **총 78,199 가격행 + 5,853 환율행**. adj_close 가 과거로 갈수록 close 보다
낮음(배당 누적) = 총수익 데이터 정상.

## 6. 다음 (Phase 2~3)

- **Phase 2**: 백테스트 엔진을 DB 읽기로 전환(→ 다년 백테스트) + 자정 증분 잡(@Scheduled, Toss/Yahoo) + 갭필.
- **Phase 3**: UI 토글(배당 재투자 / USD·KRW 환율) web·mobile. 팩터(Ken French)·매크로(FRED) 테이블.
