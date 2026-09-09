<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { adminFetch } from '../../api/adminClient'

const route = useRoute()
const warehouseId = route.params.id

const docks = ref([])
const loading = ref(true)
const error = ref('')

const sizeLabel = { LARGE: '대형', MEDIUM: '중형', SMALL: '소형' }
const statusLabel = { AVAILABLE: '배정 가능', OCCUPIED: '사용 중', MAINTENANCE: '점검 중' }

async function load() {
  loading.value = true
  error.value = ''
  try {
    docks.value = await adminFetch(`/admin/docks?warehouseId=${warehouseId}`)
  } catch (err) {
    error.value = err.message || '목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function toggleActive(dock) {
  const action = dock.active ? 'deactivate' : 'activate'
  error.value = ''
  try {
    await adminFetch(`/admin/docks/${dock.id}/${action}`, { method: 'POST' })
    await load()
  } catch (err) {
    error.value = err.message || '처리에 실패했습니다.'
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <h1>도크 관리</h1>
    <div class="header-row">
      <router-link :to="{ name: 'admin-warehouses' }">← 창고 목록</router-link>
      <router-link :to="{ name: 'admin-dock-new', params: { id: warehouseId } }">+ 도크 등록</router-link>
    </div>

    <p v-if="error" class="error-text">{{ error }}</p>
    <div v-if="loading">불러오는 중...</div>
    <ul v-else class="list">
      <li v-for="d in docks" :key="d.id">
        <strong>{{ d.name }}</strong>
        <span>{{ sizeLabel[d.size] }} / {{ statusLabel[d.status] }}</span>
        <span :class="d.active ? 'badge-active' : 'badge-inactive'">{{ d.active ? '활성' : '비활성' }}</span>
        <router-link :to="{ name: 'admin-dock-edit', params: { id: d.id } }">수정</router-link>
        <button @click="toggleActive(d)">{{ d.active ? '비활성화' : '활성화' }}</button>
      </li>
    </ul>
  </div>
</template>
