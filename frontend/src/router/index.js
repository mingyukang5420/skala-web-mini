import { createRouter, createWebHistory } from 'vue-router'
import AssignView from '../views/AssignView.vue'
import CancelView from '../views/CancelView.vue'
import AdminLoginView from '../views/admin/AdminLoginView.vue'
import AdminWarehouseListView from '../views/admin/AdminWarehouseListView.vue'
import AdminDockListView from '../views/admin/AdminDockListView.vue'
import AdminSummaryView from '../views/admin/AdminSummaryView.vue'
import { isLoggedIn } from '../api/adminClient'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/w/:warehouseId', name: 'assign', component: AssignView, meta: { title: '콕배정 - 도크 배정' } },
    { path: '/assignments/:id/cancel', name: 'cancel', component: CancelView, meta: { title: '콕배정 - 배정 취소' } },
    { path: '/admin', name: 'admin-login', component: AdminLoginView, meta: { title: '콕배정 관리자 - 로그인' } },
    {
      path: '/admin/warehouses',
      name: 'admin-warehouses',
      component: AdminWarehouseListView,
      meta: { requiresAuth: true, title: '콕배정 관리자 - 창고 관리' },
    },
    {
      path: '/admin/warehouses/:id/docks',
      name: 'admin-warehouse-docks',
      component: AdminDockListView,
      meta: { requiresAuth: true, title: '콕배정 관리자 - 도크 관리' },
    },
    {
      path: '/admin/warehouses/:id/summary',
      name: 'admin-warehouse-summary',
      component: AdminSummaryView,
      meta: { requiresAuth: true, title: '콕배정 관리자 - 혼잡도 요약' },
    },
  ],
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !isLoggedIn()) {
    return { name: 'admin-login' }
  }
})

router.afterEach((to) => {
  document.title = to.meta.title || '콕배정'
})

export default router
