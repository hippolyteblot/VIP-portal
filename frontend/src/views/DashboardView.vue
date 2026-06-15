<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { RouterLink } from 'vue-router'
import { Rocket, Folder, Play, Mail } from 'lucide-vue-next'
import AppCard from '@/components/ui/AppCard.vue'
import AppBadge from '@/components/ui/AppBadge.vue'
import { useAuthStore } from '@/stores/auth.store'
import { useMessagesStore } from '@/stores/messages.store'
import { useFormatters } from '@/composables/useFormatters'
import { getRecentApplications, type RecentApplication } from '@/utils/recentApplications'

const auth = useAuthStore()
const messagesStore = useMessagesStore()
const { formatRelativeTime } = useFormatters()

const displayName = computed(() => {
  const { firstName, lastName, email } = auth.user ?? {}
  if (firstName && lastName) return `${firstName} ${lastName}`
  return email ?? ''
})

const recentApplications = getRecentApplications(4)

const unreadMessages = computed(() =>
  messagesStore.sortedMessages.filter((m) => !m.read),
)

const showWelcome = computed(() =>
  auth.isAuthenticated && auth.user != null && auth.user.welcomeDismissed == null,
)

const quickActions = [
  {
    label: 'Launch an application',
    description: 'Start a new execution from available applications',
    route: { name: 'applications' },
    icon: Rocket,
  },
  {
    label: 'Manage files',
    description: 'Browse and manage your data files',
    route: { name: 'files' },
    icon: Folder,
  },
  {
    label: 'My executions',
    description: 'View and manage your workflow executions',
    route: { name: 'executions' },
    icon: Play,
  },
  {
    label: 'Messages',
    description: 'Read your mailbox and send messages',
    route: { name: 'messages' },
    icon: Mail,
  },
]

onMounted(() => {
  const thirtyDaysAgo = Date.now() - 30 * 24 * 60 * 60 * 1000
  messagesStore.fetchMessages(thirtyDaysAgo)
})
</script>

<template>
  <div class="space-y-8">
    <div>
      <h1 class="text-2xl font-bold">
        Welcome, {{ displayName }}
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

    <div class="grid gap-6 lg:grid-cols-3">
      <section class="space-y-3 lg:col-span-2">
        <h2 class="text-lg font-semibold text-gray-900">Notifications</h2>
        <AppCard padding>
          <div v-if="messagesStore.isLoading" class="flex justify-center py-8 text-sm text-gray-500">
            Loading notifications...
          </div>
          <div v-else-if="unreadMessages.length === 0" class="py-8 text-center text-sm text-gray-500">
            No new notifications.
          </div>
          <div v-else class="-mx-4 divide-y divide-gray-100">
            <RouterLink
              v-for="msg in unreadMessages"
              :key="msg.id"
              :to="{ name: 'messages' }"
              class="flex items-start gap-3 px-4 py-3 transition hover:bg-gray-50"
            >
              <div class="min-w-0 flex-1">
                <div class="flex items-center justify-between gap-2">
                  <p class="truncate text-sm font-medium text-gray-900">
                    {{ msg.subject }}
                  </p>
                  <span class="shrink-0 text-xs text-gray-400">
                    {{ formatRelativeTime(msg.date) }}
                  </span>
                </div>
                <p class="mt-0.5 truncate text-xs text-gray-500">
                  From: {{ msg.from }}
                </p>
                <p v-if="msg.body" class="mt-0.5 line-clamp-2 text-xs text-gray-400">
                  {{ msg.body }}
                </p>
              </div>
            </RouterLink>
          </div>
        </AppCard>
      </section>

      <section class="space-y-3">
        <h2 class="text-lg font-semibold text-gray-900">Quick actions</h2>
        <div class="space-y-3">
          <AppCard
            v-for="action in quickActions"
            :key="action.label"
            hoverable
            padding
            class="transition hover:border-primary-200"
          >
            <RouterLink :to="action.route" class="flex items-start gap-3">
              <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-primary-50 text-primary-600">
                <component :is="action.icon" class="h-5 w-5" />
              </div>
              <div class="min-w-0">
                <p class="text-sm font-medium text-gray-900">{{ action.label }}</p>
                <p class="mt-0.5 text-xs text-gray-500">{{ action.description }}</p>
              </div>
            </RouterLink>
          </AppCard>
        </div>
      </section>
    </div>
  </div>
</template>
