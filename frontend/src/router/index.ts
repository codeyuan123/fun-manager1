import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/login/LoginView.vue'),
      meta: { public: true, title: '登录' },
    },
    {
      path: '/',
      component: () => import('../layout/AppLayout.vue'),
      children: [
        { path: '', redirect: '/dashboard' },
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('../views/dashboard/DashboardView.vue'),
          meta: { title: '资产看板' },
        },
        {
          path: 'positions',
          name: 'positions',
          component: () => import('../views/positions/PositionsView.vue'),
          meta: { title: '持仓台账' },
        },
        {
          path: 'watchlist',
          name: 'watchlist',
          component: () => import('../views/watchlist/WatchlistView.vue'),
          meta: { title: '自选基金' },
        },
        {
          path: 'fund/:code',
          name: 'fund-detail',
          component: () => import('../views/fund/FundDetailView.vue'),
          meta: { title: '基金详情' },
        },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.public) return true
  if (!auth.token) return '/login'
  return true
})

export default router
