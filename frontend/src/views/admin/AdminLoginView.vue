<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiFetch } from '../../api/client'
import { setToken } from '../../api/adminClient'

const router = useRouter()
const username = ref('')
const password = ref('')
const error = ref('')
const submitting = ref(false)

async function submit() {
  submitting.value = true
  error.value = ''
  try {
    const res = await apiFetch('/admin/login', {
      method: 'POST',
      body: JSON.stringify({ username: username.value, password: password.value }),
    })
    setToken(res.accessToken)
    router.push({ name: 'admin-warehouses' })
  } catch (err) {
    error.value = err.message || '로그인에 실패했습니다.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <h1>관리자 로그인</h1>
    <form @submit.prevent="submit">
      <label>
        아이디
        <input v-model="username" required />
      </label>
      <label>
        비밀번호
        <input v-model="password" type="password" required />
      </label>
      <p v-if="error" class="error-text">{{ error }}</p>
      <button type="submit" :disabled="submitting">{{ submitting ? '로그인 중...' : '로그인' }}</button>
    </form>
  </div>
</template>
