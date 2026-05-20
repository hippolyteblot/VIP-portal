import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { messagesApi, type BackendMessage } from '@/api/messages.api'
import { useNotificationsStore } from '@/stores/notifications.store'
import type { MessageItem, SendMessagePayload } from '@/types/message.types'

function toDisplayName(user?: { email?: string; firstName?: string; lastName?: string } | null): string {
  if (!user) return 'Unknown'
  const fullName = [user.firstName, user.lastName].filter(Boolean).join(' ')
  return fullName || user.email || 'Unknown'
}

function toDateString(input?: string | number): string {
  if (typeof input === 'number') {
    return new Date(input).toISOString()
  }
  if (typeof input === 'string' && input) {
    return input
  }
  return new Date().toISOString()
}

function mapMessage(message: BackendMessage): MessageItem {
  return {
    id: String(message.id),
    subject: message.title || '(No subject)',
    body: message.message || '',
    from: toDisplayName(message.sender),
    to: (message.receivers || []).map((receiver) => receiver.email || ''),
    date: toDateString(message.postedDate || message.posted),
    read: Boolean(message.read),
  }
}

function parseRecipients(input: string): string[] {
  return input
    .split(/[;,]/g)
    .map((value) => value.trim())
    .filter(Boolean)
}

export const useMessagesStore = defineStore('messages', () => {
  const messages = ref<MessageItem[]>([])
  const isLoading = ref(false)
  const notifications = useNotificationsStore()

  const sortedMessages = computed(() =>
    [...messages.value].sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime()),
  )

  const unreadCount = computed(() => messages.value.filter((message) => !message.read).length)

  async function fetchMessages(startDate?: number) {
    isLoading.value = true
    try {
      const data = await messagesApi.getReceived(startDate)
      messages.value = data.map(mapMessage)
    } catch {
      notifications.error('Impossible de charger les messages.')
    } finally {
      isLoading.value = false
    }
  }

  async function fetchSentMessages(startDate?: number) {
    return messagesApi.getSent(startDate).then((data) => data.map(mapMessage))
  }

  async function sendMessage(payload: SendMessagePayload) {
    const recipients = parseRecipients(payload.to)
    if (recipients.length === 0) {
      notifications.error('Veuillez renseigner au moins un destinataire.')
      return
    }

    try {
      await messagesApi.send({
        recipients,
        subject: payload.subject,
        message: payload.body,
      })
      notifications.success('Message envoyé.')
    } catch {
      notifications.error('Impossible d\'envoyer le message.')
    }
  }

  async function deleteMessage(id: string) {
    const numericId = Number(id)
    if (Number.isNaN(numericId)) return

    await messagesApi.deleteReceived(numericId)
    messages.value = messages.value.filter((message) => message.id !== id)
  }

  function markAsRead(id: string) {
    const message = messages.value.find((item) => item.id === id)
    if (message) {
      message.read = true
    }
  }

  return {
    messages,
    sortedMessages,
    unreadCount,
    isLoading,
    fetchMessages,
    fetchSentMessages,
    sendMessage,
    deleteMessage,
    markAsRead,
  }
})
