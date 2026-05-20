<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus, Trash2 } from 'lucide-vue-next'
import AppCard from '@/components/ui/AppCard.vue'
import AppInput from '@/components/ui/AppInput.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppBadge from '@/components/ui/AppBadge.vue'
import { useMessagesStore } from '@/stores/messages.store'
import { useFormatters } from '@/composables/useFormatters'
import type { SendMessagePayload } from '@/types/message.types'

const messagesStore = useMessagesStore()
const { formatRelativeTime } = useFormatters()

const showComposeModal = ref(false)
const expandedId = ref<string | null>(null)
const sendingMessage = ref(false)

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
        Nouveau message
      </AppButton>
    </div>

    <div class="space-y-2">
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

    <p
      v-if="!messagesStore.isLoading && messagesStore.sortedMessages.length === 0"
      class="py-12 text-center text-gray-500"
    >
      Aucun message
    </p>

    <div
      v-if="showComposeModal"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4"
      @click.self="showComposeModal = false"
    >
      <div class="w-full max-w-2xl rounded-xl border border-gray-200 bg-white p-6 shadow-lg">
        <div class="flex items-center justify-between gap-4">
          <h2 class="text-lg font-semibold text-gray-900">Nouveau message</h2>
          <button
            type="button"
            class="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
            @click="showComposeModal = false"
            aria-label="Fermer"
          >
            ×
          </button>
        </div>
        <form class="mt-4 space-y-4" @submit.prevent="sendMessage">
          <AppInput
            v-model="newMessage.to"
            label="Destinataire"
            required
          />
          <AppInput
            v-model="newMessage.subject"
            label="Sujet"
            required
          />
          <div>
            <label class="mb-1 block text-sm font-medium text-gray-700">
              Corps
            </label>
            <textarea
              v-model="newMessage.body"
              rows="5"
              class="block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm placeholder:text-gray-400 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-0 disabled:bg-gray-50 disabled:text-gray-500"
              placeholder="Votre message..."
            />
          </div>
          <label class="flex items-center gap-2">
            <input
              v-model="newMessage.isGroupMessage"
              type="checkbox"
              class="h-4 w-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
            />
            <span class="text-sm text-gray-700">Message de groupe</span>
          </label>
          <div class="flex justify-end gap-2 pt-2">
            <AppButton type="button" variant="secondary" @click="showComposeModal = false">
              Annuler
            </AppButton>
            <AppButton type="submit" :loading="sendingMessage">
              Envoyer
            </AppButton>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
