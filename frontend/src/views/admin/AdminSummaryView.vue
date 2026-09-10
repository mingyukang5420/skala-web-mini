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
    <div class="mb-6">
      <h1 class="text-h5 font-weight-bold">창고 혼잡도 요약</h1>
      <p class="text-body-2 text-medium-emphasis mt-1">실시간 배정 데이터 기준 도크별 점유 현황</p>
    </div>

    <v-select
      v-model="selectedWarehouseId"
      :items="warehouses"
      item-title="name"
      item-value="id"
      label="대상 창고"
      max-width="280"
      class="mb-4"
      @update:model-value="loadSummary"
    />

    <v-alert v-if="error" type="error" density="compact" variant="tonal" class="mb-4">{{ error }}</v-alert>

    <div v-if="loading" class="d-flex align-center ga-2 text-medium-emphasis">
      <v-progress-circular size="20" width="2" indeterminate />
      불러오는 중...
    </div>

    <template v-else-if="summary">
      <v-card rounded="lg" variant="flat" class="ai-box mb-6 pa-4">
        <div class="d-flex align-center ga-2 mb-2">
          <v-avatar size="24" color="primary" rounded="sm">
            <v-icon icon="mdi-robot-outline" size="16" color="white" />
          </v-avatar>
          <span class="font-weight-bold">AI 분석 요약</span>
        </div>
        <p class="text-body-2">{{ summary.summary }}</p>
      </v-card>

      <div class="text-subtitle-1 font-weight-medium mb-3">도크별 실시간 점유율</div>

      <v-card rounded="lg" variant="flat" border>
        <v-list lines="two">
          <v-list-item v-for="d in summary.docks" :key="d.dockId">
            <div class="d-flex align-center ga-4 w-100">
              <span class="dock-name font-weight-medium">{{ d.name }}</span>
              <v-progress-linear
                :model-value="d.occupied ? 100 : 0"
                :color="d.occupied ? 'primary' : 'success'"
                height="10"
                rounded
                class="flex-grow-1"
              />
              <v-chip :color="d.occupied ? 'primary' : 'success'" size="small" variant="tonal" class="flex-shrink-0">
                {{ occupiedLabel(d) }}
              </v-chip>
            </div>
          </v-list-item>
        </v-list>
      </v-card>
    </template>
  </AdminLayout>
</template>

<style scoped>
.ai-box {
  background: rgba(47, 143, 232, 0.1) !important;
  color: inherit !important;
}

.dock-name {
  width: 90px;
  flex-shrink: 0;
}
</style>
