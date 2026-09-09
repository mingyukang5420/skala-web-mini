import { createRouter, createWebHistory } from 'vue-router'
import AssignView from '../views/AssignView.vue'
import CancelView from '../views/CancelView.vue'
import AdminLoginView from '../views/admin/AdminLoginView.vue'
import AdminWarehouseListView from '../views/admin/AdminWarehouseListView.vue'
import AdminWarehouseFormView from '../views/admin/AdminWarehouseFormView.vue'
import AdminDockListView from '../views/admin/AdminDockListView.vue'
import AdminDockFormView from '../views/admin/AdminDockFormView.vue'
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
      path: '/admin/warehouses/new',
      name: 'admin-warehouse-new',
      component: AdminWarehouseFormView,
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/warehouses/:id/edit',
      name: 'admin-warehouse-edit',
      component: AdminWarehouseFormView,
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/warehouses/:id/docks',
      name: 'admin-docks',
      component: AdminDockListView,
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/warehouses/:id/docks/new',
      name: 'admin-dock-new',
      component: AdminDockFormView,
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/docks/:id/edit',
      name: 'admin-dock-edit',
      component: AdminDockFormView,
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
