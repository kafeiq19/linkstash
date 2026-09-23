<script setup lang="ts">
defineProps<{
  title: string
  hint: string
}>()
</script>

<template>
  <div class="flex flex-col items-center justify-center py-20 text-center">
    <div
      class="w-14 h-14 rounded-2xl bg-surface border border-line shadow-card flex items-center justify-center mb-4"
    >
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" class="text-muted">
        <path
          d="M5 5h14v14H5V5z"
          stroke="currentColor"
          stroke-width="1.5"
          stroke-dasharray="3 2"
        />
        <path d="M8 12h8M8 9h5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
      </svg>
    </div>
    <h2 class="text-lg font-semibold text-ink">{{ title }}</h2>
    <p class="text-sm text-muted mt-1 max-w-[280px]">{{ hint }}</p>
  </div>
</template>
