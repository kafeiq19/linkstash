<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

function onUnauthorized() {
  auth.logout()
  void router.replace({ name: 'login' })
}

onMounted(() => {
  window.addEventListener('linkstash:unauthorized', onUnauthorized)
})
onUnmounted(() => {
  window.removeEventListener('linkstash:unauthorized', onUnauthorized)
})
</script>

<template>
  <router-view />
</template>
