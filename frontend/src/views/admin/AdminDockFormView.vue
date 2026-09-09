<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { adminFetch } from '../../api/adminClient'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => route.name === 'admin-dock-edit')
const warehouseId = ref(isEdit.value ? null : route.params.id)

const form = ref({
  name: '',
  size: 'LARGE',
  status: 'AVAILABLE',
  hasLeveler: false,
  hasDockSeal: false,
  supportsColdChain: false,
  supportsHazmat: false,
})
const error = ref('')
const submitting = ref(false)

async function loadExisting() {
  if (!isEdit.value) return
  const dock = await adminFetch(`/admin/docks/${route.params.id}`)
  warehouseId.value = dock.warehouseId
  form.value = {
    name: dock.name,
    size: dock.size,
    status: dock.status,
    hasLeveler: dock.hasLeveler,
    hasDockSeal: dock.hasDockSeal,
    supportsColdChain: dock.supportsColdChain,
    supportsHazmat: dock.supportsHazmat,
  }
}

async function submit() {
  submitting.value = true
  error.value = ''
  try {
    if (isEdit.value) {
      await adminFetch(`/admin/docks/${route.params.id}`, {
        method: 'PUT',
        body: JSON.stringify(form.value),
      })
    } else {
      await adminFetch('/admin/docks', {
        method: 'POST',
        body: JSON.stringify({ warehouseId: Number(warehouseId.value), ...form.value }),
      })
    }
    router.push({ name: 'admin-docks', params: { id: warehouseId.value } })
  } catch (err) {
    error.value = err.message || '저장에 실패했습니다.'
  } finally {
    submitting.value = false
  }
}

onMounted(loadExisting)
</script>

<template>
  <div class="page">
    <h1>{{ isEdit ? '도크 수정' : '도크 등록' }}</h1>
    <form @submit.prevent="submit">
      <label>
        도크명
        <input v-model="form.name" required />
      </label>
      <label>
        규격
        <select v-model="form.size">
          <option value="LARGE">대형</option>
          <option value="MEDIUM">중형</option>
          <option value="SMALL">소형</option>
        </select>
      </label>
      <label>
        상태
        <select v-model="form.status">
          <option value="AVAILABLE">배정 가능</option>
          <option value="OCCUPIED">사용 중</option>
          <option value="MAINTENANCE">점검 중</option>
        </select>
      </label>
      <label><input v-model="form.hasLeveler" type="checkbox" /> 레벨러</label>
      <label><input v-model="form.hasDockSeal" type="checkbox" /> 도크씰</label>
      <label><input v-model="form.supportsColdChain" type="checkbox" /> 냉동/냉장</label>
      <label><input v-model="form.supportsHazmat" type="checkbox" /> 위험물</label>
      <p v-if="error" class="error-text">{{ error }}</p>
      <button type="submit" :disabled="submitting">저장</button>
    </form>
  </div>
</template>
