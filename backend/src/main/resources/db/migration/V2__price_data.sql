-- 백테스트용 일별 시세 데이터 레이어 (V2).
-- 원칙: 백테스트는 이 DB만 읽는다(라이브 호출 X). 데이터는 배치로 적재/갱신한다.

-- 일별 시세. 원시 close(가격수익용) + adj_close(분할+배당 조정 = 총수익/배당재투자) 동시 보관.
-- 통화는 원본(예: US ETF = USD). 원화 환산은 fx_daily 를 곱해 조회 시 계산.
create table price_daily (
    symbol      varchar(32)     not null,
    market      varchar(16)     not null default 'US',
    date        date            not null,
    open        numeric(20, 6),
    high        numeric(20, 6),
    low         numeric(20, 6),
    close       numeric(20, 6)  not null,   -- 분할조정 원시 종가 (가격수익)
    adj_close   numeric(20, 6)  not null,   -- 분할+배당 조정 종가 (총수익 = 배당재투자)
    volume      bigint,
    source      varchar(16)     not null,   -- 'yahoo' | 'toss' | 'stooq'
    updated_at  timestamptz     not null default now(),
    primary key (symbol, date)
);
create index idx_price_daily_symbol_date on price_daily (symbol, date);

-- 일별 환율 (통화 토글: USD 기준 vs 그 당시 환율 적용 KRW 기준). pair 예: 'USDKRW'.
create table fx_daily (
    pair    varchar(12)     not null,
    date    date            not null,
    rate    numeric(20, 6)  not null,
    source  varchar(16)     not null,
    primary key (pair, date)
);
create index idx_fx_daily_pair_date on fx_daily (pair, date);

-- 적재 대상 유니버스. 백필/증분 잡이 여기 enabled=true 인 심볼을 채운다.
create table symbol_universe (
    symbol    varchar(32) primary key,
    name      varchar(128),
    market    varchar(16) not null default 'US',
    currency  varchar(3)  not null default 'USD',
    category  varchar(32),                      -- index/bond/commodity/sector/reit/intl/holding
    enabled   boolean     not null default true,
    added_at  timestamptz not null default now()
);

-- 초기 유니버스: 미국 핵심 ETF + 사용자 보유 커버드콜.
insert into symbol_universe (symbol, name, category) values
    ('SPY',  'SPDR S&P 500',                 'index'),
    ('QQQ',  'Invesco QQQ (Nasdaq 100)',     'index'),
    ('VTI',  'Vanguard Total US Market',     'index'),
    ('DIA',  'SPDR Dow Jones',               'index'),
    ('IWM',  'iShares Russell 2000',         'index'),
    ('AGG',  'iShares US Aggregate Bond',    'bond'),
    ('BND',  'Vanguard Total Bond',          'bond'),
    ('GLD',  'SPDR Gold',                     'commodity'),
    ('SLV',  'iShares Silver',                'commodity'),
    ('VNQ',  'Vanguard Real Estate',          'reit'),
    ('VEA',  'Vanguard Developed Markets',    'intl'),
    ('VWO',  'Vanguard Emerging Markets',     'intl'),
    ('JEPI', 'JPMorgan Equity Premium Income','holding'),
    ('JEPQ', 'JPMorgan Nasdaq Equity Premium','holding'),
    ('DIVO', 'Amplify CWP Enhanced Dividend', 'holding'),
    ('GPIQ', 'Goldman Sachs Nasdaq Premium',  'holding'),
    ('GPIX', 'Goldman Sachs S&P Premium',     'holding');
