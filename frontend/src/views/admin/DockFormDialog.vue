<script setup>
import { ref, watch } from 'vue'
import { adminFetch } from '../../api/adminClient'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  dockId: { type: [Number, String], default: null },
  warehouseId: { type: [Number, String], default: null },
})

const emit = defineEmits(['update:modelValue', 'saved'])

const sizeOptions = [
  { title: '대형', value: 'LARGE' },
  { title: '중형', value: 'MEDIUM' },
  { title: '소형', value: 'SMALL' },
]

const statusOptions = [
  { title: '배정 가능', value: 'AVAILABLE' },
  { title: '사용 중', value: 'OCCUPIED' },
  { title: '점검 중', value: 'MAINTENANCE' },
]

const defaultForm = () => ({
  name: '',
  size: 'LARGE',
  status: 'AVAILABLE',
  hasLeveler: false,
  hasDockSeal: false,
  supportsColdChain: false,
  supportsHazmat: false,
})

const form = ref(defaultForm())
const error = ref('')
const submitting = ref(false)
const loadingExisting = ref(false)

const isEdit = () => props.dockId != null

async function loadExisting() {
  form.value = defaultForm()
  error.value = ''
  if (!isEdit()) return
  loadingExisting.value = true
  try {
    const dock = await adminFetch(`/admin/docks/${props.dockId}`)
    form.value = {
      name: dock.name,
      size: dock.size,
      status: dock.status,
      hasLeveler: dock.hasLeveler,
      hasDockSeal: dock.hasDockSeal,
      supportsColdChain: dock.supportsColdChain,
      supportsHazmat: dock.supportsHazmat,
    }
  } catch (err) {
    error.value = err.message || '정보를 불러오지 못했습니다.'
  } finally {
    loadingExisting.value = false
  }
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) loadExisting()
  },
)

async function submit() {
  submitting.value = true
  error.value = ''
  try {
    if (isEdit()) {
      await adminFetch(`/admin/docks/${props.dockId}`, {
        method: 'PUT',
        body: JSON.stringify(form.value),
      })
    } else {
      await adminFetch('/admin/docks', {
        method: 'POST',
        body: JSON.stringify({ warehouseId: Number(props.warehouseId), ...form.value }),
      })
    }
    emit('saved')
    emit('update:modelValue', false)
  } catch (err) {
    error.value = err.message || '저장에 실패했습니다.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <v-dialog :model-value="modelValue" max-width="480" @update:model-value="emit('update:modelValue', $event)">
    <v-card rounded="lg">
      <v-card-title class="pt-6 px-6">{{ isEdit() ? '도크 수정' : '도크 등록' }}</v-card-title>
      <v-form @submit.prevent="submit">
        <v-card-text class="px-6 d-flex flex-column ga-2">
          <v-text-field
            v-model="form.name"
            label="도크명"
            required
            :disabled="loadingExisting"
            :loading="loadingExisting"
          />
          <v-select v-model="form.size" :items="sizeOptions" label="규격" :disabled="loadingExisting" />
          <v-select
            v-model="form.status"
            :items="statusOptions"
            label="가동 상태"
            :disabled="loadingExisting"
            hint="실제 배정 여부와 별개로 관리자가 직접 설정하는 값입니다. 실시간 배정 현황은 혼잡도 요약에서 확인하세요."
            persistent-hint
          />

          <div class="text-caption text-medium-emphasis mt-1 mb-n1">보유 특성</div>
          <div class="d-flex flex-wrap ga-4">
            <v-checkbox v-model="form.hasLeveler" label="도크 레벨러" density="compact" hide-details :disabled="loadingExisting" />
            <v-checkbox v-model="form.hasDockSeal" label="도크씰" density="compact" hide-details :disabled="loadingExisting" />
            <v-checkbox v-model="form.supportsColdChain" label="냉동/냉장 대응" density="compact" hide-details :disabled="loadingExisting" />
            <v-checkbox v-model="form.supportsHazmat" label="위험물 취급 가능" density="compact" hide-details :disabled="loadingExisting" />
          </div>

          <v-alert v-if="error" type="error" density="compact" variant="tonal" class="mt-2">{{ error }}</v-alert>
        </v-card-text>
        <v-card-actions class="pb-6 px-6">
          <v-spacer />
          <v-btn variant="outlined" @click="emit('update:modelValue', false)">취소</v-btn>
          <v-btn color="primary" variant="flat" type="submit" :loading="submitting" :disabled="loadingExisting">
            저장
          </v-btn>
        </v-card-actions>
      </v-form>
    </v-card>
  </v-dialog>
</template>
