<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { adminFetch } from '../../api/adminClient'
import AdminLayout from './AdminLayout.vue'
import WarehouseFormDialog from './WarehouseFormDialog.vue'
import ConfirmDialog from './ConfirmDialog.vue'

const router = useRouter()

const warehouses = ref([])
const loading = ref(true)
const error = ref('')

const headers = [
  { title: '식별 코드', key: 'code', sortable: false },
  { title: '창고명', key: 'name' },
  { title: '등록일자', key: 'createdAt' },
  { title: '상태', key: 'active', sortable: false },
  { title: '관리 작업', key: 'actions', sortable: false, align: 'end' },
]

const formOpen = ref(false)
const editingId = ref(null)
const confirmOpen = ref(false)
const confirmTarget = ref(null)
const confirmLoading = ref(false)

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

function code(warehouse) {
  return `WH-${String(warehouse.id).padStart(3, '0')}`
}

function formatDate(value) {
  if (!value) return '-'
  return value.slice(0, 10)
}

function openCreate() {
  editingId.value = null
  formOpen.value = true
}

function openEdit(warehouse) {
  editingId.value = warehouse.id
  formOpen.value = true
}

function goDocks(warehouse) {
  router.push({ name: 'admin-docks-overview', query: { warehouseId: warehouse.id } })
}

function goSummary(warehouse) {
  router.push({ name: 'admin-summary', query: { warehouseId: warehouse.id } })
}

function askToggle(warehouse) {
  confirmTarget.value = warehouse
  confirmOpen.value = true
}

async function confirmToggle() {
  const warehouse = confirmTarget.value
  if (!warehouse) return
  confirmLoading.value = true
  const action = warehouse.active ? 'deactivate' : 'activate'
  try {
    await adminFetch(`/admin/warehouses/${warehouse.id}/${action}`, { method: 'POST' })
    confirmOpen.value = false
    await load()
  } catch (err) {
    error.value = err.message || '처리에 실패했습니다.'
  } finally {
    confirmLoading.value = false
  }
}

onMounted(load)
</script>

<template>
  <AdminLayout>
    <div class="d-flex justify-space-between align-start mb-6">
      <div>
        <h1 class="text-h5 font-weight-bold">창고 관리</h1>
        <p class="text-body-2 text-medium-emphasis mt-1">등록된 물류센터를 관리합니다</p>
      </div>
      <v-btn color="primary" variant="flat" prepend-icon="mdi-plus" @click="openCreate">창고 등록</v-btn>
    </div>

    <v-alert v-if="error" type="error" density="compact" variant="tonal" class="mb-4">{{ error }}</v-alert>

    <v-card rounded="lg" variant="flat" border>
      <v-data-table :headers="headers" :items="warehouses" :loading="loading" item-value="id" no-data-text="등록된 창고가 없습니다.">
        <template #item.code="{ item }">
          <span class="text-primary font-weight-medium">{{ code(item) }}</span>
        </template>
        <template #item.name="{ item }">
          <span class="font-weight-medium">{{ item.name }}</span>
        </template>
        <template #item.createdAt="{ item }">
          {{ formatDate(item.createdAt) }}
        </template>
        <template #item.active="{ item }">
          <v-chip :color="item.active ? 'success' : 'error'" size="small" variant="tonal">
            {{ item.active ? '활성' : '비활성' }}
          </v-chip>
        </template>
        <template #item.actions="{ item }">
          <div class="d-flex justify-end ga-2">
            <v-btn size="small" variant="outlined" @click="goDocks(item)">도크 관리</v-btn>
            <v-btn size="small" variant="outlined" @click="goSummary(item)">혼잡도 요약</v-btn>
            <v-btn size="small" variant="outlined" @click="openEdit(item)">수정</v-btn>
            <v-btn size="small" variant="outlined" color="error" @click="askToggle(item)">
              {{ item.active ? '비활성화' : '활성화' }}
            </v-btn>
          </div>
        </template>
      </v-data-table>
    </v-card>

    <WarehouseFormDialog v-model="formOpen" :warehouse-id="editingId" @saved="load" />

    <ConfirmDialog
      v-model="confirmOpen"
      title="창고 상태 변경"
      :message="confirmTarget?.active ? `'${confirmTarget?.name}' 창고를 비활성화하시겠습니까?` : `'${confirmTarget?.name}' 창고를 다시 활성화하시겠습니까?`"
      :confirm-label="confirmTarget?.active ? '비활성화' : '활성화'"
      :confirm-color="confirmTarget?.active ? 'error' : 'primary'"
      :loading="confirmLoading"
      @confirm="confirmToggle"
    />
  </AdminLayout>
</template>
