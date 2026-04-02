import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AppLayout from '../layout/AppLayout.vue'
import LoginView from '../views/login/LoginView.vue'
import DashboardView from '../views/dashboard/DashboardView.vue'
import PositionsView from '../views/positions/PositionsView.vue'
import WatchlistView from '../views/watchlist/WatchlistView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { public: true, title: '登录' },
    },
    {
      path: '/',
      component: AppLayout,
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', name: 'dashboard', component: DashboardView, meta: { title: '数据看板' } },
        { path: 'positions', name: 'positions', component: PositionsView, meta: { title: '持仓管理' } },
        { path: 'watchlist', name: 'watchlist', component: WatchlistView, meta: { title: '自选基金' } },
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
