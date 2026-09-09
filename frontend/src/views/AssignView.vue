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
const statusLabel = { AVAILABLE: '배정 가능', OCCUPIED: '사용 중', MAINTENANCE: '점검 중' }

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
    <div v-if="loading">불러오는 중...</div>

    <div v-else-if="loadError" class="error-box">
      <p>{{ loadError }}</p>
      <button @click="loadData">다시 시도</button>
    </div>

    <div v-else>
      <h1>{{ warehouse.name }}</h1>

      <ul class="dock-list">
        <li v-for="dock in docks" :key="dock.id">
          <button
            class="dock-card"
            :class="{ selected: selectedDock?.id === dock.id }"
            :disabled="dock.status !== 'AVAILABLE'"
            @click="selectDock(dock)"
          >
            <strong>{{ dock.name }}</strong>
            <span class="badges">
              <span class="badge">{{ sizeLabel[dock.size] }}</span>
              <span class="badge" :class="'status-' + dock.status">{{ statusLabel[dock.status] }}</span>
              <span v-if="dock.hasLeveler" class="badge">레벨러</span>
              <span v-if="dock.hasDockSeal" class="badge">도크씰</span>
              <span v-if="dock.supportsColdChain" class="badge">냉동/냉장</span>
              <span v-if="dock.supportsHazmat" class="badge">위험물</span>
            </span>
          </button>
        </li>
      </ul>

      <form v-if="selectedDock && !assignmentResult" class="assign-form" @submit.prevent="submitAssignment">
        <h2>{{ selectedDock.name }} 배정</h2>

        <label>
          기사 이름
          <input v-model="form.driverName" required />
        </label>

        <label>
          예정 시각 (선택, 미입력 시 현재 시각)
          <input v-model="form.scheduledTime" type="datetime-local" />
        </label>

        <label>
          PIN (4자리 숫자, 취소 시 필요)
          <input v-model="form.pin" inputmode="numeric" pattern="[0-9]{4}" maxlength="4" required />
        </label>

        <p v-if="assignError" class="error-text">{{ assignError }}</p>

        <button type="submit" :disabled="submitting">{{ submitting ? '배정 중...' : '배정하기' }}</button>
      </form>

      <div v-if="assignmentResult" class="success-box">
        <p>배정이 완료됐습니다. (배정 번호 {{ assignmentResult.id }})</p>
        <p>취소가 필요하면 아래 링크에서 PIN으로 취소할 수 있습니다.</p>
        <router-link :to="{ name: 'cancel', params: { id: assignmentResult.id } }">배정 취소하러 가기</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  max-width: 480px;
  margin: 0 auto;
  padding: 20px 16px 48px;
}

.dock-list {
  list-style: none;
  padding: 0;
  margin: 16px 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.dock-card {
  width: 100%;
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: transparent;
  color: var(--text-h);
}

.dock-card.selected {
  border-color: var(--accent);
  background: var(--accent-bg);
}

.badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.badge {
  font-size: 13px;
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid var(--border);
}

.status-AVAILABLE {
  color: var(--success);
  border-color: var(--success);
  background: var(--success-bg);
}

.status-OCCUPIED,
.status-MAINTENANCE {
  color: var(--danger);
  border-color: var(--danger);
  background: var(--danger-bg);
}

.assign-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 20px;
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 10px;
}

.assign-form label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 14px;
}

.assign-form input {
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--bg);
  color: var(--text-h);
}

.assign-form button {
  padding: 12px;
  border: none;
  border-radius: 8px;
  background: var(--accent);
  color: #fff;
  font-weight: 600;
}

.error-box,
.error-text {
  color: var(--danger);
}

.error-box {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: flex-start;
}

.success-box {
  margin-top: 20px;
  padding: 16px;
  border-radius: 10px;
  background: var(--success-bg);
  color: var(--success);
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
