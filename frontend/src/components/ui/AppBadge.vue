<script setup lang="ts">
interface Props {
  variant?: 'primary' | 'success' | 'warning' | 'danger' | 'gray' | 'info'
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'gray',
})

const variantClasses: Record<string, string> = {
  primary: 'bg-primary-50 text-primary-700 ring-primary-600/20',
  success: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
  warning: 'bg-amber-50 text-amber-700 ring-amber-600/20',
  danger: 'bg-red-50 text-red-700 ring-red-600/20',
  gray: 'bg-gray-50 text-gray-600 ring-gray-500/20',
  info: 'bg-sky-50 text-sky-700 ring-sky-600/20',
}
</script>

<template>
  <span
    :class="[
      'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset',
      variantClasses[props.variant],
    ]"
  >
    <slot />
  </span>
</template>
