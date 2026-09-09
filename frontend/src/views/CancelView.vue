<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { apiFetch } from '../api/client'

const route = useRoute()
const assignmentId = route.params.id

const pin = ref('')
const submitting = ref(false)
const error = ref('')
const result = ref(null)

async function submitCancel() {
  submitting.value = true
  error.value = ''
  try {
    result.value = await apiFetch(`/assignments/${assignmentId}/cancel`, {
      method: 'POST',
      body: JSON.stringify({ pin: pin.value }),
    })
  } catch (err) {
    error.value = err.message || '취소에 실패했습니다.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <h1>배정 취소</h1>
    <p>배정 번호 {{ assignmentId }}</p>

    <form v-if="!result" class="cancel-form" @submit.prevent="submitCancel">
      <label>
        PIN (4자리 숫자)
        <input v-model="pin" inputmode="numeric" pattern="[0-9]{4}" maxlength="4" required />
      </label>

      <p v-if="error" class="error-text">{{ error }}</p>

      <button type="submit" :disabled="submitting">{{ submitting ? '취소 중...' : '배정 취소' }}</button>
    </form>

    <div v-else class="success-box">
      <p>배정이 취소됐습니다.</p>
    </div>
  </div>
</template>

<style scoped>
.page {
  max-width: 480px;
  margin: 0 auto;
  padding: 20px 16px 48px;
}

.cancel-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 10px;
}

.cancel-form label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 14px;
}

.cancel-form input {
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--bg);
  color: var(--text-h);
}

.cancel-form button {
  padding: 12px;
  border: none;
  border-radius: 8px;
  background: var(--accent);
  color: #fff;
  font-weight: 600;
}

.error-text {
  color: var(--danger);
}

.success-box {
  margin-top: 20px;
  padding: 16px;
  border-radius: 10px;
  background: var(--success-bg);
  color: var(--success);
}
</style>
