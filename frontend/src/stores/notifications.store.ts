import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { Notification, DashboardNotification } from '@/types/notification.types'

export const useNotificationsStore = defineStore('notifications', () => {
  const toasts = ref<Notification[]>([])
  const dashboardNotifications = ref<DashboardNotification[]>([])

  function addToast(toast: Omit<Notification, 'id'>) {
    const id = 'toast-' + Date.now()
    toasts.value.push({ ...toast, id })
    const duration = toast.duration ?? 5000
    setTimeout(() => removeToast(id), duration)
  }

  function removeToast(id: string) {
    toasts.value = toasts.value.filter((t) => t.id !== id)
  }

  function success(message: string, title = 'Success') {
    addToast({ type: 'success', title, message })
  }

  function error(message: string, title = 'Error') {
    addToast({ type: 'error', title, message })
  }

  function warning(message: string, title = 'Warning') {
    addToast({ type: 'warning', title, message })
  }

  function info(message: string, title = 'Information') {
    addToast({ type: 'info', title, message })
  }

  async function fetchDashboardNotifications() {
    dashboardNotifications.value = []
  }

  function markAsRead(id: string) {
    const notif = dashboardNotifications.value.find((n) => n.id === id)
    if (notif) notif.read = true
  }

  return {
    toasts,
    dashboardNotifications,
    addToast,
    removeToast,
    success,
    error,
    warning,
    info,
    fetchDashboardNotifications,
    markAsRead,
  }
})
