<template>
  <div class="space-y-3">
    <div
      v-for="i in 5"
      :key="i"
      class="bg-surface border border-line rounded-card p-4 md:p-5 animate-pulse"
    >
      <div class="flex gap-4">
        <div class="w-8 h-8 rounded-md bg-paper border border-line shrink-0" />
        <div class="flex-1 space-y-2">
          <div class="h-4 bg-paper rounded w-2/3" />
          <div class="h-3 bg-paper rounded w-1/3" />
          <div class="h-3 bg-paper rounded w-full" />
          <div class="h-3 bg-paper rounded w-5/6" />
        </div>
      </div>
    </div>
  </div>
</template>
