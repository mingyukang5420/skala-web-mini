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
    <v-card rounded="lg" variant="flat" border class="mt-8 pa-2">
      <v-card-text v-if="!result" class="d-flex flex-column ga-1 text-center">
        <div class="text-h6 font-weight-bold">PIN 확인</div>
        <p class="text-body-2 text-medium-emphasis mb-2">배정 번호 {{ assignmentId }}의 배정을 취소합니다</p>

        <v-form @submit.prevent="submitCancel">
          <v-text-field
            v-model="pin"
            label="PIN (4자리 숫자)"
            inputmode="numeric"
            pattern="[0-9]{4}"
            maxlength="4"
            required
            class="mt-2"
          />
          <v-alert v-if="error" type="error" density="compact" variant="tonal" class="mb-2">{{ error }}</v-alert>
          <v-btn block color="error" variant="flat" size="large" type="submit" :loading="submitting">
            배정 취소
          </v-btn>
        </v-form>
      </v-card-text>

      <v-card-text v-else class="d-flex flex-column align-center text-center py-6">
        <v-avatar color="success" variant="tonal" size="48" class="mb-3">
          <v-icon icon="mdi-check-circle-outline" size="28" />
        </v-avatar>
        <div class="text-h6 font-weight-bold">배정이 취소됐습니다</div>
      </v-card-text>
    </v-card>
  </div>
</template>

<style scoped>
.page {
  max-width: 420px;
  margin: 0 auto;
  padding: 16px 16px 48px;
}
</style>
