// 백테스트 화면. web BacktestPage 의 RN 미러.
import { useState } from 'react';
import {
  ActivityIndicator,
  LayoutChangeEvent,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';

import { STRATEGIES, useBacktest, type BacktestParams, type Strategy } from '../api/backtest';
import { formatMoney, formatPercent, signColor } from '../api/dashboard';
import { LineChart, type ChartSeries } from '../ui/LineChart';
import { colors } from '../ui/theme';

const DEFAULTS: BacktestParams = {
  symbol: '005930',
  strategy: 'SMA_CROSS',
  shortWindow: 5,
  longWindow: 20,
  rsiPeriod: 14,
  rsiBuyBelow: 30,
  rsiSellAbove: 70,
  count: 200,
  capital: 1_000_000,
};

export function BacktestScreen() {
  const [form, setForm] = useState<BacktestParams>(DEFAULTS);
  const [submitted, setSubmitted] = useState<BacktestParams | null>(null);
  const { data, isFetching, isError, error } = useBacktest(submitted);

  const setNum = (k: keyof BacktestParams) => (t: string) =>
    setForm(f => ({ ...f, [k]: Number(t.replace(/[^0-9]/g, '')) || 0 }));

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <Text style={styles.h1}>백테스팅</Text>
      <Text style={styles.sub}>토스 일봉으로 전략을 과거에 적용해 검증합니다. 예측이 아니라 회고.</Text>

      <View style={styles.form}>
        <Label>종목 코드</Label>
        <TextInput
          style={styles.input}
          value={form.symbol}
          onChangeText={t => setForm(f => ({ ...f, symbol: t.trim() }))}
          autoCapitalize="none"
          placeholder="005930"
        />

        <Label>전략</Label>
        <View style={styles.chips}>
          {STRATEGIES.map(s => (
            <Pressable
              key={s.value}
              onPress={() => setForm(f => ({ ...f, strategy: s.value as Strategy }))}
              style={[styles.chip, form.strategy === s.value && styles.chipOn]}>
              <Text style={[styles.chipText, form.strategy === s.value && styles.chipTextOn]}>
                {s.label}
              </Text>
            </Pressable>
          ))}
        </View>

        {form.strategy === 'SMA_CROSS' && (
          <Row>
            <NumField label="단기" value={form.shortWindow} onChange={setNum('shortWindow')} />
            <NumField label="장기" value={form.longWindow} onChange={setNum('longWindow')} />
          </Row>
        )}
        {form.strategy === 'RSI' && (
          <Row>
            <NumField label="기간" value={form.rsiPeriod} onChange={setNum('rsiPeriod')} />
            <NumField label="매수<" value={form.rsiBuyBelow} onChange={setNum('rsiBuyBelow')} />
            <NumField label="매도>" value={form.rsiSellAbove} onChange={setNum('rsiSellAbove')} />
          </Row>
        )}

        <Row>
          <NumField label="기간(봉)" value={form.count} onChange={setNum('count')} />
          <NumField label="초기자본" value={form.capital} onChange={setNum('capital')} />
        </Row>

        <Pressable
          onPress={() => setSubmitted({ ...form })}
          disabled={isFetching}
          style={[styles.run, isFetching && { opacity: 0.5 }]}>
          <Text style={styles.runText}>{isFetching ? '백테스트 중…' : '백테스트 실행'}</Text>
        </Pressable>
      </View>

      {isError && <Text style={styles.err}>실행 실패: {(error as Error).message}</Text>}
      {data && <Results data={data} />}
    </ScrollView>
  );
}

function Results({ data }: { data: import('../api/backtest').BacktestResult }) {
  const [w, setW] = useState(0);
  const series: ChartSeries[] = [{ values: data.equity.map(e => e.value), color: colors.accent, fill: true }];
  const excess = data.totalReturn - data.buyHoldReturn;
  const onLayout = (e: LayoutChangeEvent) => setW(e.nativeEvent.layout.width);

  return (
    <View style={{ marginTop: 20, gap: 12 }}>
      <View style={styles.cards}>
        <Metric label="전략 수익률" value={formatPercent(data.totalReturn)} color={signColor(data.totalReturn)} />
        <Metric label="매수후보유 대비" value={formatPercent(excess)} color={signColor(excess)} sub={`B&H ${formatPercent(data.buyHoldReturn)}`} />
        <Metric label="최대 낙폭(MDD)" value={`-${(data.maxDrawdown * 100).toFixed(2)}%`} color={colors.loss} />
        <Metric label="매매·승률" value={`${data.trades}회`} sub={`승률 ${(data.winRate * 100).toFixed(0)}%`} />
      </View>

      <View style={styles.chartCard} onLayout={onLayout}>
        <View style={styles.chartHead}>
          <Text style={styles.h2}>자본 곡선</Text>
          <Text style={styles.muted}>
            {formatMoney(data.initialCapital, 'KRW')} → {formatMoney(Math.round(data.finalEquity), 'KRW')}
          </Text>
        </View>
        <LineChart
          series={series}
          width={w}
          baseline={data.initialCapital}
          formatY={v => Math.round(v / 10000).toLocaleString('ko-KR') + '만'}
        />
        <Text style={styles.cap}>{data.symbol} · {data.params} · {data.bars}봉</Text>
      </View>
    </View>
  );
}

function Label({ children }: { children: string }) {
  return <Text style={styles.label}>{children}</Text>;
}
function Row({ children }: { children: React.ReactNode }) {
  return <View style={styles.row}>{children}</View>;
}
function NumField({ label, value, onChange }: { label: string; value: number; onChange: (t: string) => void }) {
  return (
    <View style={{ flex: 1 }}>
      <Label>{label}</Label>
      <TextInput style={styles.input} value={String(value)} onChangeText={onChange} keyboardType="number-pad" />
    </View>
  );
}
function Metric({ label, value, sub, color }: { label: string; value: string; sub?: string; color?: string }) {
  return (
    <View style={styles.metric}>
      <Text style={styles.metricLabel}>{label}</Text>
      <Text style={[styles.metricValue, color ? { color } : null]}>{value}</Text>
      {sub ? <Text style={styles.muted}>{sub}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  content: { padding: 20, paddingBottom: 48 },
  h1: { fontSize: 24, fontWeight: '700', color: colors.ink },
  h2: { fontSize: 16, fontWeight: '600', color: colors.ink },
  sub: { fontSize: 13, color: colors.muted, marginTop: 4, marginBottom: 16 },
  form: { backgroundColor: colors.surface, borderRadius: 12, borderWidth: 1, borderColor: colors.line, padding: 16, gap: 6 },
  label: { fontSize: 12, color: colors.muted, marginTop: 8, marginBottom: 4 },
  input: { borderWidth: 1, borderColor: colors.line, borderRadius: 8, paddingHorizontal: 10, paddingVertical: 8, fontSize: 14, color: colors.ink, backgroundColor: '#fff' },
  chips: { flexDirection: 'row', gap: 8, flexWrap: 'wrap' },
  chip: { paddingHorizontal: 12, paddingVertical: 7, borderRadius: 16, backgroundColor: '#eee7dd' },
  chipOn: { backgroundColor: colors.accent },
  chipText: { fontSize: 13, color: colors.muted },
  chipTextOn: { color: '#fff', fontWeight: '600' },
  row: { flexDirection: 'row', gap: 10 },
  run: { backgroundColor: colors.accent, borderRadius: 10, paddingVertical: 13, alignItems: 'center', marginTop: 14 },
  runText: { color: '#fff', fontSize: 15, fontWeight: '700' },
  err: { color: colors.loss, marginTop: 14 },
  cards: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  metric: { flexGrow: 1, flexBasis: '46%', backgroundColor: colors.surface, borderRadius: 12, borderWidth: 1, borderColor: colors.line, padding: 14 },
  metricLabel: { fontSize: 12, color: colors.muted },
  metricValue: { fontSize: 18, fontWeight: '800', color: colors.ink, marginTop: 2 },
  muted: { fontSize: 12, color: colors.muted },
  chartCard: { backgroundColor: colors.surface, borderRadius: 12, borderWidth: 1, borderColor: colors.line, padding: 14 },
  chartHead: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 6 },
  cap: { fontSize: 12, color: colors.muted, marginTop: 4 },
});
