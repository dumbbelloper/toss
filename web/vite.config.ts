import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

const BACKEND = 'http://localhost:8080'

// BFF: SPA(:5173)와 백엔드(:8080)를 dev proxy 로 동일 출처처럼 묶는다.
// → 세션 쿠키/CSRF 가 first-party 로 자연스럽게 동작하고 CORS 가 불필요하다.
//   토큰은 백엔드 세션에만 있고 브라우저로 내려오지 않는다.
// 로그인 콜백(/login/oauth2/code/keycloak)도 proxy 를 거치게 해 브라우저가 :5173 에 머문다.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    // changeOrigin 은 끈다(기본값). Host 를 :5173 으로 유지해야 로그인 성공 후
    // 백엔드의 상대 리다이렉트("/")가 :5173 으로 해석돼 브라우저가 SPA 로 복귀한다.
    proxy: {
      '/api': { target: BACKEND },
      '/oauth2': { target: BACKEND },
      '/login/oauth2': { target: BACKEND },
      '/logout': { target: BACKEND },
    },
  },
})
