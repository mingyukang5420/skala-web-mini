<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
const warehouseId = route.params.warehouseId
</script>

<template>
  <div>
    <h1>도크 배정</h1>
    <p>warehouseId: {{ warehouseId }}</p>
  </div>
</template>
