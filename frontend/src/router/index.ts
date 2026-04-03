import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/login/LoginView.vue'),
      meta: { public: true, title: '登录', ignoreTab: true },
    },
    {
      path: '/positions',
      redirect: '/fund/positions',
    },
    {
      path: '/watchlist',
      redirect: '/fund/watchlist',
    },
    {
      path: '/dashboard',
      redirect: '/dashboard/workbench',
    },
    {
      path: '/',
      component: () => import('../layout/AppLayout.vue'),
      children: [
        { path: '', redirect: '/dashboard/workbench' },
        {
          path: 'dashboard/workbench',
          name: 'dashboard-workbench',
          component: () => import('../views/dashboard/DashboardView.vue'),
          meta: {
            title: '工作台',
            menu: true,
            group: 'dashboard',
            groupTitle: 'Dashboard',
            icon: 'House',
            affix: true,
          },
        },
        {
          path: 'fund/positions',
          name: 'fund-positions',
          component: () => import('../views/positions/PositionsView.vue'),
          meta: {
            title: '持仓',
            menu: true,
            group: 'fund',
            groupTitle: '基金',
            icon: 'Wallet',
          },
        },
        {
          path: 'fund/watchlist',
          name: 'fund-watchlist',
          component: () => import('../views/watchlist/WatchlistView.vue'),
          meta: {
            title: '自选',
            menu: true,
            group: 'fund',
            groupTitle: '基金',
            icon: 'Star',
          },
        },
        {
          path: 'fund/:code',
          name: 'fund-detail',
          component: () => import('../views/fund/FundDetailView.vue'),
          meta: {
            title: '基金详情',
            hideInMenu: true,
            group: 'fund',
          },
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
