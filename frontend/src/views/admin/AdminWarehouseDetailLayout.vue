<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { adminFetch } from '../../api/adminClient'

const props = defineProps({
  warehouseId: { type: [Number, String], required: true },
})

const route = useRoute()
const router = useRouter()
const warehouse = ref(null)

async function load() {
  warehouse.value = await adminFetch(`/admin/warehouses/${props.warehouseId}`)
}

watch(() => props.warehouseId, load, { immediate: true })

const tabs = [
  { name: 'admin-warehouse-docks', label: '도크 관리' },
  { name: 'admin-warehouse-summary', label: '혼잡도 요약' },
]

function goTab(name) {
  router.push({ name, params: { id: props.warehouseId } })
}
</script>

<template>
  <div>
    <router-link :to="{ name: 'admin-warehouses' }" class="back-link">
      <v-icon icon="mdi-chevron-left" size="18" />
      창고 목록
    </router-link>

    <h1 class="text-h5 font-weight-bold mt-1 mb-4">{{ warehouse?.name || ' ' }}</h1>

    <v-tabs :model-value="route.name" color="primary" class="mb-6">
      <v-tab v-for="tab in tabs" :key="tab.name" :value="tab.name" @click="goTab(tab.name)">
        {{ tab.label }}
      </v-tab>
    </v-tabs>

    <slot :warehouse="warehouse" />
  </div>
</template>

<style scoped>
.back-link {
  display: inline-flex;
  align-items: center;
  color: rgba(0, 0, 0, 0.6);
  text-decoration: none;
  font-size: 14px;
}

.back-link:hover {
  color: rgb(var(--v-theme-primary));
}
</style>
