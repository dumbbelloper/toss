import { createRootRoute, createRoute, createRouter } from '@tanstack/react-router'

import { HomePage } from './pages/HomePage'
import { PortfolioPage } from './pages/PortfolioPage'
import { RootLayout } from './ui/RootLayout'

const rootRoute = createRootRoute({ component: RootLayout })

const indexRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/',
  component: HomePage,
})

const portfolioRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/portfolio',
  component: PortfolioPage,
})

const routeTree = rootRoute.addChildren([indexRoute, portfolioRoute])

export const router = createRouter({ routeTree })

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}
