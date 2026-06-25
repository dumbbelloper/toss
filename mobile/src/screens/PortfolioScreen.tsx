// 자산관리 화면. web 의 PortfolioPage 를 RN 으로 미러링.
// 요약 카드 + 시세 차트(react-native-svg) + 보유 종목 목록.

import { useState } from 'react';
import {
  ActivityIndicator,
  LayoutChangeEvent,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import { useCandles } from '../api/charts';
import {
  formatMoney,
  formatPercent,
  formatPrice,
  signColor,
  usePortfolio,
  type HoldingsItem,
} from '../api/dashboard';
import { LineChart, type ChartSeries } from '../ui/LineChart';
import { colors } from '../ui/theme';

export function PortfolioScreen() {
  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <Text style={styles.h1}>자산 관리</Text>
      <Summary />
      <PriceSection />
      <Holdings />
    </ScrollView>
  );
}

function Summary() {
  const { data, isLoading, isError } = usePortfolio();

  if (isLoading) {
    return <ActivityIndicator style={styles.gap} />;
  }
  if (isError || !data) {
    return (
      <View style={styles.notice}>
        <Text style={styles.noticeTitle}>포트폴리오를 불러오지 못했습니다.</Text>
        <Text style={styles.noticeBody}>
          토스 계좌(toss.account-seq) 설정 시 보유·주문이 조회됩니다. 시세는 계좌 없이도 동작합니다.
        </Text>
      </View>
    );
  }

  const pl = data.profitLoss;
  const daily = data.dailyProfitLoss;
  return (
    <View style={styles.cardRow}>
      <Card label="평가금액" value={formatPrice(data.marketValue.amount)} />
      <Card label="총 손익" value={formatPrice(pl.amount)} sub={formatPercent(pl.rate)} color={signColor(pl.rate)} />
      <Card label="일간 손익" value={formatPrice(daily.amount)} sub={formatPercent(daily.rate)} color={signColor(daily.rate)} />
    </View>
  );
}

function Card({ label, value, sub, color }: { label: string; value: string; sub?: string; color?: string }) {
  return (
    <View style={styles.card}>
      <Text style={styles.cardLabel}>{label}</Text>
      <Text style={[styles.cardValue, color ? { color } : null]}>{value}</Text>
      {sub ? <Text style={[styles.cardSub, color ? { color } : null]}>{sub}</Text> : null}
    </View>
  );
}

/** 시세 차트. 보유 종목 선택(없으면 샘플 005930). 계좌 없이도 동작. */
function PriceSection() {
  const portfolio = usePortfolio();
  const holdings = portfolio.data?.items ?? [];
  const symbols = holdings.length
    ? holdings.map(h => ({ symbol: h.symbol, name: h.name || h.symbol }))
    : [{ symbol: '005930', name: '삼성전자' }];

  const [symbol, setSymbol] = useState(symbols[0].symbol);
  const [width, setWidth] = useState(0);
  const active = symbols.find(s => s.symbol === symbol) ?? symbols[0];
  const { data, isLoading, isError } = useCandles(active.symbol);

  const closes = data?.candles.map(c => c.closePrice) ?? [];
  const series: ChartSeries[] = [{ values: closes, color: colors.accent, fill: true }];

  const onLayout = (e: LayoutChangeEvent) => setWidth(e.nativeEvent.layout.width);

  return (
    <View style={styles.section}>
      <Text style={styles.h2}>시세 · {active.name}</Text>
      {symbols.length > 1 && (
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.chips}>
          {symbols.map(s => (
            <Pressable
              key={s.symbol}
              onPress={() => setSymbol(s.symbol)}
              style={[styles.chip, s.symbol === symbol && styles.chipActive]}>
              <Text style={[styles.chipText, s.symbol === symbol && styles.chipTextActive]}>{s.name}</Text>
            </Pressable>
          ))}
        </ScrollView>
      )}
      <View style={styles.chartCard} onLayout={onLayout}>
        {isLoading ? (
          <ActivityIndicator style={{ height: 220 }} />
        ) : isError || closes.length < 2 ? (
          <Text style={styles.empty}>시세를 불러올 수 없습니다.</Text>
        ) : (
          <LineChart series={series} width={width} formatY={v => Math.round(v).toLocaleString('ko-KR')} />
        )}
      </View>
    </View>
  );
}

function Holdings() {
  const { data, isLoading, isError } = usePortfolio();
  if (isLoading || isError || !data) {
    return null;
  }
  if (!data.items.length) {
    return <Text style={styles.empty}>보유 종목이 없습니다.</Text>;
  }
  return (
    <View style={styles.section}>
      <Text style={styles.h2}>보유 종목</Text>
      <View style={styles.list}>
        {data.items.map(item => (
          <HoldingRow key={item.symbol} item={item} />
        ))}
      </View>
    </View>
  );
}

function HoldingRow({ item }: { item: HoldingsItem }) {
  const c = item.currency;
  const color = signColor(item.profitLoss.rate);
  return (
    <View style={styles.row}>
      <View style={{ flexShrink: 1 }}>
        <Text style={styles.rowName}>{item.name || item.symbol}</Text>
        <Text style={styles.rowSymbol}>
          {item.symbol} · {item.quantity.toLocaleString('ko-KR')}주
        </Text>
      </View>
      <View style={{ alignItems: 'flex-end' }}>
        <Text style={styles.rowValue}>{formatMoney(item.marketValue.amount, c)}</Text>
        <Text style={[styles.rowPl, { color }]}>
          {formatMoney(item.profitLoss.amount, c)} ({formatPercent(item.profitLoss.rate)})
        </Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  content: { padding: 20, paddingBottom: 48 },
  h1: { fontSize: 24, fontWeight: '700', marginBottom: 16 },
  h2: { fontSize: 17, fontWeight: '600', marginBottom: 10 },
  gap: { marginTop: 20 },
  section: { marginTop: 24 },
  cardRow: { gap: 10 },
  card: { backgroundColor: colors.surface, borderRadius: 12, padding: 16, borderWidth: 1, borderColor: '#eee' },
  cardLabel: { color: '#9ca3af', fontSize: 12 },
  cardValue: { fontSize: 18, fontWeight: '700', marginTop: 4 },
  cardSub: { fontSize: 13, fontWeight: '500', marginTop: 2 },
  chartCard: {
    backgroundColor: colors.surface,
    borderRadius: 12,
    padding: 12,
    borderWidth: 1,
    borderColor: '#eee',
    minHeight: 244,
    justifyContent: 'center',
  },
  chips: { gap: 8, paddingBottom: 10 },
  chip: { paddingHorizontal: 12, paddingVertical: 6, borderRadius: 16, backgroundColor: '#eef0f3' },
  chipActive: { backgroundColor: colors.accent },
  chipText: { fontSize: 13, color: '#4b5563' },
  chipTextActive: { color: '#fff', fontWeight: '600' },
  empty: { color: '#9ca3af', textAlign: 'center', paddingVertical: 32 },
  list: { backgroundColor: colors.surface, borderRadius: 12, borderWidth: 1, borderColor: '#eee' },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 14,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#eee',
  },
  rowName: { fontSize: 15, fontWeight: '600' },
  rowSymbol: { fontSize: 12, color: '#9ca3af', marginTop: 2 },
  rowValue: { fontSize: 15, fontWeight: '500' },
  rowPl: { fontSize: 12, marginTop: 2 },
  notice: { backgroundColor: '#fffbeb', borderRadius: 12, padding: 16, borderWidth: 1, borderColor: '#fde68a', marginTop: 8 },
  noticeTitle: { color: '#92400e', fontWeight: '600' },
  noticeBody: { color: '#b45309', fontSize: 13, marginTop: 4 },
});
