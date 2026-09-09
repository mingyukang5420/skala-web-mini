<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { adminFetch, clearToken } from '../../api/adminClient'

const router = useRouter()
const warehouses = ref([])
const loading = ref(true)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    warehouses.value = await adminFetch('/admin/warehouses')
  } catch (err) {
    error.value = err.message || '목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function toggleActive(warehouse) {
  const action = warehouse.active ? 'deactivate' : 'activate'
  error.value = ''
  try {
    await adminFetch(`/admin/warehouses/${warehouse.id}/${action}`, { method: 'POST' })
    await load()
  } catch (err) {
    error.value = err.message || '처리에 실패했습니다.'
  }
}

function logout() {
  clearToken()
  router.push({ name: 'admin-login' })
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="header-row">
      <h1>창고 관리</h1>
      <button @click="logout">로그아웃</button>
    </div>

    <router-link :to="{ name: 'admin-warehouse-new' }">+ 창고 등록</router-link>

    <p v-if="error" class="error-text">{{ error }}</p>
    <div v-if="loading">불러오는 중...</div>
    <ul v-else class="list">
      <li v-for="w in warehouses" :key="w.id">
        <strong>{{ w.name }}</strong>
        <span :class="w.active ? 'badge-active' : 'badge-inactive'">{{ w.active ? '활성' : '비활성' }}</span>
        <router-link :to="{ name: 'admin-warehouse-edit', params: { id: w.id } }">수정</router-link>
        <router-link :to="{ name: 'admin-docks', params: { id: w.id } }">도크 관리</router-link>
        <button @click="toggleActive(w)">{{ w.active ? '비활성화' : '활성화' }}</button>
      </li>
    </ul>
  </div>
</template>
