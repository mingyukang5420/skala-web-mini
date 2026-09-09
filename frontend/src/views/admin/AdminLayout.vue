<script setup>
import { useRouter } from 'vue-router'
import { clearToken } from '../../api/adminClient'

const router = useRouter()

function logout() {
  clearToken()
  router.push({ name: 'admin-login' })
}
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="admin-brand">콕배정 관리자</div>
      <nav class="admin-nav">
        <router-link :to="{ name: 'admin-warehouses' }">창고 관리</router-link>
        <router-link :to="{ name: 'admin-summary' }">혼잡도 요약</router-link>
      </nav>
      <button class="logout-btn" @click="logout">로그아웃</button>
    </aside>
    <main class="admin-content">
      <slot />
    </main>
  </div>
</template>

<style scoped>
.admin-shell {
  display: flex;
  min-height: 100vh;
}

.admin-sidebar {
  width: 180px;
  flex-shrink: 0;
  border-right: 1px solid var(--border);
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.admin-brand {
  font-weight: 700;
  color: var(--text-h);
}

.admin-nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.admin-nav a {
  padding: 8px 10px;
  border-radius: 6px;
  color: var(--text);
  text-decoration: none;
}

.admin-nav a.router-link-active {
  background: var(--accent-bg);
  color: var(--accent);
  font-weight: 600;
}

.logout-btn {
  margin-top: auto;
  padding: 8px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: transparent;
}

.admin-content {
  flex: 1;
  min-width: 0;
}

.admin-content .page {
  max-width: none;
  margin: 0;
}

@media (max-width: 640px) {
  .admin-shell {
    flex-direction: column;
  }

  .admin-sidebar {
    width: auto;
    flex-direction: row;
    align-items: center;
    border-right: none;
    border-bottom: 1px solid var(--border);
  }

  .admin-nav {
    flex-direction: row;
  }

  .logout-btn {
    margin-top: 0;
    margin-left: auto;
  }
}
</style>
