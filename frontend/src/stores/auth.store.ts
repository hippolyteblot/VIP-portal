import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { sessionApi } from '@/api/session.api'
import { usersApi } from '@/api/users.api'
import type { VipSession, LoginCredentials, RegisterPayload, User } from '@/types/auth.types'



export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const session = ref<VipSession | null>(null)
  const isLoading = ref(false)
  const initialized = ref(false)

  const isAuthenticated = computed(() => !!session.value)

  /**
   * Called on app startup to check if there's an existing session. If so, it populates the `session` and `user` state.
   */
  async function initialize() {
    if (initialized.value) return

    try {
      const vipSession = await sessionApi.getSession()
      if (vipSession.confirmed === false) {
        session.value = null
        user.value = null
      } else {
        session.value = vipSession
        buildUserFromSession(vipSession)
      }
    } catch {
      session.value = null
      user.value = null
    } finally {
      initialized.value = true
    }
  }

  async function login(credentials: LoginCredentials) {
    isLoading.value = true;
    try {
      const vipSession = await sessionApi.login(credentials);
      if (vipSession.confirmed === false) {
        return vipSession;
      }
      session.value = vipSession;
      buildUserFromSession(vipSession);
      return vipSession;
    } catch (error) {
      throw error; // Throw to be handled by the caller (such as LoginView)
    } finally {
      isLoading.value = false;
    }
  }

  function buildUserFromSession(vipSession: VipSession) {
    user.value = {
      email: vipSession.email
    }
  }

  async function logout() {
    try {
      await sessionApi.logout()
    } catch {
      // if logout fails, we still want to clear the local session and user state to ensure the app behaves as logged out
    }
    user.value = null
    session.value = null
    initialized.value = false
  }

  async function register(payload: RegisterPayload) {
    isLoading.value = true
    try {
      await usersApi.register(payload)
    } finally {
      isLoading.value = false
    }
  }

  async function activate(email: string, code: string) {
    isLoading.value = true
    try {
      await usersApi.activate(email, code)
      // Assuming activation logs the user in (sets cookies), check session:
      await sessionApi.getSession().then((s) => {
        session.value = s
        buildUserFromSession(s)
      }).catch(() => {})
    } finally {
      isLoading.value = false
    }
  }

  return {
    user,
    session,
    isLoading,
    initialized,
    isAuthenticated,
    initialize,
    login,
    register,
    activate,
    logout,
  }
})
