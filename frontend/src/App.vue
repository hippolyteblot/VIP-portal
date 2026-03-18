<script setup lang="ts">
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import AuthLayout from '@/components/layout/AuthLayout.vue'
import MainLayout from '@/components/layout/MainLayout.vue'
import AppToast from '@/components/ui/AppToast.vue'

const route = useRoute()
const auth = useAuthStore()
</script>

<template>
  
  <AppToast />

  <component :is="route.path === '/login' || route.path === '/register' ? AuthLayout : MainLayout">
    <RouterView />
  </component>
</template>
