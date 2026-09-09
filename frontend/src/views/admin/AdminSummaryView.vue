<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { adminFetch } from '../../api/adminClient'
import AdminLayout from './AdminLayout.vue'

const route = useRoute()
const warehouses = ref([])
const selectedWarehouseId = ref(null)
const summary = ref(null)
const loading = ref(false)
const error = ref('')

const statusLabel = { AVAILABLE: '사용가능', OCCUPIED: '사용중', MAINTENANCE: '점검중' }

async function loadWarehouses() {
  warehouses.value = await adminFetch('/admin/warehouses')
  const preselected = Number(route.query.warehouseId)
  const initial = warehouses.value.find((w) => w.id === preselected) ?? warehouses.value[0]
  if (initial) {
    selectedWarehouseId.value = initial.id
    await loadSummary()
  }
}

async function loadSummary() {
  if (!selectedWarehouseId.value) return
  loading.value = true
  error.value = ''
  summary.value = null
  try {
    summary.value = await adminFetch(`/admin/warehouses/${selectedWarehouseId.value}/summary`)
  } catch (err) {
    error.value = err.message || '요약을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

function occupiedLabel(dock) {
  return dock.occupied ? '사용중' : statusLabel[dock.status]
}

onMounted(loadWarehouses)
</script>

<template>
  <AdminLayout>
    <div class="page">
      <h1>창고 혼잡도 요약</h1>
      <p class="subtitle">실시간 배정 데이터 기준 도크별 점유 현황</p>

      <label class="warehouse-select">
        대상 창고
        <select v-model="selectedWarehouseId" @change="loadSummary">
          <option v-for="w in warehouses" :key="w.id" :value="w.id">{{ w.name }}</option>
        </select>
      </label>

      <p v-if="error" class="error-text">{{ error }}</p>
      <div v-if="loading">불러오는 중...</div>

      <template v-else-if="summary">
        <div class="ai-box">
          <strong>AI 분석 요약</strong>
          <p>{{ summary.summary }}</p>
        </div>

        <ul class="dock-occupancy-list">
          <li v-for="d in summary.docks" :key="d.dockId">
            <span class="dock-name">{{ d.name }}</span>
            <div class="bar-track">
              <div class="bar-fill" :class="{ occupied: d.occupied }" :style="{ width: d.occupied ? '100%' : '0%' }"></div>
            </div>
            <span :class="d.occupied ? 'badge-occupied' : 'badge-free'">{{ occupiedLabel(d) }}</span>
          </li>
        </ul>
      </template>
    </div>
  </AdminLayout>
</template>

<style scoped>
.subtitle {
  color: var(--text);
  margin-bottom: 16px;
}

.warehouse-select {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 14px;
  max-width: 240px;
  margin-bottom: 16px;
}

.ai-box {
  background: var(--accent-bg);
  border: 1px solid var(--accent-border, var(--border));
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.dock-occupancy-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.dock-occupancy-list li {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border: 1px solid var(--border);
  border-radius: 10px;
}

.dock-name {
  width: 80px;
  flex-shrink: 0;
  font-weight: 600;
}

.bar-track {
  flex: 1;
  height: 10px;
  border-radius: 999px;
  background: var(--border);
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  background: var(--success);
}

.bar-fill.occupied {
  background: var(--accent);
}

.badge-occupied,
.badge-free {
  font-size: 13px;
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid var(--border);
  flex-shrink: 0;
}

.badge-occupied {
  color: var(--accent);
  border-color: var(--accent);
  background: var(--accent-bg);
}

.badge-free {
  color: var(--success);
  border-color: var(--success);
  background: var(--success-bg);
}
</style>
