<script setup lang="ts">
import { useAuthStore } from '@/stores/auth.store'
const auth = useAuthStore()
</script>


<template>
    <div class="space-y-8">
        <h1 class="text-2xl font-bold">Welcome, {{ auth.user?.email }}</h1>
        <p>Welcome to your VIP Portal dashboard!</p>
    </div>
</template>