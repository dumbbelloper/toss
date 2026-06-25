/**
 * @format
 */

import React from 'react';
import ReactTestRenderer from 'react-test-renderer';

// 인증 훅을 mock 해 비동기(react-query)·네트워크 없이 화면 렌더만 검증한다.
jest.mock('../src/auth/auth', () => ({
  useMe: () => ({ data: undefined, isLoading: false, error: null }),
  useLogin: () => ({ mutate: jest.fn(), isPending: false, error: null }),
  useLogout: () => ({ mutate: jest.fn(), isPending: false, error: null }),
  isUnauthorized: () => true,
}));

import App from '../App';

test('renders auth screen (signed out)', async () => {
  let tree!: ReactTestRenderer.ReactTestRenderer;
  await ReactTestRenderer.act(() => {
    tree = ReactTestRenderer.create(<App />);
  });
  const json = JSON.stringify(tree.toJSON());
  expect(json).toContain('Toss Mobile');
  expect(json).toContain('로그인이 필요합니다.');
  await ReactTestRenderer.act(() => {
    tree.unmount();
  });
});
