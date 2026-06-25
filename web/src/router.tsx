import { createRootRoute, createRoute, createRouter } from '@tanstack/react-router'

import { BacktestPage } from './pages/BacktestPage'
import { HomePage } from './pages/HomePage'
import { PortfolioPage } from './pages/PortfolioPage'
import { SimulatorPage } from './pages/SimulatorPage'
import { RequireAuth } from './ui/RequireAuth'
import { RootLayout } from './ui/RootLayout'

const rootRoute = createRootRoute({ component: RootLayout })

// 인덱스(/)는 미인증=랜딩, 인증=대시보드 (HomePage 가 분기). 나머지는 인증 가드.
const indexRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/',
  component: HomePage,
})

const portfolioRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/portfolio',
  component: () => (
    <RequireAuth>
      <PortfolioPage />
    </RequireAuth>
  ),
})

const backtestRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/backtest',
  component: () => (
    <RequireAuth>
      <BacktestPage />
    </RequireAuth>
  ),
})

const simulatorRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/simulator',
  component: () => (
    <RequireAuth>
      <SimulatorPage />
    </RequireAuth>
  ),
})

const routeTree = rootRoute.addChildren([indexRoute, portfolioRoute, backtestRoute, simulatorRoute])

export const router = createRouter({ routeTree })

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}
