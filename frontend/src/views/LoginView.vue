<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { ApiError } from '@/api/client'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function onSubmit() {
  if (form.username.trim().length < 3 || form.password.length < 8) {
    ElMessage.warning('用户名至少 3 位，密码至少 8 位')
    return
  }
  loading.value = true
  try {
    await auth.login(form.username.trim(), form.password)
    ElMessage.success('欢迎回来')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    void router.replace(redirect)
  } catch (e) {
    ElMessage.error(e instanceof ApiError ? e.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-paper flex items-center justify-center px-4">
    <div class="w-full max-w-[380px]">
      <div class="mb-8 text-center">
        <div class="inline-flex items-center gap-2 mb-3">
          <span
            class="w-9 h-9 rounded-xl bg-accent text-white flex items-center justify-center text-lg font-semibold"
            >L</span
          >
          <span class="text-[28px] font-semibold tracking-tight text-ink">Linkstash</span>
        </div>
        <p class="text-muted text-sm">登录以继续阅读清单</p>
      </div>

      <form
        class="bg-surface border border-line rounded-card shadow-card p-6 space-y-4"
        @submit.prevent="onSubmit"
      >
        <div>
          <label class="block text-caption text-muted mb-1.5">用户名</label>
          <el-input
            v-model="form.username"
            placeholder="3–32 位字母数字下划线"
            size="large"
            autocomplete="username"
          />
        </div>
        <div>
          <label class="block text-caption text-muted mb-1.5">密码</label>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="至少 8 位"
            size="large"
            show-password
            autocomplete="current-password"
          />
        </div>
        <el-button
          type="primary"
          size="large"
          class="w-full !h-10"
          :loading="loading"
          native-type="submit"
        >
          登录
        </el-button>
        <p class="text-center text-caption text-muted">
          还没有账号？
          <router-link to="/register" class="text-accent hover:underline">注册</router-link>
        </p>
      </form>
    </div>
  </div>
</template>
