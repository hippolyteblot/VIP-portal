<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import AppInput from '@/components/ui/AppInput.vue'
import AppButton from '@/components/ui/AppButton.vue'
import { useAuthStore } from '@/stores/auth.store'
import type { RegisterPayload } from '@/types/auth.types'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  firstName: '',
  lastName: '',
  email: '',
  password: '',
  passwordConfirm: '',
  countryCode: '',
  institution: '',
  comments: '',
  acceptTerms: false,
})

const passwordMismatch = computed(() =>
  form.password && form.passwordConfirm && form.password !== form.passwordConfirm,
)

async function onSubmit() {
  if (passwordMismatch.value) {
    console.log('Passwords do not match')
    return
  }
  if (!form.acceptTerms) {
    console.log('Terms must be accepted')
    return
  }

  const payload: RegisterPayload = {
    firstName: form.firstName,
    lastName: form.lastName,
    email: form.email,
    password: form.password,
    countryCode: form.countryCode,
    institution: form.institution,
    comments: form.comments,
  }

  try {
    await authStore.register(payload)
    console.log('Account created successfully')
    router.push('/login')
  } catch {
    console.log('Error during registration')
  }
}
</script>

<template>
  <div class="space-y-8">
    <div class="lg:hidden flex justify-center">
      <div class="h-10 w-10 rounded-lg bg-primary-600 text-white font-bold flex items-center justify-center">
        VIP
      </div>
    </div>

    <div>
      <h1 class="text-2xl font-bold text-gray-900">
        Créer un compte
      </h1>
      <p class="mt-1 text-sm text-gray-500">
        Rejoignez la communauté VIP
      </p>
    </div>

    <form class="space-y-4" @submit.prevent="onSubmit">
      <div class="grid grid-cols-2 gap-4">
        <AppInput
          v-model="form.firstName"
          label="Prénom"
          placeholder="Jean"
          required
        />
        <AppInput
          v-model="form.lastName"
          label="Nom"
          placeholder="Dupont"
          required
        />
      </div>

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

      <AppInput
        v-model="form.passwordConfirm"
        label="Confirmer le mot de passe"
        type="password"
        placeholder="••••••••"
        :error="passwordMismatch ? 'Les mots de passe ne correspondent pas' : undefined"
        required
      />

      <AppInput
        v-model="form.countryCode"
        label="Code pays"
        placeholder="fr"
        required
      />

      <AppInput
        v-model="form.institution"
        label="Institution"
        placeholder="Université Paris-Saclay"
        required
      />

      <div class="space-y-1">
        <label class="block text-sm font-medium text-gray-700">
          Commentaires pour l'équipe VIP
        </label>
        <textarea
          v-model="form.comments"
          rows="4"
          placeholder="Décrivez votre projet de recherche..."
          class="block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-0 focus:border-primary-500 transition-colors duration-150"
        />
      </div>

      <label class="flex items-start gap-3 cursor-pointer">
        <input
          v-model="form.acceptTerms"
          type="checkbox"
          class="mt-1 h-4 w-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
        />
        <span class="text-sm text-gray-600">
          J'accepte les conditions d'utilisation
        </span>
      </label>

      <AppButton
        type="submit"
        variant="primary"
        :loading="authStore.isLoading"
      >
        Créer mon compte
      </AppButton>
    </form>

    <p class="text-center text-sm text-gray-600">
      Déjà un compte ?
      <RouterLink to="/login" class="font-medium text-primary-600 hover:text-primary-700">
        Se connecter
      </RouterLink>
    </p>
  </div>
</template>
