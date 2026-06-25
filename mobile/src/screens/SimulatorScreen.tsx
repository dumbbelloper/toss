// 시뮬레이터 화면 (SIM01 ETF 세후 월배당). web SimulatorPage 의 RN 미러.
import { useState } from 'react';
import { ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';

import { formatMoney } from '../api/dashboard';
import { useDividend, type DividendInput } from '../api/sim';
import { colors } from '../ui/theme';

const DEFAULTS: DividendInput = { principal: 100_000_000, yieldPercent: 8, otherFinancialIncome: 0 };

export function SimulatorScreen() {
  const [input, setInput] = useState<DividendInput>(DEFAULTS);
  const { data } = useDividend(input);

  const setNum = (k: keyof DividendInput) => (t: string) =>
    setInput(s => ({ ...s, [k]: Number(t.replace(/[^0-9.]/g, '')) || 0 }));

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <Text style={styles.h1}>ETF 세후 월배당</Text>
      <Text style={styles.sub}>배당소득세 15.4% 분리과세 기준. 연 금융소득 2,000만원 초과 시 종합과세.</Text>

      <View style={styles.form}>
        <Field label="투자 원금 (원)" value={input.principal} onChange={setNum('principal')} />
        <Field label="연 배당수익률 (%)" value={input.yieldPercent} onChange={setNum('yieldPercent')} />
        <Field label="기타 금융소득 (원/년)" value={input.otherFinancialIncome} onChange={setNum('otherFinancialIncome')} />
      </View>

      {data && (
        <View style={{ gap: 12, marginTop: 16 }}>
          <View style={styles.hero}>
            <Text style={styles.muted}>세후 월평균 배당</Text>
            <Text style={styles.heroValue}>{formatMoney(data.monthlyAfterTax, 'KRW')}</Text>
            <Text style={styles.muted}>
              세후 실효 {data.afterTaxYield.toFixed(2)}% · 연 {formatMoney(data.annualAfterTax, 'KRW')}
            </Text>
          </View>

          <View style={styles.cards}>
            <Card label="세전 연배당" value={formatMoney(data.annualGross, 'KRW')} />
            <Card label={`세금 (${(data.taxRate * 100).toFixed(1)}%)`} value={`-${formatMoney(data.taxAmount, 'KRW')}`} loss />
            <Card label="세후 연배당" value={formatMoney(data.annualAfterTax, 'KRW')} />
          </View>

          {data.comprehensiveTaxable && (
            <View style={styles.notice}>
              <Text style={styles.noticeTitle}>금융소득종합과세 대상</Text>
              <Text style={styles.noticeBody}>
                연 금융소득이 2,000만원을 초과합니다. 초과분은 누진세율(6~45%)이 적용될 수 있어
                실제 세금은 분리과세(15.4%) 기준보다 클 수 있습니다.
              </Text>
            </View>
          )}
        </View>
      )}
    </ScrollView>
  );
}

function Field({ label, value, onChange }: { label: string; value: number; onChange: (t: string) => void }) {
  return (
    <View>
      <Text style={styles.label}>{label}</Text>
      <TextInput style={styles.input} value={String(value)} onChangeText={onChange} keyboardType="number-pad" />
    </View>
  );
}

function Card({ label, value, loss }: { label: string; value: string; loss?: boolean }) {
  return (
    <View style={styles.card}>
      <Text style={styles.muted}>{label}</Text>
      <Text style={[styles.cardValue, loss ? { color: colors.loss } : null]}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  content: { padding: 20, paddingBottom: 48 },
  h1: { fontSize: 24, fontWeight: '700', color: colors.ink },
  sub: { fontSize: 13, color: colors.muted, marginTop: 4, marginBottom: 16 },
  form: { backgroundColor: colors.surface, borderRadius: 12, borderWidth: 1, borderColor: colors.line, padding: 16, gap: 4 },
  label: { fontSize: 12, color: colors.muted, marginTop: 8, marginBottom: 4 },
  input: { borderWidth: 1, borderColor: colors.line, borderRadius: 8, paddingHorizontal: 10, paddingVertical: 8, fontSize: 14, color: colors.ink, backgroundColor: '#fff' },
  hero: { backgroundColor: colors.surface, borderRadius: 12, borderWidth: 1, borderColor: colors.line, padding: 20 },
  heroValue: { fontSize: 32, fontWeight: '800', color: colors.accent, marginVertical: 2 },
  muted: { fontSize: 12, color: colors.muted },
  cards: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  card: { flexGrow: 1, flexBasis: '30%', backgroundColor: colors.surface, borderRadius: 12, borderWidth: 1, borderColor: colors.line, padding: 14 },
  cardValue: { fontSize: 15, fontWeight: '700', color: colors.ink, marginTop: 2 },
  notice: { backgroundColor: '#fffbeb', borderRadius: 12, padding: 16, borderWidth: 1, borderColor: '#fde68a' },
  noticeTitle: { color: '#92400e', fontWeight: '600' },
  noticeBody: { color: '#b45309', fontSize: 13, marginTop: 4 },
});
