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
    { path: '/w/:warehouseId', name: 'assign', component: AssignView },
    { path: '/assignments/:id/cancel', name: 'cancel', component: CancelView },
    { path: '/admin', name: 'admin-login', component: AdminLoginView },
    {
      path: '/admin/warehouses',
      name: 'admin-warehouses',
      component: AdminWarehouseListView,
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/docks',
      name: 'admin-docks-overview',
      component: AdminDockListView,
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/summary',
      name: 'admin-summary',
      component: AdminSummaryView,
      meta: { requiresAuth: true },
    },
  ],
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !isLoggedIn()) {
    return { name: 'admin-login' }
  }
})

export default router
