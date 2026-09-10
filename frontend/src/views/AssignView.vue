<script setup>
import { ref, computed } from 'vue'
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
const cancelled = ref(false)

const cancelDialog = ref(false)
const cancelPin = ref('')
const cancelSubmitting = ref(false)
const cancelError = ref('')

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

function dockFeatures(dock) {
  return featureMeta.filter((f) => dock[f.key]).map((f) => f.label)
}

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
  assignError.value = ''
  form.value = { driverName: '', scheduledTime: '', pin: '' }
}

const scheduledTimeLabel = computed(() => {
  if (!form.value.scheduledTime) return '현재 (즉시 배정)'
  const d = new Date(form.value.scheduledTime)
  return d.toLocaleString('ko-KR', { hour: '2-digit', minute: '2-digit', hour12: false })
})

const specLabel = computed(() => {
  if (!selectedDock.value) return ''
  const features = dockFeatures(selectedDock.value)
  const size = sizeLabel[selectedDock.value.size]
  return features.length ? `${size} (${features.join(', ')} 지원)` : size
})

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
    // 배정 도크 상태는 배정 성공 여부와 무관하게 자동으로 갱신되지 않으므로
    // 목록을 다시 불러오는 대신 안내 문구만 보여준다 (기획 결정 사항).
    assignmentResult.value = await apiFetch('/assignments', {
      method: 'POST',
      body: JSON.stringify(body),
    })
  } catch (err) {
    assignError.value = err.message || '배정에 실패했습니다.'
  } finally {
    submitting.value = false
  }
}

function openCancelDialog() {
  cancelPin.value = ''
  cancelError.value = ''
  cancelDialog.value = true
}

async function confirmCancel() {
  cancelSubmitting.value = true
  cancelError.value = ''
  try {
    await apiFetch(`/assignments/${assignmentResult.value.id}/cancel`, {
      method: 'POST',
      body: JSON.stringify({ pin: cancelPin.value }),
    })
    cancelDialog.value = false
    cancelled.value = true
  } catch (err) {
    cancelError.value = err.message || '취소에 실패했습니다.'
  } finally {
    cancelSubmitting.value = false
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

    <!-- 배정 완료: 상세 정보 화면 (SCR-ASSIGN-002) -->
    <div v-else-if="assignmentResult">
      <div class="d-flex align-center justify-space-between mb-1 mt-4">
        <div>
          <h1 class="text-h5 font-weight-bold">{{ warehouse.name }}</h1>
          <p class="text-body-2 text-medium-emphasis">배정 상태 상세 정보</p>
        </div>
        <v-chip variant="outlined" size="small">실시간 모니터링 중</v-chip>
      </div>

      <v-card rounded="lg" variant="flat" border class="mt-6 pa-6 text-center">
        <template v-if="!cancelled">
          <v-avatar color="success" variant="tonal" size="48" class="mb-3">
            <v-icon icon="mdi-check-circle-outline" size="28" />
          </v-avatar>
          <div class="text-h6 font-weight-bold mb-1">도크 배정 정상 완료</div>
          <p class="text-body-2 text-medium-emphasis mb-4">입차 대기 리스트에 등록되었습니다.</p>

          <v-divider class="mb-4" />

          <div class="detail-row">
            <span class="text-medium-emphasis">지정 도크명</span>
            <span class="font-weight-bold">{{ selectedDock.name }}</span>
          </div>
          <div class="detail-row">
            <span class="text-medium-emphasis">도착 예정 시각</span>
            <span class="font-weight-bold">{{ scheduledTimeLabel }}</span>
          </div>
          <div class="detail-row">
            <span class="text-medium-emphasis">담당 기사</span>
            <span class="font-weight-bold">{{ form.driverName }}</span>
          </div>
          <div class="detail-row">
            <span class="text-medium-emphasis">보유 규격</span>
            <span class="font-weight-bold">{{ specLabel }}</span>
          </div>

          <v-alert type="info" variant="tonal" density="compact" class="mt-4 text-left">
            도착 후 입차 검수가 자동 연동됩니다. PIN 번호를 숙지하여 주십시오.
          </v-alert>
        </template>

        <template v-else>
          <v-avatar color="grey-lighten-1" variant="tonal" size="48" class="mb-3">
            <v-icon icon="mdi-close-circle-outline" size="28" />
          </v-avatar>
          <div class="text-h6 font-weight-bold">배정이 취소됐습니다</div>
        </template>
      </v-card>

      <template v-if="!cancelled">
        <v-btn block variant="outlined" color="error" size="large" class="mt-4" @click="openCancelDialog">
          배정 취소
        </v-btn>
        <p class="text-caption text-medium-emphasis text-center mt-2">배정 취소 시 도크 점유가 즉시 상실됩니다.</p>
      </template>

      <v-dialog v-model="cancelDialog" max-width="380">
        <v-card rounded="lg">
          <v-card-text class="pt-6 px-6">
            <div class="text-h6 font-weight-bold mb-1">PIN 확인</div>
            <p class="text-body-2 text-medium-emphasis mb-4">배정을 취소하려면 설정된 4자리 PIN을 입력해 주십시오.</p>
            <v-text-field
              v-model="cancelPin"
              label="PIN"
              inputmode="numeric"
              pattern="[0-9]{4}"
              maxlength="4"
              autofocus
            />
            <v-alert v-if="cancelError" type="error" density="compact" variant="tonal">{{ cancelError }}</v-alert>
          </v-card-text>
          <v-card-actions class="px-6 pb-6">
            <v-btn variant="outlined" block @click="cancelDialog = false">취소</v-btn>
            <v-btn color="error" variant="flat" block :loading="cancelSubmitting" @click="confirmCancel">확인</v-btn>
          </v-card-actions>
        </v-card>
      </v-dialog>
    </div>

    <!-- 배정 전: 도크 목록 + 배정 설정 (SCR-ASSIGN-001) -->
    <div v-else>
      <div class="d-flex align-center justify-space-between mb-1 mt-4">
        <h1 class="text-h5 font-weight-bold">{{ warehouse.name }}</h1>
        <v-chip variant="outlined" size="small">실시간 모니터링 중</v-chip>
      </div>
      <p class="text-body-2 text-medium-emphasis mb-4">실시간 도크 배정 및 가동 상태 모니터링</p>

      <v-table density="comfortable" class="dock-table">
        <thead>
          <tr>
            <th>도크명</th>
            <th>규격</th>
            <th>상태</th>
            <th>보유 특성</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="dock in docks"
            :key="dock.id"
            class="dock-row"
            :class="{ 'dock-row-selected': selectedDock?.id === dock.id, 'dock-row-disabled': dock.status !== 'AVAILABLE' }"
            @click="selectDock(dock)"
          >
            <td class="font-weight-bold">{{ dock.name }}</td>
            <td>{{ sizeLabel[dock.size] }}</td>
            <td>
              <v-chip size="small" variant="tonal" :color="statusMeta[dock.status].color">
                {{ statusMeta[dock.status].label }}
              </v-chip>
            </td>
            <td>
              <div class="d-flex flex-wrap ga-1">
                <v-chip v-for="f in dockFeatures(dock)" :key="f" size="small" variant="outlined">{{ f }}</v-chip>
              </div>
            </td>
          </tr>
        </tbody>
      </v-table>

      <v-card v-if="selectedDock" rounded="lg" variant="flat" border class="mt-6">
        <v-card-text class="pa-5">
          <div class="text-subtitle-1 font-weight-bold">도크 배정 설정</div>
          <p class="text-body-2 text-medium-emphasis mb-4">선택된 도크에 화물 차량 및 담당 기사를 배정합니다.</p>
          <v-divider class="mb-4" />

          <v-form @submit.prevent="submitAssignment">
            <div class="d-flex flex-wrap ga-3">
              <v-text-field v-model="form.driverName" label="기사명" required class="flex-grow-1" style="min-width: 160px" />
              <v-text-field
                v-model="form.scheduledTime"
                label="도착 예정 시각"
                type="datetime-local"
                prepend-inner-icon="mdi-clock-outline"
                class="flex-grow-1"
                style="min-width: 200px"
              />
              <v-text-field
                v-model="form.pin"
                label="배정 PIN (4자리)"
                inputmode="numeric"
                pattern="[0-9]{4}"
                maxlength="4"
                required
                class="flex-grow-1"
                style="min-width: 160px"
              />
            </div>

            <v-alert v-if="assignError" type="warning" variant="tonal" class="mb-4">{{ assignError }}</v-alert>

            <v-btn block color="secondary" size="large" variant="flat" type="submit" :loading="submitting">
              도크 배정 완료
            </v-btn>
          </v-form>
        </v-card-text>
      </v-card>
    </div>
  </div>
</template>

<style scoped>
.page {
  max-width: 640px;
  margin: 0 auto;
  padding: 16px 16px 48px;
}

.dock-table {
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 10px;
}

.dock-row {
  cursor: pointer;
  border-left: 3px solid transparent;
}

.dock-row-selected {
  border-left-color: rgb(var(--v-theme-primary));
  background: rgba(var(--v-theme-primary), 0.04);
}

.dock-row-disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  font-size: 14px;
}
</style>
