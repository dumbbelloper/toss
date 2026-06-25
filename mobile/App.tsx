/**
 * 곳간 Mobile.
 *
 * 웹(BFF 쿠키 세션)과 달리 모바일은 react-native-app-auth 로 PKCE 토큰을 받아 Bearer 로
 * 백엔드 /api/me 를 호출한다. 미인증=랜딩(소개+로그인), 인증=대시보드+탭(자산/백테스트/시뮬).
 */

import { useState } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  View,
  useColorScheme,
} from 'react-native';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { SafeAreaProvider, useSafeAreaInsets } from 'react-native-safe-area-context';

import { isUnauthorized, useLogin, useLogout, useMe } from './src/auth/auth';
import { BacktestScreen } from './src/screens/BacktestScreen';
import { PortfolioScreen } from './src/screens/PortfolioScreen';
import { SimulatorScreen } from './src/screens/SimulatorScreen';
import { LogoMark } from './src/ui/Logo';
import { colors } from './src/ui/theme';

const queryClient = new QueryClient();

type Tab = 'home' | 'portfolio' | 'backtest' | 'sim';

const FEATURES: { emoji: string; title: string; desc: string }[] = [
  { emoji: '📊', title: '자산 관리', desc: '토스증권 보유·평가·시세를 한 화면에. 실시간 손익과 차트.' },
  { emoji: '⏳', title: '백테스팅', desc: '미국·한국 ETF 수십 년 데이터로 전략 검증. 배당 재투자·환율까지.' },
  { emoji: '💰', title: '배당 시뮬레이터', desc: '실제 분배금 세후 현금흐름. 국내 vs 해외 세제 비교, 건보료 경고.' },
];

const LINKS: { tab: Tab; emoji: string; title: string; desc: string }[] = [
  { tab: 'portfolio', emoji: '📊', title: '자산 관리', desc: '보유·평가·시세' },
  { tab: 'backtest', emoji: '⏳', title: '백테스팅', desc: '전략 과거 검증' },
  { tab: 'sim', emoji: '💰', title: '배당 시뮬레이터', desc: '세후 현금흐름' },
];

function Root() {
  const me = useMe();
  const insets = useSafeAreaInsets();
  const [tab, setTab] = useState<Tab>('home');
  const loggedIn = !!me.data;

  return (
    <View style={styles.root}>
      <View style={[styles.screen, { paddingTop: insets.top }]}>
        {!loggedIn ? (
          <LandingScreen loading={me.isLoading} error={me.error} />
        ) : tab === 'portfolio' ? (
          <PortfolioScreen />
        ) : tab === 'backtest' ? (
          <BacktestScreen />
        ) : tab === 'sim' ? (
          <SimulatorScreen />
        ) : (
          <HomeScreen name={me.data!.name || me.data!.username} onNavigate={setTab} />
        )}
      </View>
      {loggedIn && <TabBar tab={tab} onChange={setTab} />}
    </View>
  );
}

/** 미인증 랜딩 — 서비스 소개 + 로그인 CTA. web LandingPage 미러. */
function LandingScreen({ loading, error }: { loading: boolean; error: unknown }) {
  const login = useLogin();
  return (
    <ScrollView contentContainerStyle={styles.landing}>
      <View style={styles.hero}>
        <LogoMark size={52} />
        <Text style={styles.heroTitle}>곳간</Text>
        <Text style={styles.heroSub}>
          내 자산을 한 곳에. 토스증권 연동 · 백테스트 · 세후 배당 시뮬레이터.
        </Text>
        <Button label="토스증권 계정으로 시작하기" onPress={() => login.mutate()} disabled={login.isPending || loading} />
        <Text style={styles.heroNote}>Keycloak 보안 로그인 · 토큰은 기기 Keychain 에만 저장</Text>
        {loading && <ActivityIndicator style={{ marginTop: 16 }} />}
        {!!error && !isUnauthorized(error) && (
          <Text style={styles.errorText}>오류: {(error as Error).message}</Text>
        )}
        {login.error && <Text style={styles.errorText}>로그인에 실패했어요. 다시 시도해주세요.</Text>}
      </View>

      <View style={styles.featureList}>
        {FEATURES.map(f => (
          <View key={f.title} style={styles.featureCard}>
            <Text style={styles.featureEmoji}>{f.emoji}</Text>
            <View style={{ flex: 1 }}>
              <Text style={styles.featureTitle}>{f.title}</Text>
              <Text style={styles.featureDesc}>{f.desc}</Text>
            </View>
          </View>
        ))}
      </View>
    </ScrollView>
  );
}

/** 인증 홈(대시보드) — 인사 + 바로가기 + 로그아웃. 유저 정보는 노출하지 않음. */
function HomeScreen({ name, onNavigate }: { name: string; onNavigate: (t: Tab) => void }) {
  const logout = useLogout();
  return (
    <ScrollView contentContainerStyle={styles.home}>
      <Text style={styles.homeHello}>안녕하세요, {name} 님</Text>
      <Text style={styles.homeSub}>무엇부터 살펴볼까요?</Text>

      <View style={styles.linkList}>
        {LINKS.map(l => (
          <Pressable key={l.tab} onPress={() => onNavigate(l.tab)} style={styles.linkCard}>
            <Text style={styles.linkEmoji}>{l.emoji}</Text>
            <View style={{ flex: 1 }}>
              <Text style={styles.linkTitle}>{l.title}</Text>
              <Text style={styles.linkDesc}>{l.desc}</Text>
            </View>
            <Text style={styles.linkChevron}>›</Text>
          </Pressable>
        ))}
      </View>

      <Pressable
        onPress={() => logout.mutate()}
        disabled={logout.isPending}
        style={[styles.logout, logout.isPending && { opacity: 0.5 }]}>
        <Text style={styles.logoutText}>로그아웃</Text>
      </Pressable>
    </ScrollView>
  );
}

function TabBar({ tab, onChange }: { tab: Tab; onChange: (t: Tab) => void }) {
  const insets = useSafeAreaInsets();
  return (
    <View style={[styles.tabBar, { paddingBottom: insets.bottom + 8 }]}>
      {(['home', 'portfolio', 'backtest', 'sim'] as const).map(t => (
        <Pressable key={t} onPress={() => onChange(t)} style={styles.tab}>
          <Text style={[styles.tabText, tab === t && styles.tabTextActive]}>
            {t === 'home' ? '홈' : t === 'portfolio' ? '자산' : t === 'backtest' ? '백테스트' : '시뮬'}
          </Text>
        </Pressable>
      ))}
    </View>
  );
}

function Button({ label, onPress, disabled }: { label: string; onPress: () => void; disabled?: boolean }) {
  return (
    <Pressable
      onPress={onPress}
      disabled={disabled}
      style={({ pressed }) => [styles.button, disabled && styles.buttonDisabled, pressed && styles.buttonPressed]}>
      <Text style={styles.buttonText}>{label}</Text>
    </Pressable>
  );
}

function App() {
  const isDark = useColorScheme() === 'dark';
  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <QueryClientProvider client={queryClient}>
        <SafeAreaProvider>
          <StatusBar barStyle={isDark ? 'light-content' : 'dark-content'} />
          <Root />
        </SafeAreaProvider>
      </QueryClientProvider>
    </GestureHandlerRootView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  screen: { flex: 1 },
  tabBar: {
    flexDirection: 'row',
    backgroundColor: colors.surface,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: colors.line,
    paddingTop: 8,
  },
  tab: { flex: 1, alignItems: 'center', paddingVertical: 6 },
  tabText: { fontSize: 13, color: colors.muted, fontWeight: '500' },
  tabTextActive: { color: colors.accent, fontWeight: '700' },

  // 랜딩
  landing: { padding: 24, paddingBottom: 48 },
  hero: { alignItems: 'center', paddingVertical: 24 },
  heroTitle: { fontSize: 34, fontWeight: '800', color: colors.ink, marginTop: 12 },
  heroSub: { fontSize: 15, color: colors.muted, textAlign: 'center', marginTop: 10, lineHeight: 22, marginBottom: 22 },
  heroNote: { fontSize: 11, color: colors.muted, marginTop: 12, textAlign: 'center' },
  featureList: { gap: 12, marginTop: 12 },
  featureCard: {
    flexDirection: 'row',
    gap: 14,
    backgroundColor: colors.surface,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: colors.line,
    padding: 18,
  },
  featureEmoji: { fontSize: 26 },
  featureTitle: { fontSize: 16, fontWeight: '700', color: colors.ink },
  featureDesc: { fontSize: 13, color: colors.muted, marginTop: 4, lineHeight: 19 },

  // 홈(대시보드)
  home: { padding: 24, paddingBottom: 48 },
  homeHello: { fontSize: 24, fontWeight: '700', color: colors.ink },
  homeSub: { fontSize: 14, color: colors.muted, marginTop: 4, marginBottom: 20 },
  linkList: { gap: 12 },
  linkCard: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 14,
    backgroundColor: colors.surface,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: colors.line,
    padding: 18,
  },
  linkEmoji: { fontSize: 24 },
  linkTitle: { fontSize: 16, fontWeight: '700', color: colors.ink },
  linkDesc: { fontSize: 13, color: colors.muted, marginTop: 2 },
  linkChevron: { fontSize: 24, color: colors.muted },
  logout: {
    marginTop: 28,
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 10,
    paddingVertical: 13,
    alignItems: 'center',
  },
  logoutText: { color: colors.ink, fontSize: 15, fontWeight: '600' },

  button: { backgroundColor: colors.accent, paddingVertical: 14, paddingHorizontal: 24, borderRadius: 12, alignItems: 'center', alignSelf: 'stretch' },
  buttonPressed: { opacity: 0.85 },
  buttonDisabled: { opacity: 0.5 },
  buttonText: { color: '#fff', fontSize: 16, fontWeight: '700' },
  errorText: { color: colors.loss, marginTop: 16, textAlign: 'center' },
});

export default App;
