<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiFetch } from '../../api/client'
import { setToken } from '../../api/adminClient'

const router = useRouter()
const username = ref('')
const password = ref('')
const showPassword = ref(false)
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
  <div class="login-page">
    <v-card class="login-card pa-4" rounded="lg" elevation="4" max-width="380" width="100%">
      <v-card-text class="d-flex flex-column align-center pt-6 pb-2">
        <v-avatar size="56" rounded="lg" class="mb-4">
          <v-img src="/favicon.png" alt="콕배정" />
        </v-avatar>
        <img src="../../assets/logo.png" alt="콕배정" class="login-logo" />
        <div class="text-h6 font-weight-bold mt-4">콕배정 관리자 로그인</div>
        <div class="text-body-2 text-medium-emphasis mt-1">물류 시스템 도크 제어 포탈</div>
      </v-card-text>

      <v-form class="px-2" @submit.prevent="submit">
        <v-card-text class="d-flex flex-column ga-2">
          <v-text-field v-model="username" label="아이디" prepend-inner-icon="mdi-account-outline" required />
          <v-text-field
            v-model="password"
            label="비밀번호"
            :type="showPassword ? 'text' : 'password'"
            prepend-inner-icon="mdi-lock-outline"
            :append-inner-icon="showPassword ? 'mdi-eye-off-outline' : 'mdi-eye-outline'"
            required
            @click:append-inner="showPassword = !showPassword"
          />
          <v-alert v-if="error" type="error" density="compact" variant="tonal">{{ error }}</v-alert>
        </v-card-text>
        <v-card-actions class="px-4 pb-6">
          <v-btn block color="primary" size="large" variant="flat" type="submit" :loading="submitting">
            로그인
          </v-btn>
        </v-card-actions>
      </v-form>
    </v-card>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f4f7fb;
  padding: 16px;
}

.login-logo {
  height: 28px;
  object-fit: contain;
}
</style>
