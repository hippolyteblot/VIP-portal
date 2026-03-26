<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import AppInput from '@/components/ui/AppInput.vue'
import AppButton from '@/components/ui/AppButton.vue'
import { useAuthStore } from '@/stores/auth.store'
import { useNotificationsStore } from '@/stores/notifications.store'
import axios from 'axios'

const router = useRouter()
const authStore = useAuthStore()
const notificationsStore = useNotificationsStore()

const form = reactive({
  email: '',
  password: '',
})

async function onSubmit() {
  try {
    await authStore.login({ username: form.email, password: form.password })
    notificationsStore.success('You have been logged in successfully.')
    console.log('Connected')
    router.push('/dashboard')
  } catch (error: unknown) {
    let errorMessage = 'Error during login'

    if (axios.isAxiosError(error)) {
      if (error.response) {
        console.error('Login error response:', error.response)
        switch (error.response.status) {
          case 401:
            errorMessage = 'Invalid email or password.'
            break
          case 403:
            errorMessage = 'Your account is not activated yet.'
            break
          default:
            errorMessage = `Login failed with status ${error.response.status}.`
            break
        }
      }
    }
    notificationsStore.error(errorMessage)
  }
}
</script>

<template>
  <div class="space-y-8">
    <div class="lg:hidden flex justify-center">
      <img
        src="@/assets/vip-logo-without-text.png"
        alt="VIP Logo"
        class="h-24 w-auto rounded-sm object-cover"
      />
    </div>

    <div>
      <h1 class="text-2xl font-bold text-gray-900">Connexion</h1>
      <p class="mt-1 text-sm text-gray-500">Connectez-vous à votre compte VIP</p>
    </div>

    <form class="space-y-5" @submit.prevent="onSubmit">
      <AppInput
        v-model="form.email"
        label="Email"
        type="email"
        placeholder="vous@exemple.com"
        required
      />
      <AppInput
        v-model="form.password"
        label="Mot de passe"
        type="password"
        placeholder="••••••••"
        required
      />
      <AppButton type="submit" variant="primary" :loading="authStore.isLoading">
        Se connecter
      </AppButton>
    </form>

    <p class="text-center text-sm text-gray-600">
      Pas encore de compte ?
      <RouterLink to="/register" class="font-medium text-primary-600 hover:text-primary-700">
        Créer un compte
      </RouterLink>
    </p>
  </div>
</template>
