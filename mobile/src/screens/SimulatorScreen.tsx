// 시뮬레이터 화면 (SIM01 배당). web SimulatorPage 의 RN 미러.
// 실분배금 기반 세후 현금흐름: 단일/비교, 일시금/월적립, DRIP, 기간, 건보료 경고.
import { useState } from 'react';
import {
  LayoutChangeEvent,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';

import { formatMoney } from '../api/dashboard';
import { useUniverse } from '../api/backtest';
import {
  TAX_CLASS_LABEL,
  useDividend,
  type Contribution,
  type DividendParams,
  type DividendResult,
} from '../api/sim';
import { LineChart, type ChartSeries } from '../ui/LineChart';
import { colors } from '../ui/theme';

const today = new Date().toISOString().slice(0, 10);

const DEFAULTS: DividendParams = {
  symbol: 'SPY',
  contribution: 'LUMP_SUM',
  amount: 10_000_000,
  start: '2020-01-01',
  end: today,
  reinvest: true,
};

type Mode = 'single' | 'compare';

export function SimulatorScreen() {
  const universe = useUniverse().data ?? [];
  const options = universe.length ? universe : [{ symbol: 'SPY', name: 'SPDR S&P 500' }];
  const [mode, setMode] = useState<Mode>('single');
  const [form, setForm] = useState<DividendParams>(DEFAULTS);
  const [symbolB, setSymbolB] = useState('360750.KS');
  const [submitted, setSubmitted] = useState<{ a: DividendParams; b: DividendParams | null } | null>(null);

  const a = useDividend(submitted?.a ?? null);
  const b = useDividend(submitted?.b ?? null);

  const set = <K extends keyof DividendParams>(k: K, v: DividendParams[K]) =>
    setForm(s => ({ ...s, [k]: v }));
  const run = () =>
    setSubmitted({ a: form, b: mode === 'compare' ? { ...form, symbol: symbolB } : null });
  const busy = a.isFetching || b.isFetching;

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <Text style={styles.h1}>배당 시뮬레이터</Text>
      <Text style={styles.sub}>
        내부 DB의 실제 분배금으로 세후 현금흐름을 계산합니다. 해외 ETF는 그 당시 환율 적용 · 참고용.
      </Text>

      <View style={styles.form}>
        <Chips
          value={mode}
          onChange={v => setMode(v as Mode)}
          options={[
            { value: 'single', label: '단일' },
            { value: 'compare', label: '국내 vs 해외' },
          ]}
        />

        <Label>{mode === 'compare' ? '종목 A' : '종목'}</Label>
        <SymbolChips options={options} value={form.symbol} onChange={v => set('symbol', v)} />
        {mode === 'compare' && (
          <>
            <Label>종목 B</Label>
            <SymbolChips options={options} value={symbolB} onChange={setSymbolB} />
          </>
        )}

        <Label>투자 방식</Label>
        <Chips
          value={form.contribution}
          onChange={v => set('contribution', v as Contribution)}
          options={[
            { value: 'LUMP_SUM', label: '일시금 거치' },
            { value: 'MONTHLY', label: '월 적립' },
          ]}
        />

        <Label>배당</Label>
        <Chips
          value={form.reinvest ? 'y' : 'n'}
          onChange={v => set('reinvest', v === 'y')}
          options={[
            { value: 'y', label: '세후 재투자' },
            { value: 'n', label: '현금 수령' },
          ]}
        />

        <Label>{form.contribution === 'LUMP_SUM' ? '투자금 (원)' : '월 납입액 (원)'}</Label>
        <TextInput
          style={styles.input}
          value={String(form.amount)}
          keyboardType="number-pad"
          onChangeText={t => set('amount', Number(t.replace(/[^0-9]/g, '')) || 0)}
        />

        <View style={styles.row}>
          <View style={{ flex: 1 }}>
            <Label>시작일</Label>
            <TextInput style={styles.input} value={form.start} autoCapitalize="none" onChangeText={v => set('start', v)} />
          </View>
          <View style={{ flex: 1 }}>
            <Label>종료일</Label>
            <TextInput style={styles.input} value={form.end} autoCapitalize="none" onChangeText={v => set('end', v)} />
          </View>
        </View>

        <Pressable onPress={run} disabled={busy} style={[styles.run, busy && { opacity: 0.5 }]}>
          <Text style={styles.runText}>{busy ? '계산 중…' : '계산'}</Text>
        </Pressable>
      </View>

      {(a.isError || b.isError) && (
        <Text style={styles.err}>계산 실패: {((a.error ?? b.error) as Error | undefined)?.message}</Text>
      )}

      {submitted && a.data && !submitted.b && <Single r={a.data} />}
      {submitted && submitted.b && a.data && b.data && <Compare a={a.data} b={b.data} />}
    </ScrollView>
  );
}

function Single({ r }: { r: DividendResult }) {
  const [w, setW] = useState(0);
  const series: ChartSeries[] = [
    { values: r.timeline.map(p => p.cumulativeNet), color: colors.accent, fill: true },
  ];
  return (
    <View style={{ gap: 12, marginTop: 16 }}>
      <View style={styles.hero}>
        <View style={styles.between}>
          <Text style={styles.muted}>세후 누적 분배금{r.reinvest ? ' (재투자됨)' : ''}</Text>
          <ClassBadge r={r} />
        </View>
        <Text style={styles.heroValue}>{formatMoney(Math.round(r.totalNetDividend), 'KRW')}</Text>
        <Text style={styles.muted}>
          연 {(r.yieldOnCost * 100).toFixed(2)}% (yield on cost) · 세전{' '}
          {formatMoney(Math.round(r.totalGrossDividend), 'KRW')}
        </Text>
      </View>

      <View style={styles.cards}>
        <Stat label="투자 원금" value={formatMoney(Math.round(r.invested), 'KRW')} />
        <Stat label="평가액(현재)" value={formatMoney(Math.round(r.finalValue), 'KRW')} />
        <Stat label="보유 주수" value={`${r.finalShares.toFixed(2)}주`} />
      </View>

      <Warnings r={r} />

      <View style={styles.chartCard} onLayout={(e: LayoutChangeEvent) => setW(e.nativeEvent.layout.width)}>
        <View style={styles.between}>
          <Text style={styles.h2}>누적 세후 분배금</Text>
          <Text style={styles.muted}>{r.timeline.length}회 분배</Text>
        </View>
        <LineChart
          series={series}
          width={w}
          xLabels={r.timeline.map(p => p.date)}
          formatY={v => Math.round(v / 10000).toLocaleString('ko-KR') + '만'}
        />
      </View>
    </View>
  );
}

function Compare({ a, b }: { a: DividendResult; b: DividendResult }) {
  const [w, setW] = useState(0);
  const longer = a.timeline.length >= b.timeline.length ? a : b;
  const diff = a.totalNetDividend - b.totalNetDividend;
  const series: ChartSeries[] = [
    { values: a.timeline.map(p => p.cumulativeNet), color: colors.accent, label: a.symbol },
    { values: b.timeline.map(p => p.cumulativeNet), color: colors.loss, label: b.symbol },
  ];
  return (
    <View style={{ gap: 12, marginTop: 16 }}>
      <View style={styles.cards}>
        <CompareCol r={a} accent />
        <CompareCol r={b} />
      </View>
      <View style={styles.diffCard}>
        <Text style={styles.diffText}>
          {a.symbol} 세후 분배금이 {b.symbol} 대비{' '}
          <Text style={{ color: diff >= 0 ? colors.gain : colors.loss, fontWeight: '700' }}>
            {diff >= 0 ? '+' : ''}
            {formatMoney(Math.round(diff), 'KRW')}
          </Text>
          . 동일 노출이라도 과세·환율로 세후가 갈립니다.
        </Text>
      </View>
      <Warnings r={a} prefix={`${a.symbol}: `} />
      <Warnings r={b} prefix={`${b.symbol}: `} />
      <View style={styles.chartCard} onLayout={(e: LayoutChangeEvent) => setW(e.nativeEvent.layout.width)}>
        <Text style={styles.h2}>누적 세후 분배금 (A vs B)</Text>
        <LineChart
          series={series}
          width={w}
          xLabels={longer.timeline.map(p => p.date)}
          formatY={v => Math.round(v / 10000).toLocaleString('ko-KR') + '만'}
        />
      </View>
    </View>
  );
}

function CompareCol({ r, accent }: { r: DividendResult; accent?: boolean }) {
  return (
    <View style={[styles.col, accent && { borderColor: colors.accent }]}>
      <View style={styles.between}>
        <Text style={styles.colSym}>{r.symbol}</Text>
        <ClassBadge r={r} />
      </View>
      <Text style={styles.colValue}>{formatMoney(Math.round(r.totalNetDividend), 'KRW')}</Text>
      <KV k="세전 분배금" v={formatMoney(Math.round(r.totalGrossDividend), 'KRW')} />
      <KV k="yield on cost" v={`${(r.yieldOnCost * 100).toFixed(2)}%`} />
      <KV k="평가액" v={formatMoney(Math.round(r.finalValue), 'KRW')} />
    </View>
  );
}

function KV({ k, v }: { k: string; v: string }) {
  return (
    <View style={[styles.between, { marginTop: 2 }]}>
      <Text style={styles.muted}>{k}</Text>
      <Text style={styles.kvVal}>{v}</Text>
    </View>
  );
}

function ClassBadge({ r }: { r: DividendResult }) {
  return (
    <View style={styles.badge}>
      <Text style={styles.badgeText}>
        {TAX_CLASS_LABEL[r.taxClass]} · {r.currency}
      </Text>
    </View>
  );
}

function Warnings({ r, prefix = '' }: { r: DividendResult; prefix?: string }) {
  if (!r.healthInsuranceRisk && !r.comprehensiveTaxRisk) {
    return null;
  }
  const annual = formatMoney(Math.round(r.maxAnnualGrossDividend), 'KRW');
  return (
    <View style={{ gap: 8 }}>
      {r.healthInsuranceRisk && (
        <View style={styles.notice}>
          <Text style={styles.noticeTitle}>{prefix}건강보험료 영향 가능</Text>
          <Text style={styles.noticeBody}>
            연 배당(최대 {annual})이 금융소득 1,000만원을 초과합니다. 전액 건보료 반영 + 피부양자 박탈 가능.
          </Text>
        </View>
      )}
      {r.comprehensiveTaxRisk && (
        <View style={[styles.notice, { backgroundColor: '#fef2f2', borderColor: '#fecaca' }]}>
          <Text style={[styles.noticeTitle, { color: '#9f1239' }]}>{prefix}금융소득종합과세 대상</Text>
          <Text style={[styles.noticeBody, { color: '#be123c' }]}>
            연 금융소득 2,000만원 초과. 누진세율(6~45%) 합산으로 실제 세금이 더 클 수 있습니다.
          </Text>
        </View>
      )}
    </View>
  );
}

function Label({ children }: { children: string }) {
  return <Text style={styles.label}>{children}</Text>;
}
function Stat({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.card}>
      <Text style={styles.muted}>{label}</Text>
      <Text style={styles.cardValue}>{value}</Text>
    </View>
  );
}
function Chips({
  value,
  onChange,
  options,
}: {
  value: string;
  onChange: (v: string) => void;
  options: { value: string; label: string }[];
}) {
  return (
    <View style={styles.chips}>
      {options.map(o => (
        <Pressable key={o.value} onPress={() => onChange(o.value)} style={[styles.chip, value === o.value && styles.chipOn]}>
          <Text style={[styles.chipText, value === o.value && styles.chipTextOn]}>{o.label}</Text>
        </Pressable>
      ))}
    </View>
  );
}
function SymbolChips({
  options,
  value,
  onChange,
}: {
  options: { symbol: string; name: string }[];
  value: string;
  onChange: (v: string) => void;
}) {
  return (
    <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.chips}>
      {options.map(o => (
        <Pressable key={o.symbol} onPress={() => onChange(o.symbol)} style={[styles.chip, value === o.symbol && styles.chipOn]}>
          <Text style={[styles.chipText, value === o.symbol && styles.chipTextOn]}>{o.symbol}</Text>
        </Pressable>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  content: { padding: 20, paddingBottom: 48 },
  h1: { fontSize: 24, fontWeight: '700', color: colors.ink },
  h2: { fontSize: 16, fontWeight: '600', color: colors.ink },
  sub: { fontSize: 13, color: colors.muted, marginTop: 4, marginBottom: 16 },
  form: { backgroundColor: colors.surface, borderRadius: 12, borderWidth: 1, borderColor: colors.line, padding: 16, gap: 4 },
  label: { fontSize: 12, color: colors.muted, marginTop: 10, marginBottom: 4 },
  input: { borderWidth: 1, borderColor: colors.line, borderRadius: 8, paddingHorizontal: 10, paddingVertical: 8, fontSize: 14, color: colors.ink, backgroundColor: '#fff' },
  row: { flexDirection: 'row', gap: 10 },
  chips: { flexDirection: 'row', gap: 8, flexWrap: 'wrap' },
  chip: { paddingHorizontal: 12, paddingVertical: 7, borderRadius: 16, backgroundColor: '#eee7dd' },
  chipOn: { backgroundColor: colors.accent },
  chipText: { fontSize: 13, color: colors.muted },
  chipTextOn: { color: '#fff', fontWeight: '600' },
  run: { backgroundColor: colors.accent, borderRadius: 10, paddingVertical: 13, alignItems: 'center', marginTop: 16 },
  runText: { color: '#fff', fontSize: 15, fontWeight: '700' },
  err: { color: colors.loss, marginTop: 14 },
  hero: { backgroundColor: colors.surface, borderRadius: 12, borderWidth: 1, borderColor: colors.line, padding: 20 },
  heroValue: { fontSize: 30, fontWeight: '800', color: colors.accent, marginVertical: 4 },
  muted: { fontSize: 12, color: colors.muted },
  between: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  cards: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  card: { flexGrow: 1, flexBasis: '30%', backgroundColor: colors.surface, borderRadius: 12, borderWidth: 1, borderColor: colors.line, padding: 14 },
  cardValue: { fontSize: 15, fontWeight: '700', color: colors.ink, marginTop: 2 },
  col: { flexGrow: 1, flexBasis: '46%', backgroundColor: colors.surface, borderRadius: 12, borderWidth: 1, borderColor: colors.line, padding: 14 },
  colSym: { fontSize: 15, fontWeight: '700', color: colors.ink },
  colValue: { fontSize: 20, fontWeight: '800', color: colors.accent, marginVertical: 6 },
  kvVal: { fontSize: 13, fontWeight: '500', color: colors.ink },
  diffCard: { backgroundColor: colors.surface, borderRadius: 12, borderWidth: 1, borderColor: colors.line, padding: 14 },
  diffText: { fontSize: 13, color: colors.ink, lineHeight: 19 },
  badge: { backgroundColor: '#eee7dd', borderRadius: 10, paddingHorizontal: 8, paddingVertical: 3 },
  badgeText: { fontSize: 10, color: colors.muted },
  chartCard: { backgroundColor: colors.surface, borderRadius: 12, borderWidth: 1, borderColor: colors.line, padding: 14, gap: 6 },
  notice: { backgroundColor: '#fffbeb', borderRadius: 12, padding: 14, borderWidth: 1, borderColor: '#fde68a' },
  noticeTitle: { color: '#92400e', fontWeight: '600', fontSize: 13 },
  noticeBody: { color: '#b45309', fontSize: 12, marginTop: 4, lineHeight: 17 },
});
