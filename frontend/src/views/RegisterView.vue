<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { ApiError } from '@/api/client'

const auth = useAuthStore()
const router = useRouter()

const form = reactive({ username: '', password: '', confirm: '' })
const loading = ref(false)

function validateUsername(name: string): boolean {
  return /^[A-Za-z0-9_]{3,32}$/.test(name)
}

async function onSubmit() {
  const username = form.username.trim()
  if (!validateUsername(username)) {
    ElMessage.warning('用户名需为 3–32 位字母、数字或下划线')
    return
  }
  if (form.password.length < 8) {
    ElMessage.warning('密码至少 8 位')
    return
  }
  if (form.password !== form.confirm) {
    ElMessage.warning('两次密码不一致')
    return
  }
  loading.value = true
  try {
    await auth.register(username, form.password)
    ElMessage.success('注册成功')
    void router.replace('/')
  } catch (e) {
    ElMessage.error(e instanceof ApiError ? e.message : '注册失败')
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
        <p class="text-muted text-sm">创建你的稍后读清单</p>
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
            autocomplete="new-password"
          />
        </div>
        <div>
          <label class="block text-caption text-muted mb-1.5">确认密码</label>
          <el-input
            v-model="form.confirm"
            type="password"
            placeholder="再次输入密码"
            size="large"
            show-password
            autocomplete="new-password"
          />
        </div>
        <el-button
          type="primary"
          size="large"
          class="w-full !h-10"
          :loading="loading"
          native-type="submit"
        >
          注册
        </el-button>
        <p class="text-center text-caption text-muted">
          已有账号？
          <router-link to="/login" class="text-accent hover:underline">登录</router-link>
        </p>
      </form>
    </div>
  </div>
</template>
