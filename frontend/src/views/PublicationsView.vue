<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ExternalLink, Pencil, Plus, Search, Trash2 } from 'lucide-vue-next'
import { applicationsApi } from '@/api/applications.api'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppCard from '@/components/ui/AppCard.vue'
import { useAuthStore } from '@/stores/auth.store'
import { useNotificationsStore } from '@/stores/notifications.store'
import { usePublicationsStore } from '@/stores/publications.store'
import type { Publication, PublicationInput } from '@/types/publication.types'

const authStore = useAuthStore()
const notificationsStore = useNotificationsStore()
const publicationsStore = usePublicationsStore()

const isSaving = ref(false)
const isModalOpen = ref(false)
const isEditing = ref(false)
const editingId = ref<number | null>(null)
const appOptions = ref<string[]>([])
const isLoadingApplications = ref(false)

const form = reactive<PublicationInput>({
  title: '',
  date: '',
  doi: '',
  authors: '',
  type: '',
  typeName: '',
  vipApplication: '',
})

const isAdmin = computed(() => authStore.session?.userlevel === 'Administrator')
const hasApplicationOptions = computed(() => appOptions.value.length > 0)

function badgeVariant(type: string | null | undefined): 'primary' | 'info' | 'success' | 'warning' | 'gray' {
  const normalized = (type ?? '').toLowerCase()
  if (normalized.includes('journal') || normalized.includes('article')) {
    return 'primary'
  }
  if (normalized.includes('conference') || normalized.includes('proceeding')) {
    return 'info'
  }
  if (normalized.includes('software') || normalized.includes('tool')) {
    return 'success'
  }
  if (normalized.includes('book') || normalized.includes('chapter')) {
    return 'warning'
  }
  return 'gray'
}

function formatDateShort(value: string | null | undefined): string {
  const date = (value ?? '').trim()
  if (!date) {
    return 'No date'
  }

  const parsedDate = new Date(date)
  if (Number.isNaN(parsedDate.getTime())) {
    return date
  }

  return parsedDate.toLocaleDateString('en-GB', { year: 'numeric', month: 'short', day: '2-digit' })
}

function normalizeOptional(value: string | null): string | null {
  const trimmed = (value ?? '').trim()
  return trimmed.length ? trimmed : null
}

function buildPayload(): PublicationInput {
  return {
    title: form.title.trim(),
    authors: form.authors.trim(),
    date: normalizeOptional(form.date),
    doi: normalizeOptional(form.doi),
    type: normalizeOptional(form.type),
    typeName: normalizeOptional(form.typeName),
    vipApplication: normalizeOptional(form.vipApplication),
  }
}

function resetForm() {
  editingId.value = null
  isEditing.value = false
  form.title = ''
  form.date = ''
  form.doi = ''
  form.authors = ''
  form.type = ''
  form.typeName = ''
  form.vipApplication = ''
}

function openCreateModal() {
  resetForm()
  isModalOpen.value = true
}

function closeModal() {
  isModalOpen.value = false
  resetForm()
}

function openEditModal(publication: Publication) {
  if (!isAdmin.value) {
    return
  }

  isModalOpen.value = true
  isEditing.value = true
  editingId.value = publication.id
  form.title = publication.title || ''
  form.date = publication.date || ''
  form.doi = publication.doi || ''
  form.authors = publication.authors || ''
  form.type = publication.type || ''
  form.typeName = publication.typeName || ''
  form.vipApplication = publication.vipApplication || ''
}

async function submit() {
  if (!form.title?.trim() || !form.authors?.trim()) {
    notificationsStore.warning('Title and authors are required.')
    return
  }

  isSaving.value = true
  try {
    const payload = buildPayload()

    if (isEditing.value && editingId.value !== null) {
      if (!isAdmin.value) {
        notificationsStore.error('Only administrators can edit publications.')
        return
      }
      await publicationsStore.updatePublication(editingId.value, payload)
      notificationsStore.success('Publication updated.')
    } else {
      await publicationsStore.createPublication(payload)
      notificationsStore.success('Publication created.')
    }
    closeModal()
  } catch {
    notificationsStore.error('Unable to save publication.')
  } finally {
    isSaving.value = false
  }
}

async function removePublication(id: number) {
  if (!isAdmin.value) {
    notificationsStore.error('Only administrators can delete publications.')
    return
  }

  if (!window.confirm('Delete this publication?')) {
    return
  }

  try {
    await publicationsStore.deletePublication(id)
    notificationsStore.success('Publication deleted.')
    if (editingId.value === id) {
      closeModal()
    }
  } catch {
    notificationsStore.error('Unable to delete publication.')
  }
}

async function fetchApplicationOptions() {
  isLoadingApplications.value = true
  try {
    const page = await applicationsApi.getAll(0, 50)
    appOptions.value = page.data.map((app) => app.name).sort((a, b) => a.localeCompare(b))
  } catch {
    notificationsStore.error('Unable to load applications list for publication form.')
  } finally {
    isLoadingApplications.value = false
  }
}

onMounted(async () => {
  await fetchApplicationOptions()

  try {
    await publicationsStore.fetchPublications()
  } catch {
    notificationsStore.error('Unable to load publications.')
  }
})
</script>

<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-gray-900">Publications</h1>
      <p class="mt-1 text-sm text-gray-500">
        Browse the publication catalog and add new entries. Edit and delete actions are reserved to administrators.
      </p>
      <AppButton class="mt-3" @click="openCreateModal">
        <Plus class="h-4 w-4" />
        Add publication
      </AppButton>
    </div>

    <div class="relative">
      <Search class="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400" />
      <input
        v-model="publicationsStore.searchQuery"
        type="search"
        placeholder="Search publications by title, authors, type, app..."
        class="block w-full rounded-lg border border-gray-300 py-2.5 pl-10 pr-4 text-sm placeholder:text-gray-400 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-0"
      />
    </div>

    <div v-if="publicationsStore.isLoading" class="flex justify-center py-16 text-sm text-gray-500">
      Loading publications...
    </div>

    <div v-else-if="publicationsStore.filteredPublications.length === 0" class="py-12 text-center text-gray-500">
      No publications found.
    </div>

    <div v-else class="space-y-4">
      <AppCard
        v-for="pub in publicationsStore.filteredPublications"
        :key="pub.id"
        padding
        class="flex flex-col gap-3"
      >
        <div class="flex flex-wrap items-start justify-between gap-2">
          <h3 class="text-lg font-bold text-gray-900">
            {{ pub.title }}
          </h3>
          <AppBadge :variant="badgeVariant(pub.type)">
            {{ pub.typeName || pub.type || 'Publication' }}
          </AppBadge>
        </div>

        <p class="text-sm text-gray-600">
          {{ pub.authors }}
        </p>

        <div class="flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-gray-500">
          <span>{{ formatDateShort(pub.date) }}</span>
          <span v-if="pub.vipApplication">{{ pub.vipApplication }}</span>
          <a
            v-if="pub.doi"
            :href="`https://doi.org/${pub.doi}`"
            target="_blank"
            rel="noopener noreferrer"
            class="inline-flex items-center gap-1 text-primary-600 hover:text-primary-700"
          >
            <ExternalLink class="h-3.5 w-3.5" />
            DOI
          </a>
        </div>

        <div v-if="isAdmin" class="flex items-center gap-2 pt-1">
          <AppButton variant="secondary" size="sm" @click="openEditModal(pub)">
            <Pencil class="h-3.5 w-3.5" />
            Edit
          </AppButton>
          <AppButton variant="danger" size="sm" @click="removePublication(pub.id)">
            <Trash2 class="h-3.5 w-3.5" />
            Delete
          </AppButton>
        </div>
      </AppCard>
    </div>

    <div
      v-if="isModalOpen"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
      @click.self="closeModal"
    >
      <AppCard class="w-full max-w-2xl" padding>
        <div class="flex items-start justify-between gap-3">
          <div>
            <h2 class="text-lg font-semibold text-gray-900">
              {{ isEditing ? 'Edit publication' : 'Add publication' }}
            </h2>
            <p class="mt-1 text-sm text-gray-500">
              Fill in the publication metadata. Fields marked with * are required.
            </p>
          </div>
          <button type="button" class="text-sm text-gray-500 hover:text-gray-700" @click="closeModal">Close</button>
        </div>

        <div class="mt-4 grid grid-cols-1 gap-3 md:grid-cols-2">
          <input v-model="form.title" type="text" placeholder="Title *" class="rounded-lg border border-gray-300 px-3 py-2 text-sm" />
          <input v-model="form.authors" type="text" placeholder="Authors *" class="rounded-lg border border-gray-300 px-3 py-2 text-sm" />
          <input v-model="form.date" type="text" placeholder="Date" class="rounded-lg border border-gray-300 px-3 py-2 text-sm" />
          <input v-model="form.doi" type="text" placeholder="DOI" class="rounded-lg border border-gray-300 px-3 py-2 text-sm" />
          <input v-model="form.type" type="text" placeholder="Type" class="rounded-lg border border-gray-300 px-3 py-2 text-sm" />
          <input v-model="form.typeName" type="text" placeholder="Type name" class="rounded-lg border border-gray-300 px-3 py-2 text-sm" />
          <select
            v-model="form.vipApplication"
            class="rounded-lg border border-gray-300 px-3 py-2 text-sm md:col-span-2"
            :disabled="isLoadingApplications"
          >
            <option value="">No linked application</option>
            <option v-for="appName in appOptions" :key="appName" :value="appName">{{ appName }}</option>
          </select>
        </div>

        <p v-if="!isLoadingApplications && !hasApplicationOptions" class="mt-2 text-xs text-amber-700">
          No application available. Leave this field empty or create an application first.
        </p>

        <div class="mt-5 flex flex-wrap justify-end gap-2">
          <AppButton variant="secondary" @click="closeModal">
            Cancel
          </AppButton>
          <AppButton
            :loading="isSaving"
            :disabled="isEditing && !isAdmin"
            @click="submit"
          >
            {{ isEditing ? 'Update publication' : 'Create publication' }}
          </AppButton>
        </div>
      </AppCard>
    </div>
  </div>
</template>
