<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { RouterLink } from 'vue-router'
import AppCard from '@/components/ui/AppCard.vue'
import AppBadge from '@/components/ui/AppBadge.vue'
import { useAuthStore } from '@/stores/auth.store'
import { getRecentApplications, type RecentApplication } from '@/utils/recentApplications'

const auth = useAuthStore()

const recentApplications = ref<RecentApplication[]>([])

const showWelcome = computed(() =>
  auth.isAuthenticated && auth.user != null && auth.user.welcomeDismissed == null
)

onMounted(() => {
    recentApplications.value = getRecentApplications(4)
})
</script>


<template>

    <div class="space-y-8">
        <div>
            <h1 class="text-2xl font-bold">
                Welcome, {{ auth.user?.email }}
            </h1>
            <p>Welcome to your VIP Portal dashboard!</p>
    </div>

        <section class="space-y-3">
            <h2 class="text-lg font-semibold text-gray-900">
                Recently used applications
            </h2>

            <p v-if="recentApplications.length === 0" class="text-sm text-gray-500">
                No recent application usage yet. Launch an application to see it here.
            </p>

            <div v-else class="grid grid-cols-1 gap-4 md:grid-cols-4">
                <AppCard
                    v-for="app in recentApplications"
                    :key="app.name"
                    hoverable
                >
                    <RouterLink
                        :to="{
                            name: 'application-launch',
                            params: { name: app.name, version: app.lastVersion || undefined },
                        }"
                        class="block"
                    >
                        <div class="flex items-center justify-between gap-2">
                            <h3 class="text-base font-semibold text-gray-900">
                                {{ app.fullName || app.name }}
                            </h3>
                            <AppBadge v-if="app.lastVersion" variant="gray">
                                {{ app.lastVersion }}
                            </AppBadge>
                        </div>
                        <p class="mt-2 text-xs text-gray-500">
                            Last used: {{ new Date(app.usedAt).toLocaleString() }}
                        </p>
                    </RouterLink>
                </AppCard>
            </div>
        </section>
    </div>
</template>