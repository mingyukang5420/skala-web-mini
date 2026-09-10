<script setup>
import { useRouter } from 'vue-router'
import { clearToken } from '../../api/adminClient'

const router = useRouter()

const navItems = [{ to: { name: 'admin-warehouses' }, label: '창고 관리', icon: 'mdi-warehouse' }]

function logout() {
  clearToken()
  router.push({ name: 'admin-login' })
}
</script>

<template>
  <v-navigation-drawer permanent theme="dark" width="220" class="admin-sidebar">
    <div class="admin-brand">
      <v-avatar size="32" rounded="lg">
        <v-img src="/favicon.png" alt="콕배정" />
      </v-avatar>
      <span class="admin-brand-text">콕배정 관리자</span>
    </div>

    <v-list nav density="comfortable" class="admin-nav" bg-color="transparent">
      <v-list-item
        v-for="item in navItems"
        :key="item.label"
        :to="item.to"
        :prepend-icon="item.icon"
        :title="item.label"
        rounded="lg"
        class="admin-nav-item"
      />
    </v-list>

    <template #append>
      <div class="pa-3">
        <v-btn block variant="outlined" color="white" prepend-icon="mdi-logout" @click="logout">로그아웃</v-btn>
      </div>
    </template>
  </v-navigation-drawer>

  <v-main>
    <v-container fluid class="admin-content pa-6 pa-md-8">
      <slot />
    </v-container>
  </v-main>
</template>

<style scoped>
.admin-sidebar {
  background: #0f172a !important;
}

.admin-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 16px 12px;
}

.admin-brand-text {
  color: #fff;
  font-weight: 700;
  font-size: 16px;
}

.admin-nav {
  padding: 4px 8px;
}

.admin-nav-item {
  color: rgba(255, 255, 255, 0.72);
}

.admin-nav-item:hover {
  background: rgba(255, 255, 255, 0.06);
}

.admin-nav-item.v-list-item--active {
  background: rgba(255, 255, 255, 0.12) !important;
  color: #fff;
}

.admin-content {
  max-width: 1100px;
}
</style>
