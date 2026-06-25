/**
 * Toss Mobile — 인증 데모 화면.
 *
 * 웹(BFF 쿠키 세션)과 달리 모바일은 react-native-app-auth 로 PKCE 토큰을 받아
 * Bearer 로 백엔드 /api/me 를 호출한다. 로그인 여부에 따라 사용자 정보 또는 로그인 버튼을 보인다.
 */

import { useState } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  ActivityIndicator,
  Pressable,
  StatusBar,
  StyleSheet,
  Text,
  View,
  useColorScheme,
} from 'react-native';
import { SafeAreaProvider, useSafeAreaInsets } from 'react-native-safe-area-context';

import { isUnauthorized, useLogin, useLogout, useMe } from './src/auth/auth';
import { PortfolioScreen } from './src/screens/PortfolioScreen';
import { LogoMark } from './src/ui/Logo';

const queryClient = new QueryClient();

type Tab = 'home' | 'portfolio';

/** 로그인 시 홈/자산 탭 전환. 미로그인이면 홈(인증 화면)만. */
function Root() {
  const me = useMe();
  const [tab, setTab] = useState<Tab>('home');
  const loggedIn = !!me.data;

  return (
    <View style={styles.root}>
      <View style={styles.screen}>
        {loggedIn && tab === 'portfolio' ? <PortfolioScreen /> : <AuthScreen />}
      </View>
      {loggedIn && <TabBar tab={tab} onChange={setTab} />}
    </View>
  );
}

function TabBar({ tab, onChange }: { tab: Tab; onChange: (t: Tab) => void }) {
  const insets = useSafeAreaInsets();
  return (
    <View style={[styles.tabBar, { paddingBottom: insets.bottom + 8 }]}>
      {(['home', 'portfolio'] as const).map(t => (
        <Pressable key={t} onPress={() => onChange(t)} style={styles.tab}>
          <Text style={[styles.tabText, tab === t && styles.tabTextActive]}>
            {t === 'home' ? '홈' : '자산'}
          </Text>
        </Pressable>
      ))}
    </View>
  );
}

function AuthScreen() {
  const insets = useSafeAreaInsets();
  const me = useMe();
  const login = useLogin();
  const logout = useLogout();

  const busy = me.isLoading || login.isPending || logout.isPending;

  return (
    <View style={[styles.container, { paddingTop: insets.top + 24 }]}>
      <View style={styles.brand}>
        <LogoMark size={26} />
        <Text style={styles.title}>곳간</Text>
      </View>

      {me.isLoading && <ActivityIndicator style={styles.gap} />}

      {/* 로그인됨 */}
      {me.data && (
        <View style={styles.card}>
          <Text style={styles.hello}>안녕하세요, {me.data.username} 님</Text>
          <Field label="subject" value={me.data.subject} />
          <Field label="email" value={me.data.email ?? '—'} />
          <Field label="name" value={me.data.name ?? '—'} />
          <Field label="roles" value={me.data.roles.join(', ') || '—'} />
        </View>
      )}

      {/* 로그아웃 상태(401) 또는 미인증 */}
      {!me.data && !me.isLoading && (
        <Text style={styles.gap}>
          {me.error && !isUnauthorized(me.error)
            ? `오류: ${(me.error as Error).message}`
            : '로그인이 필요합니다.'}
        </Text>
      )}

      <View style={styles.gap}>
        {me.data ? (
          <Button label="로그아웃" onPress={() => logout.mutate()} disabled={busy} />
        ) : (
          <Button label="Keycloak 로그인" onPress={() => login.mutate()} disabled={busy} />
        )}
      </View>

      {login.error && (
        <Text style={styles.errorText}>로그인에 실패했어요. 잠시 후 다시 시도해주세요.</Text>
      )}
    </View>
  );
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.field}>
      <Text style={styles.fieldLabel}>{label}</Text>
      <Text style={styles.fieldValue}>{value}</Text>
    </View>
  );
}

function Button({
  label,
  onPress,
  disabled,
}: {
  label: string;
  onPress: () => void;
  disabled?: boolean;
}) {
  return (
    <Pressable
      onPress={onPress}
      disabled={disabled}
      style={({ pressed }) => [
        styles.button,
        disabled && styles.buttonDisabled,
        pressed && styles.buttonPressed,
      ]}>
      <Text style={styles.buttonText}>{label}</Text>
    </Pressable>
  );
}

function App() {
  const isDark = useColorScheme() === 'dark';
  return (
    <QueryClientProvider client={queryClient}>
      <SafeAreaProvider>
        <StatusBar barStyle={isDark ? 'light-content' : 'dark-content'} />
        <Root />
      </SafeAreaProvider>
    </QueryClientProvider>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: '#f5f5f7' },
  screen: { flex: 1 },
  tabBar: {
    flexDirection: 'row',
    backgroundColor: '#fff',
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: '#ddd',
    paddingTop: 8,
  },
  tab: { flex: 1, alignItems: 'center', paddingVertical: 6 },
  tabText: { fontSize: 13, color: '#9ca3af', fontWeight: '500' },
  tabTextActive: { color: '#3182f6', fontWeight: '700' },
  container: { flex: 1, paddingHorizontal: 24, backgroundColor: '#f5f5f7' },
  brand: { flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 8 },
  title: { fontSize: 28, fontWeight: '700' },
  gap: { marginTop: 20 },
  card: {
    marginTop: 24,
    padding: 20,
    backgroundColor: '#fff',
    borderRadius: 12,
    gap: 8,
  },
  hello: { fontSize: 18, fontWeight: '600', marginBottom: 8 },
  field: { flexDirection: 'row', justifyContent: 'space-between' },
  fieldLabel: { color: '#888', fontSize: 13 },
  fieldValue: { fontSize: 13, fontWeight: '500', flexShrink: 1, textAlign: 'right' },
  button: {
    backgroundColor: '#1e6fff',
    paddingVertical: 14,
    borderRadius: 10,
    alignItems: 'center',
  },
  buttonPressed: { opacity: 0.85 },
  buttonDisabled: { backgroundColor: '#9bbcf2' },
  buttonText: { color: '#fff', fontSize: 16, fontWeight: '600' },
  errorText: { color: '#c0392b', marginTop: 16 },
});

export default App;
