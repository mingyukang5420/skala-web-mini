<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { adminFetch } from '../../api/adminClient'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)

const name = ref('')
const error = ref('')
const submitting = ref(false)

async function loadExisting() {
  if (!isEdit.value) return
  const warehouse = await adminFetch(`/admin/warehouses/${route.params.id}`)
  name.value = warehouse.name
}

async function submit() {
  submitting.value = true
  error.value = ''
  try {
    if (isEdit.value) {
      await adminFetch(`/admin/warehouses/${route.params.id}`, {
        method: 'PUT',
        body: JSON.stringify({ name: name.value }),
      })
    } else {
      await adminFetch('/admin/warehouses', {
        method: 'POST',
        body: JSON.stringify({ name: name.value }),
      })
    }
    router.push({ name: 'admin-warehouses' })
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
    <h1>{{ isEdit ? '창고 수정' : '창고 등록' }}</h1>
    <form @submit.prevent="submit">
      <label>
        창고명
        <input v-model="name" required />
      </label>
      <p v-if="error" class="error-text">{{ error }}</p>
      <button type="submit" :disabled="submitting">저장</button>
    </form>
  </div>
</template>
