import { createRouter, createWebHistory } from 'vue-router'
import AssignView from '../views/AssignView.vue'
import CancelView from '../views/CancelView.vue'
import AdminLoginView from '../views/admin/AdminLoginView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/w/:warehouseId', name: 'assign', component: AssignView },
    { path: '/assignments/:id/cancel', name: 'cancel', component: CancelView },
    { path: '/admin', name: 'admin-login', component: AdminLoginView },
  ],
})

export default router
