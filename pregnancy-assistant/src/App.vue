<script setup lang="ts">
import { onMounted } from 'vue'
import AppShell from './components/AppShell.vue'
import { usePregnancyStore } from './stores/pregnancy'

const store = usePregnancyStore()

onMounted(() => {
  void store.hydrate()
})
</script>

<template>
  <AppShell />
</template>
