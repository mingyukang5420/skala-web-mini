<script setup>
import { ref, watch } from 'vue'
import { adminFetch } from '../../api/adminClient'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  warehouseId: { type: [Number, String], default: null },
})

const emit = defineEmits(['update:modelValue', 'saved'])

const name = ref('')
const error = ref('')
const submitting = ref(false)
const loadingExisting = ref(false)

const isEdit = () => props.warehouseId != null

async function loadExisting() {
  name.value = ''
  error.value = ''
  if (!isEdit()) return
  loadingExisting.value = true
  try {
    const warehouse = await adminFetch(`/admin/warehouses/${props.warehouseId}`)
    name.value = warehouse.name
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
      await adminFetch(`/admin/warehouses/${props.warehouseId}`, {
        method: 'PUT',
        body: JSON.stringify({ name: name.value }),
      })
    } else {
      await adminFetch('/admin/warehouses', {
        method: 'POST',
        body: JSON.stringify({ name: name.value }),
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
  <v-dialog :model-value="modelValue" max-width="440" @update:model-value="emit('update:modelValue', $event)">
    <v-card rounded="lg">
      <v-card-title class="pt-6 px-6">{{ isEdit() ? '창고 수정' : '창고 등록' }}</v-card-title>
      <v-form @submit.prevent="submit">
        <v-card-text class="px-6">
          <v-text-field
            v-model="name"
            label="창고명"
            required
            :disabled="loadingExisting"
            :loading="loadingExisting"
          />
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
