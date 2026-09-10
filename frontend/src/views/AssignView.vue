<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { apiFetch } from '../api/client'

const route = useRoute()
const warehouseId = route.params.warehouseId

const loading = ref(true)
const loadError = ref('')
const warehouse = ref(null)
const docks = ref([])

const selectedDock = ref(null)
const form = ref({ driverName: '', scheduledTime: '', pin: '' })
const submitting = ref(false)
const assignError = ref('')
const assignmentResult = ref(null)

const sizeLabel = { LARGE: '대형', MEDIUM: '중형', SMALL: '소형' }
const statusMeta = {
  AVAILABLE: { label: '배정 가능', color: 'success' },
  OCCUPIED: { label: '사용 중', color: 'grey-darken-1' },
  MAINTENANCE: { label: '점검 중', color: 'error' },
}
const featureMeta = [
  { key: 'hasLeveler', label: '레벨러' },
  { key: 'hasDockSeal', label: '도크씰' },
  { key: 'supportsColdChain', label: '냉동/냉장' },
  { key: 'supportsHazmat', label: '위험물' },
]

async function loadData() {
  loading.value = true
  loadError.value = ''
  try {
    const [warehouseRes, docksRes] = await Promise.all([
      apiFetch(`/warehouses/${warehouseId}`),
      apiFetch(`/warehouses/${warehouseId}/docks`),
    ])
    warehouse.value = warehouseRes
    docks.value = docksRes
  } catch (err) {
    loadError.value = err.message || '창고 정보를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

function selectDock(dock) {
  if (dock.status !== 'AVAILABLE') return
  selectedDock.value = dock
  assignmentResult.value = null
  assignError.value = ''
  form.value = { driverName: '', scheduledTime: '', pin: '' }
}

async function submitAssignment() {
  submitting.value = true
  assignError.value = ''
  try {
    const body = {
      dockId: selectedDock.value.id,
      driverName: form.value.driverName,
      pin: form.value.pin,
    }
    if (form.value.scheduledTime) {
      body.scheduledTime = form.value.scheduledTime
    }
    assignmentResult.value = await apiFetch('/assignments', {
      method: 'POST',
      body: JSON.stringify(body),
    })
  } catch (err) {
    // 배정 도크 상태는 배정 성공 여부와 무관하게 자동으로 갱신되지 않으므로
    // 목록을 다시 불러오는 대신 안내 문구만 보여준다 (기획 결정 사항).
    assignError.value = err.message || '배정에 실패했습니다.'
  } finally {
    submitting.value = false
  }
}

loadData()
</script>

<template>
  <div class="page">
    <div v-if="loading" class="d-flex align-center ga-2 text-medium-emphasis pt-8">
      <v-progress-circular size="20" width="2" indeterminate />
      불러오는 중...
    </div>

    <v-alert v-else-if="loadError" type="error" variant="tonal" class="mt-8">
      {{ loadError }}
      <template #append>
        <v-btn size="small" variant="text" @click="loadData">다시 시도</v-btn>
      </template>
    </v-alert>

    <div v-else>
      <div class="d-flex align-center justify-space-between mb-1 mt-4">
        <h1 class="text-h5 font-weight-bold">{{ warehouse.name }}</h1>
        <v-chip color="success" size="small" variant="tonal" prepend-icon="mdi-circle-medium">
          실시간 모니터링 중
        </v-chip>
      </div>
      <p class="text-body-2 text-medium-emphasis mb-4">실시간 도크 배정 및 가동 상태 모니터링</p>

      <div class="d-flex flex-column ga-3">
        <v-card
          v-for="dock in docks"
          :key="dock.id"
          rounded="lg"
          variant="outlined"
          :class="{ 'dock-card-selected': selectedDock?.id === dock.id }"
          :disabled="dock.status !== 'AVAILABLE'"
          @click="selectDock(dock)"
        >
          <v-card-text class="d-flex flex-column ga-2">
            <span class="font-weight-bold">{{ dock.name }}</span>
            <div class="d-flex flex-wrap ga-1">
              <v-chip size="small" variant="outlined">{{ sizeLabel[dock.size] }}</v-chip>
              <v-chip size="small" variant="tonal" :color="statusMeta[dock.status].color">
                {{ statusMeta[dock.status].label }}
              </v-chip>
              <v-chip v-for="f in featureMeta.filter((f) => dock[f.key])" :key="f.key" size="small" variant="outlined">
                {{ f.label }}
              </v-chip>
            </div>
          </v-card-text>
        </v-card>
      </div>

      <v-card v-if="selectedDock && !assignmentResult" rounded="lg" variant="flat" border class="mt-6">
        <v-card-title class="pt-5 px-5">{{ selectedDock.name }} 배정</v-card-title>
        <v-form @submit.prevent="submitAssignment">
          <v-card-text class="px-5 d-flex flex-column ga-2">
            <v-text-field v-model="form.driverName" label="기사 이름" required />
            <v-text-field
              v-model="form.scheduledTime"
              label="도착 예정 시각 (선택, 미입력 시 현재 시각)"
              type="datetime-local"
              prepend-inner-icon="mdi-clock-outline"
            />
            <v-text-field
              v-model="form.pin"
              label="배정 PIN (4자리)"
              inputmode="numeric"
              pattern="[0-9]{4}"
              maxlength="4"
              required
            />
            <v-alert v-if="assignError" type="error" density="compact" variant="tonal">{{ assignError }}</v-alert>
          </v-card-text>
          <v-card-actions class="px-5 pb-5">
            <v-btn block color="primary" size="large" variant="flat" type="submit" :loading="submitting">
              도크 배정 완료
            </v-btn>
          </v-card-actions>
        </v-form>
      </v-card>

      <v-card v-if="assignmentResult" rounded="lg" variant="flat" class="mt-6 pa-5 text-center success-box">
        <v-avatar color="success" variant="tonal" size="48" class="mb-3">
          <v-icon icon="mdi-check-circle-outline" size="28" />
        </v-avatar>
        <div class="text-h6 font-weight-bold mb-1">도크 배정 정상 완료</div>
        <p class="text-body-2 mb-4">배정 번호 {{ assignmentResult.id }}</p>
        <v-btn
          variant="outlined"
          color="error"
          block
          :to="{ name: 'cancel', params: { id: assignmentResult.id } }"
        >
          배정 취소하러 가기
        </v-btn>
      </v-card>
    </div>
  </div>
</template>

<style scoped>
.page {
  max-width: 480px;
  margin: 0 auto;
  padding: 16px 16px 48px;
}

.dock-card-selected {
  border-color: rgb(var(--v-theme-primary)) !important;
  background: rgba(var(--v-theme-primary), 0.06);
}

.success-box {
  background: rgba(var(--v-theme-success), 0.08) !important;
}
</style>
