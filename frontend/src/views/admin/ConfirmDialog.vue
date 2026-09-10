<script setup>
defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '확인' },
  message: { type: String, default: '' },
  confirmLabel: { type: String, default: '확인' },
  confirmColor: { type: String, default: 'error' },
  loading: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'confirm'])
</script>

<template>
  <v-dialog :model-value="modelValue" max-width="420" @update:model-value="emit('update:modelValue', $event)">
    <v-card rounded="lg">
      <v-card-text class="d-flex flex-column align-center text-center pt-8 pb-2">
        <v-avatar color="error" variant="tonal" size="48" class="mb-4">
          <v-icon icon="mdi-alert-circle-outline" size="28" />
        </v-avatar>
        <div class="text-h6 mb-2">{{ title }}</div>
        <div class="text-body-2 text-medium-emphasis">{{ message }}</div>
      </v-card-text>
      <v-card-actions class="pb-6 px-6">
        <v-spacer />
        <v-btn variant="outlined" @click="emit('update:modelValue', false)">취소</v-btn>
        <v-btn :color="confirmColor" variant="flat" :loading="loading" @click="emit('confirm')">{{ confirmLabel }}</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
