/* eslint-env jest */
// jest 환경엔 네이티브 브리지가 없으므로 네이티브 모듈을 mock 한다.

jest.mock('react-native-keychain', () => ({
  setGenericPassword: jest.fn(() => Promise.resolve(false)),
  getGenericPassword: jest.fn(() => Promise.resolve(false)), // 저장된 세션 없음
  resetGenericPassword: jest.fn(() => Promise.resolve(true)),
}));

// SafeAreaProvider 는 실제로는 inset 측정 전까지 children 을 렌더하지 않으므로
// (test-renderer 엔 레이아웃이 없음) children 을 그대로 렌더하도록 mock.
jest.mock('react-native-safe-area-context', () => {
  const inset = { top: 0, right: 0, bottom: 0, left: 0 };
  return {
    SafeAreaProvider: ({ children }) => children,
    useSafeAreaInsets: () => inset,
    SafeAreaView: ({ children }) => children,
  };
});

jest.mock('react-native-app-auth', () => ({
  authorize: jest.fn(() => Promise.resolve({})),
  refresh: jest.fn(() => Promise.resolve({})),
  revoke: jest.fn(() => Promise.resolve()),
}));

// 네트워크 호출은 401(로그아웃)로 고정 — useMe 가 결정적으로 "미인증" 상태가 된다.
global.fetch = jest.fn(() =>
  Promise.resolve({ ok: false, status: 401, text: () => Promise.resolve('') }),
);
