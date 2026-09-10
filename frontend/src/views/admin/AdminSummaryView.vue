<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { adminFetch } from '../../api/adminClient'
import AdminLayout from './AdminLayout.vue'
import AdminWarehouseDetailLayout from './AdminWarehouseDetailLayout.vue'

const route = useRoute()
const warehouseId = route.params.id
const summary = ref(null)
const loading = ref(true)
const error = ref('')

const statusLabel = { AVAILABLE: '사용가능', OCCUPIED: '사용중', MAINTENANCE: '점검중' }

async function loadSummary() {
  loading.value = true
  error.value = ''
  try {
    summary.value = await adminFetch(`/admin/warehouses/${warehouseId}/summary`)
  } catch (err) {
    error.value = err.message || '요약을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

function occupiedLabel(dock) {
  return dock.occupied ? '사용중' : statusLabel[dock.status]
}

onMounted(loadSummary)
</script>

<template>
  <AdminLayout>
    <AdminWarehouseDetailLayout :warehouse-id="warehouseId">
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
    </AdminWarehouseDetailLayout>
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
