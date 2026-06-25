-- 토스증권 트레이딩 앱 기반 스키마 (V1).
-- 모델은 Toss OpenAPI 의 Order / Price 응답 구조를 따른다.
-- 금액/수량은 부동소수 오차 방지를 위해 numeric 사용.

-- 시세 스냅샷 (폴링 모니터링 이력)
create table price_snapshot (
    id          bigserial primary key,
    symbol      varchar(32)     not null,
    last_price  numeric(24, 8)  not null,
    currency    varchar(3)      not null,
    ts          timestamptz,                       -- 데이터 시각 (체결 미발생 시 null 가능)
    fetched_at  timestamptz     not null default now()
);
create index idx_price_snapshot_symbol_time on price_snapshot (symbol, fetched_at desc);

-- 주문 기록 (생성/정정/취소 및 체결 상태 추적)
create table order_log (
    id              bigserial primary key,
    order_id        varchar(64)     not null,
    client_order_id varchar(36),                   -- 멱등성 키 (선택)
    account_seq     bigint          not null,
    symbol          varchar(32)     not null,
    side            varchar(8)      not null,      -- BUY / SELL
    order_type      varchar(16)     not null,      -- LIMIT / MARKET
    time_in_force   varchar(8),                    -- DAY / CLS / OPG
    status          varchar(32)     not null,      -- PENDING / FILLED / CANCELED ...
    price           numeric(24, 8),                -- MARKET 주문 시 null
    quantity        numeric(24, 8)  not null,
    order_amount    numeric(24, 8),                -- 금액 기반(US MARKET) 주문에만
    currency        varchar(3)      not null,
    ordered_at      timestamptz,
    canceled_at     timestamptz,
    raw             jsonb,                          -- 원본 응답 보관 (디버깅/감사)
    created_at      timestamptz     not null default now(),
    updated_at      timestamptz     not null default now()
);
create unique index uq_order_log_order_id on order_log (order_id);
create index idx_order_log_symbol on order_log (symbol);
create index idx_order_log_status on order_log (status);
