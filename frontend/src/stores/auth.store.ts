import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { sessionApi } from '@/api/session.api'
import type { VipSession, LoginCredentials, User } from '@/types/auth.types'



export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const session = ref<VipSession | null>(null)
  const isLoading = ref(false)
  const initialized = ref(false)

  const isAuthenticated = computed(() => !!session.value)

  /**
   * Appelé une seule fois au démarrage de l'app (avant la première navigation).
   * Tente de restaurer la session via GET /internal/session.
   * Les cookies HttpOnly posés par le backend sont envoyés automatiquement.
   */
  async function initialize() {
    if (initialized.value) return

    try {
      const vipSession = await sessionApi.getSession()
      session.value = vipSession
      buildUserFromSession(vipSession)
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
      session.value = vipSession;
      buildUserFromSession(vipSession);
    } catch (error) {
      isLoading.value = false;
      throw error; // Rejette l'erreur pour qu'elle soit gérée dans le composant
    }
    isLoading.value = false;
  }

  /**
   * Construit le profil utilisateur à partir de la session.
   * Les données complémentaires viennent du mock car
   * l'API GET /users/me n'existe pas encore côté backend.
   */
  function buildUserFromSession(vipSession: VipSession) {
    user.value = {
      email: vipSession.email
    }
  }

  async function logout() {
    try {
      await sessionApi.logout()
    } catch {
      // Si le DELETE échoue (CSRF, réseau…), on ne peut pas effacer
      // les cookies HttpOnly côté client. Mais on nettoie le state Pinia.
    }
    user.value = null
    session.value = null
    initialized.value = false
  }

  async function register(_payload: unknown) {
    await new Promise((resolve) => setTimeout(resolve, 500))
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
    logout,
  }
})
