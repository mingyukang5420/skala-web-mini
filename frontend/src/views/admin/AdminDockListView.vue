<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { adminFetch } from '../../api/adminClient'
import AdminLayout from './AdminLayout.vue'
import DockFormDialog from './DockFormDialog.vue'
import ConfirmDialog from './ConfirmDialog.vue'

const route = useRoute()
const router = useRouter()

const sizeLabel = { LARGE: '대형', MEDIUM: '중형', SMALL: '소형' }
const statusMeta = {
  AVAILABLE: { label: '사용가능', color: 'success' },
  OCCUPIED: { label: '사용중', color: 'grey-darken-1' },
  MAINTENANCE: { label: '점검중', color: 'error' },
}
const featureMeta = [
  { key: 'hasLeveler', label: '도크 레벨러', icon: 'mdi-elevator-passenger-outline' },
  { key: 'hasDockSeal', label: '도크씰', icon: 'mdi-door' },
  { key: 'supportsColdChain', label: '냉동/냉장', icon: 'mdi-snowflake' },
  { key: 'supportsHazmat', label: '위험물', icon: 'mdi-alert-octagon-outline' },
]

const headers = [
  { title: '도크명', key: 'name' },
  { title: '규격', key: 'size' },
  { title: '가동 상태', key: 'status' },
  { title: '보유 특성', key: 'features', sortable: false },
  { title: '상태', key: 'active', sortable: false },
  { title: '관리 작업', key: 'actions', sortable: false, align: 'end' },
]

const warehouses = ref([])
const selectedWarehouseId = ref(null)
const docks = ref([])
const loading = ref(false)
const error = ref('')

const formOpen = ref(false)
const editingId = ref(null)
const confirmOpen = ref(false)
const confirmTarget = ref(null)
const confirmLoading = ref(false)

async function loadWarehouses() {
  warehouses.value = await adminFetch('/admin/warehouses')
  const preselected = Number(route.query.warehouseId)
  const initial = warehouses.value.find((w) => w.id === preselected) ?? warehouses.value[0]
  if (initial) {
    selectedWarehouseId.value = initial.id
    await loadDocks()
  }
}

async function loadDocks() {
  if (!selectedWarehouseId.value) return
  loading.value = true
  error.value = ''
  try {
    docks.value = await adminFetch(`/admin/docks?warehouseId=${selectedWarehouseId.value}`)
  } catch (err) {
    error.value = err.message || '목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

function onWarehouseChange() {
  router.replace({ name: 'admin-docks-overview', query: { warehouseId: selectedWarehouseId.value } })
  loadDocks()
}

function openCreate() {
  editingId.value = null
  formOpen.value = true
}

function openEdit(dock) {
  editingId.value = dock.id
  formOpen.value = true
}

function askToggle(dock) {
  confirmTarget.value = dock
  confirmOpen.value = true
}

async function confirmToggle() {
  const dock = confirmTarget.value
  if (!dock) return
  confirmLoading.value = true
  const action = dock.active ? 'deactivate' : 'activate'
  try {
    await adminFetch(`/admin/docks/${dock.id}/${action}`, { method: 'POST' })
    confirmOpen.value = false
    await loadDocks()
  } catch (err) {
    error.value = err.message || '처리에 실패했습니다.'
  } finally {
    confirmLoading.value = false
  }
}

onMounted(loadWarehouses)
</script>

<template>
  <AdminLayout>
    <div class="d-flex flex-wrap justify-space-between align-start mb-6 ga-4">
      <div>
        <h1 class="text-h5 font-weight-bold">도크 관리</h1>
        <p class="text-body-2 text-medium-emphasis mt-1">창고별 도크 현황을 관리합니다</p>
      </div>
      <v-btn color="primary" variant="flat" prepend-icon="mdi-plus" :disabled="!selectedWarehouseId" @click="openCreate">
        도크 등록
      </v-btn>
    </div>

    <v-select
      v-model="selectedWarehouseId"
      :items="warehouses"
      item-title="name"
      item-value="id"
      label="대상 창고"
      max-width="280"
      class="mb-4"
      @update:model-value="onWarehouseChange"
    />

    <v-alert v-if="error" type="error" density="compact" variant="tonal" class="mb-4">{{ error }}</v-alert>

    <v-card rounded="lg" variant="flat" border>
      <v-data-table :headers="headers" :items="docks" :loading="loading" item-value="id" no-data-text="등록된 도크가 없습니다.">
        <template #item.name="{ item }">
          <span class="font-weight-medium">{{ item.name }}</span>
        </template>
        <template #item.size="{ item }">{{ sizeLabel[item.size] }}</template>
        <template #item.status="{ item }">
          <v-chip :color="statusMeta[item.status].color" size="small" variant="tonal">
            {{ statusMeta[item.status].label }}
          </v-chip>
        </template>
        <template #item.features="{ item }">
          <div class="d-flex flex-wrap ga-1">
            <v-chip
              v-for="f in featureMeta.filter((f) => item[f.key])"
              :key="f.key"
              size="x-small"
              variant="outlined"
              :prepend-icon="f.icon"
            >
              {{ f.label }}
            </v-chip>
          </div>
        </template>
        <template #item.active="{ item }">
          <v-chip :color="item.active ? 'success' : 'error'" size="small" variant="tonal">
            {{ item.active ? '활성' : '비활성' }}
          </v-chip>
        </template>
        <template #item.actions="{ item }">
          <div class="d-flex justify-end ga-2">
            <v-btn size="small" variant="outlined" @click="openEdit(item)">수정</v-btn>
            <v-btn size="small" variant="outlined" color="error" @click="askToggle(item)">
              {{ item.active ? '비활성화' : '활성화' }}
            </v-btn>
          </div>
        </template>
      </v-data-table>
    </v-card>

    <DockFormDialog
      v-model="formOpen"
      :dock-id="editingId"
      :warehouse-id="selectedWarehouseId"
      @saved="loadDocks"
    />

    <ConfirmDialog
      v-model="confirmOpen"
      title="도크 상태 변경"
      :message="confirmTarget?.active ? `'${confirmTarget?.name}' 도크를 비활성화하시겠습니까?` : `'${confirmTarget?.name}' 도크를 다시 활성화하시겠습니까?`"
      :confirm-label="confirmTarget?.active ? '비활성화' : '활성화'"
      :confirm-color="confirmTarget?.active ? 'error' : 'primary'"
      :loading="confirmLoading"
      @confirm="confirmToggle"
    />
  </AdminLayout>
</template>
