<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { WrapText, Download } from 'lucide-vue-next'
import AppButton from '@/components/ui/AppButton.vue'

interface Props {
  content: string
  loading: boolean
  downloadUrl?: string
  fileName?: string
}

const props = withDefaults(defineProps<Props>(), {
  content: '',
  loading: false,
  downloadUrl: undefined,
  fileName: '',
})

const emit = defineEmits<{
  refresh: []
}>()

const wrap = ref(false)
const preRef = ref<HTMLPreElement | null>(null)

watch(() => props.content, async () => {
  await nextTick()
  if (preRef.value) {
    preRef.value.scrollTop = preRef.value.scrollHeight
  }
})

function toggleWrap() {
  wrap.value = !wrap.value
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
</script>

<template>
  <div class="flex flex-col gap-2">
    <div v-if="fileName" class="flex items-center justify-between">
      <span class="text-xs font-medium text-gray-500">{{ fileName }} ({{ formatSize(content.length) }})</span>
      <div class="flex items-center gap-1">
        <AppButton variant="ghost" size="sm" @click="toggleWrap" :disabled="loading">
          <WrapText class="h-3.5 w-3.5" />
          {{ wrap ? 'No wrap' : 'Wrap' }}
        </AppButton>
        <a
          v-if="downloadUrl"
          :href="downloadUrl"
          target="_blank"
          class="inline-flex items-center gap-1 rounded-lg px-2 py-1 text-xs font-medium text-gray-600 hover:text-gray-900 hover:bg-gray-100 transition-colors"
        >
          <Download class="h-3.5 w-3.5" />
          Download
        </a>
      </div>
    </div>

    <div
      class="relative overflow-hidden rounded-lg border border-gray-200 bg-gray-950"
      :class="{ 'min-h-[200px]': loading || !content }"
    >
      <div
        v-if="loading"
        class="flex items-center justify-center py-12"
      >
        <svg class="h-6 w-6 animate-spin text-gray-400" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
        </svg>
      </div>

      <pre
        v-show="!loading"
        ref="preRef"
        :class="[
          'm-0 max-h-[500px] overflow-auto p-4 text-xs leading-5 text-gray-100',
          wrap ? 'whitespace-pre-wrap break-all' : 'whitespace-pre',
        ]"
      ><code>{{ content || ' ' }}</code></pre>

      <div
        v-if="!loading && !content"
        class="absolute inset-0 flex items-center justify-center text-sm text-gray-500 pointer-events-none"
      >
        No output yet.
      </div>
    </div>
  </div>
</template>
