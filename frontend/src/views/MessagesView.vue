<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { Plus, Trash2 } from 'lucide-vue-next'
import AppCard from '@/components/ui/AppCard.vue'
import AppInput from '@/components/ui/AppInput.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppBadge from '@/components/ui/AppBadge.vue'
import { usersApi } from '@/api/users.api'
import { useMessagesStore } from '@/stores/messages.store'
import { useFormatters } from '@/composables/useFormatters'
import type { SendMessagePayload } from '@/types/message.types'
import type { UserSuggestion } from '@/types/user.types'

const messagesStore = useMessagesStore()
const { formatRelativeTime } = useFormatters()

const showComposeModal = ref(false)
const expandedId = ref<string | null>(null)
const sendingMessage = ref(false)
const activeTab = ref<'received' | 'sent'>('received')
const userSuggestions = ref<UserSuggestion[]>([])
const isSearchingUsers = ref(false)
const activeQuery = ref('')
const searchTimer = ref<number | null>(null)

const MIN_QUERY_LENGTH = 2

const newMessage = ref<SendMessagePayload & { to: string }>({
  to: '',
  subject: '',
  body: '',
  isGroupMessage: false,
})

function resetForm() {
  newMessage.value = {
    to: '',
    subject: '',
    body: '',
    isGroupMessage: false,
  }
  userSuggestions.value = []
  activeQuery.value = ''
}

async function sendMessage() {
  if (!newMessage.value.to || !newMessage.value.subject) return
  sendingMessage.value = true
  try {
    await messagesStore.sendMessage({
      to: newMessage.value.to,
      subject: newMessage.value.subject,
      body: newMessage.value.body,
      isGroupMessage: newMessage.value.isGroupMessage,
    })
    showComposeModal.value = false
    resetForm()
  } finally {
    sendingMessage.value = false
  }
}

function openComposeModal() {
  resetForm()
  showComposeModal.value = true
}

function extractQuery(value: string): string {
  const segments = value.split(/[;,]/)
  return (segments[segments.length - 1] || '').trim()
}

function applySuggestion(email: string) {
  const segments = newMessage.value.to.split(/[;,]/).map((segment) => segment.trim())
  const nextSegments = segments.slice(0, -1).filter((segment) => segment.length > 0)
  nextSegments.push(email)
  newMessage.value.to = `${nextSegments.join(', ')}, `
  userSuggestions.value = []
  activeQuery.value = ''
}

function formatSuggestionLabel(user: UserSuggestion): string {
  const fullName = [user.firstName, user.lastName].filter(Boolean).join(' ')
  return fullName ? `${fullName} <${user.email}>` : user.email
}

async function onMessageClick(id: string) {
  expandedId.value = expandedId.value === id ? null : id
  const msg = messagesStore.messages.find((m) => m.id === id)
  if (msg && !msg.read) {
    await messagesStore.markAsRead(id)
  }
}

onMounted(() => {
  messagesStore.fetchMessages()
})

watch(activeTab, (tab) => {
  if (tab === 'sent' && messagesStore.sentMessages.length === 0) {
    messagesStore.fetchSentMessages()
  }
})

watch(
  () => newMessage.value.to,
  (value) => {
    const query = extractQuery(value)
    activeQuery.value = query

    if (searchTimer.value !== null) {
      window.clearTimeout(searchTimer.value)
    }

    if (!query || query.length < MIN_QUERY_LENGTH) {
      userSuggestions.value = []
      isSearchingUsers.value = false
      return
    }

    searchTimer.value = window.setTimeout(async () => {
      isSearchingUsers.value = true
      try {
        const results = await usersApi.search(query, 10)
        if (activeQuery.value === query) {
          userSuggestions.value = results
        }
      } finally {
        if (activeQuery.value === query) {
          isSearchingUsers.value = false
        }
      }
    }, 250)
  },
)
</script>

<template>
  <div class="space-y-6">
    <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <h1 class="flex items-center gap-2 text-2xl font-bold text-gray-900">
        Messages
        <AppBadge v-if="messagesStore.unreadCount" variant="primary">
          {{ messagesStore.unreadCount }}
        </AppBadge>
      </h1>
      <AppButton @click="openComposeModal">
        <Plus class="h-4 w-4" />
        New message
      </AppButton>
    </div>

    <div class="flex flex-wrap gap-2">
      <AppButton
        type="button"
        :variant="activeTab === 'received' ? 'primary' : 'secondary'"
        @click="activeTab = 'received'"
      >
        Inbox
      </AppButton>
      <AppButton
        type="button"
        :variant="activeTab === 'sent' ? 'primary' : 'secondary'"
        @click="activeTab = 'sent'"
      >
        Sent
      </AppButton>
    </div>

    <div v-if="activeTab === 'received'" class="space-y-2">
      <AppCard
        v-for="msg in messagesStore.sortedMessages"
        :key="msg.id"
        padding
        hoverable
        class="cursor-pointer"
        @click="onMessageClick(msg.id)"
      >
        <div class="flex items-start gap-3">
          <span
            v-if="!msg.read"
            class="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-primary-600"
          />
          <span v-else class="w-2 shrink-0" />
          <div class="min-w-0 flex-1">
            <div class="flex flex-wrap items-start justify-between gap-2">
              <p
                :class="[
                  'font-medium',
                  msg.read ? 'text-gray-700' : 'font-bold text-gray-900',
                ]"
              >
                {{ msg.subject }}
              </p>
              <AppButton
                variant="ghost"
                size="sm"
                @click.stop="messagesStore.deleteMessage(msg.id)"
              >
                <Trash2 class="h-4 w-4 text-red-600" />
              </AppButton>
            </div>
            <p class="text-sm text-gray-500">
              {{ msg.from }} · {{ formatRelativeTime(msg.date) }}
            </p>
            <p
              v-if="expandedId === msg.id"
              class="mt-2 whitespace-pre-wrap text-sm text-gray-600"
            >
              {{ msg.body }}
            </p>
            <p
              v-else
              class="mt-1 line-clamp-2 text-sm text-gray-500"
            >
              {{ msg.body.slice(0, 120) }}{{ msg.body.length > 120 ? '...' : '' }}
            </p>
          </div>
        </div>
      </AppCard>
    </div>

    <div v-else class="space-y-2">
      <AppCard
        v-for="msg in messagesStore.sortedSentMessages"
        :key="msg.id"
        padding
        hoverable
        class="cursor-pointer"
        @click="expandedId = expandedId === msg.id ? null : msg.id"
      >
        <div class="flex items-start gap-3">
          <span class="w-2 shrink-0" />
          <div class="min-w-0 flex-1">
            <div class="flex flex-wrap items-start justify-between gap-2">
              <p class="font-medium text-gray-900">
                {{ msg.subject }}
              </p>
              <AppButton
                variant="ghost"
                size="sm"
                @click.stop="messagesStore.deleteSentMessage(msg.id)"
              >
                <Trash2 class="h-4 w-4 text-red-600" />
              </AppButton>
            </div>
            <p class="text-sm text-gray-500">
              To {{ msg.to.join(', ') || '-' }} · {{ formatRelativeTime(msg.date) }}
            </p>
            <p
              v-if="expandedId === msg.id"
              class="mt-2 whitespace-pre-wrap text-sm text-gray-600"
            >
              {{ msg.body }}
            </p>
            <p
              v-else
              class="mt-1 line-clamp-2 text-sm text-gray-500"
            >
              {{ msg.body.slice(0, 120) }}{{ msg.body.length > 120 ? '...' : '' }}
            </p>
          </div>
        </div>
      </AppCard>
    </div>

    <p
      v-if="activeTab === 'received' && !messagesStore.isLoading && messagesStore.sortedMessages.length === 0"
      class="py-12 text-center text-gray-500"
    >
      No messages
    </p>
    <p
      v-if="activeTab === 'sent' && !messagesStore.isLoadingSent && messagesStore.sortedSentMessages.length === 0"
      class="py-12 text-center text-gray-500"
    >
      No sent messages
    </p>

    <div
      v-if="showComposeModal"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4"
      @click.self="showComposeModal = false"
    >
      <div class="w-full max-w-2xl rounded-xl border border-gray-200 bg-white p-6 shadow-lg">
        <div class="flex items-center justify-between gap-4">
          <h2 class="text-lg font-semibold text-gray-900">New message</h2>
          <button
            type="button"
            class="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
            @click="showComposeModal = false"
            aria-label="Close"
          >
            ×
          </button>
        </div>
        <form class="mt-4 space-y-4" @submit.prevent="sendMessage">
          <AppInput
            v-model="newMessage.to"
            label="Recipients"
            required
          />
          <div v-if="activeQuery && (isSearchingUsers || userSuggestions.length > 0)" class="rounded-lg border border-gray-200 bg-gray-50 p-3">
            <p class="text-xs font-semibold uppercase tracking-wide text-gray-500">
              Suggestions
            </p>
            <div class="mt-2 space-y-1">
              <p v-if="isSearchingUsers" class="text-sm text-gray-500">Searching...</p>
              <button
                v-for="user in userSuggestions"
                :key="user.id"
                type="button"
                class="flex w-full items-center justify-between rounded-md px-2 py-1 text-left text-sm text-gray-700 hover:bg-white"
                @click="applySuggestion(user.email)"
              >
                <span>{{ formatSuggestionLabel(user) }}</span>
              </button>
              <p
                v-if="!isSearchingUsers && userSuggestions.length === 0"
                class="text-sm text-gray-500"
              >
                No suggestions
              </p>
            </div>
          </div>
          <AppInput
            v-model="newMessage.subject"
            label="Subject"
            required
          />
          <div>
            <label class="mb-1 block text-sm font-medium text-gray-700">
              Body
            </label>
            <textarea
              v-model="newMessage.body"
              rows="5"
              class="block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm placeholder:text-gray-400 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-0 disabled:bg-gray-50 disabled:text-gray-500"
              placeholder="Your message..."
            />
          </div>
          <label class="flex items-center gap-2">
            <input
              v-model="newMessage.isGroupMessage"
              type="checkbox"
              class="h-4 w-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
            />
            <span class="text-sm text-gray-700">Group message</span>
          </label>
          <div class="flex justify-end gap-2 pt-2">
            <AppButton type="button" variant="secondary" @click="showComposeModal = false">
              Cancel
            </AppButton>
            <AppButton type="submit" :loading="sendingMessage">
              Send
            </AppButton>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
