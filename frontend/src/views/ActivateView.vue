<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute, RouterLink } from 'vue-router'
import AppInput from '@/components/ui/AppInput.vue'
import AppButton from '@/components/ui/AppButton.vue'
import { useAuthStore } from '@/stores/auth.store'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const code = ref('')
const errorMessage = ref('')
const email = computed(() => route.query.email as string | undefined || '')

async function onSubmit() {
  if (!code.value) return
  errorMessage.value = ''
  try {
    await authStore.activate(email.value, code.value)
    // If successfully activated and session exists
    router.push('/dashboard')
  } catch (e: any) {
    console.error('Error during activation', e)
    errorMessage.value = "Le code de vérification est invalide ou a expiré. Veuillez réessayer."
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
      <h1 class="text-2xl font-bold text-gray-900">
        Vérification de l'email
      </h1>
      <p class="mt-2 text-sm text-gray-500">
        Un email avec un code de vérification a été envoyé à <strong>{{ email }}</strong>.
        Veuillez entrer ce code ci-dessous pour activer votre compte.
      </p>
    </div>

    <form class="space-y-6" @submit.prevent="onSubmit">
      <AppInput
        v-model="code"
        label="Code de vérification"
        placeholder="Ex: 123456"
        required
      />

      <div v-if="errorMessage" class="text-sm text-red-600 bg-red-50 p-3 rounded-md border border-red-200">
        {{ errorMessage }}
      </div>

      <AppButton
        type="submit"
        variant="primary"
        class="w-full"
        :loading="authStore.isLoading"
      >
        Valider et se connecter
      </AppButton>
    </form>

    <p class="text-center text-sm text-gray-600">
      <RouterLink to="/login" class="font-medium text-primary-600 hover:text-primary-700">
        Retour à la page de connexion
      </RouterLink>
    </p>
  </div>
</template>
