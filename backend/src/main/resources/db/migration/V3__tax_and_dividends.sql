-- V3: ETF 세금 분류 + 실분배금 (SIM01 고도화 토대). 규칙 단일출처: docs/product/korea-tax.md
-- 대상 범위: ETF 만(개별주식 제외), 국내상장 + 해외상장.

-- ETF 과세 3분류:
--   kr_equity = 국내상장 국내주식형 ETF (매매차익 비과세, 분배금 15.4%)
--   kr_other  = 국내상장 해외·채권·원자재 ETF (매매차익 15.4% 배당 = min(실차익, 과표기준가 상승분), 분배금 15.4%)
--   foreign   = 해외상장 ETF (매매차익 22% 양도세·250만 공제, 분배금 15.4%)
alter table symbol_universe add column tax_class varchar(16);
update symbol_universe set tax_class = 'foreign' where tax_class is null;  -- 기존 17종은 전부 미국 상장

-- 국내상장 ETF 추가 (Yahoo '.KS'). 국내/해외 ETF 세금 차이를 시뮬에서 비교하기 위함.
insert into symbol_universe (symbol, name, market, currency, category, tax_class) values
    ('069500.KS', 'KODEX 200',              'KR', 'KRW', 'index',   'kr_equity'),
    ('360750.KS', 'TIGER 미국S&P500',       'KR', 'KRW', 'index',   'kr_other'),
    ('133690.KS', 'TIGER 미국나스닥100',    'KR', 'KRW', 'index',   'kr_other'),
    ('458730.KS', 'TIGER 미국배당다우존스', 'KR', 'KRW', 'holding', 'kr_other');

-- 실분배금(배당) 이벤트. Yahoo events=div. 시뮬 세후 현금흐름의 단일 출처.
create table dividend_daily (
    symbol    varchar(32)    not null,
    ex_date   date           not null,
    amount    numeric(20, 6) not null,   -- 1주당 분배금(상장 통화 기준)
    currency  varchar(3)     not null,
    source    varchar(16)    not null,
    primary key (symbol, ex_date)
);
create index idx_dividend_daily_symbol on dividend_daily (symbol, ex_date);
