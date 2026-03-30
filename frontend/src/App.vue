<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AuthLayout from '@/components/layout/AuthLayout.vue'
import MainLayout from '@/components/layout/MainLayout.vue'
import AppToast from '@/components/ui/AppToast.vue'

const route = useRoute()
const isLandingRoute = computed(() => ['landing', 'home'].includes(route.name as string))
</script>

<template>
  
  <AppToast />

  <template v-if="isLandingRoute">
    <RouterView />
  </template>
  <component v-else :is="route.path === '/login' || route.path === '/register' ? AuthLayout : MainLayout">
    <RouterView />
  </component>
</template>
