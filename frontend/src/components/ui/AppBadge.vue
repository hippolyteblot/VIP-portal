<script setup lang="ts">
interface Props {
  variant?: 'primary' | 'success' | 'warning' | 'danger' | 'gray' | 'info' | 'yellow' | 'cyan' | 'green' | 'blue' | 'purple'
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

  yellow: 'bg-yellow-50 text-yellow-700 ring-yellow-600/20',
  cyan: 'bg-cyan-50 text-cyan-700 ring-cyan-600/20',
  green: 'bg-green-50 text-green-700 ring-green-600/20',
  blue: 'bg-blue-50 text-blue-700 ring-blue-600/20',
  purple: 'bg-purple-50 text-purple-700 ring-purple-600/20',

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
