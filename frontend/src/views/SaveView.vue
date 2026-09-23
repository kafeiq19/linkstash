<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useBookmarksStore } from '@/stores/bookmarks'
import type { Bookmark } from '@/api/types'

const route = useRoute()
const router = useRouter()
const store = useBookmarksStore()

const rawUrl = ref('')
const title = ref('')
const note = ref('')
const saving = ref(false)
const saved = ref<Bookmark | null>(null)
const error = ref('')

const origin = window.location.origin

const bookmarklet = computed(() => {
  const target = `${origin}/save`
  return `javascript:(function(){location.href=${JSON.stringify(target)}+'?url='+encodeURIComponent(location.href)+'&title='+encodeURIComponent(document.title||'')})()`
})

function normalize(input: string) {
  const s = input.trim()
  if (!s) return ''
  if (/^https?:\/\//i.test(s)) return s
  return `https://${s}`
}

watch(
  () => [route.query.url, route.query.title] as const,
  ([u, t]) => {
    if (typeof u === 'string' && u) {
      rawUrl.value = u
      title.value = typeof t === 'string' ? t : ''
      saved.value = null
      void save()
    }
  },
  { immediate: true },
)

async function save() {
  const url = normalize(rawUrl.value)
  if (!url) {
    error.value = '请输入有效链接'
    return
  }
  saving.value = true
  error.value = ''
  try {
    let item = await store.addBookmark(url)
    const patch: { title?: string; note?: string } = {}
    if (title.value.trim()) patch.title = title.value.trim()
    if (note.value.trim()) patch.note = note.value.trim()
    if (Object.keys(patch).length) {
      item = await store.update(item.id, patch)
    }
    saved.value = item
    ElMessage.success('已保存到收件箱')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function copyBookmarklet() {
  try {
    await navigator.clipboard.writeText(bookmarklet.value)
    ElMessage.success('书签小工具代码已复制')
  } catch {
    ElMessage.error('复制失败，请手动创建书签并粘贴地址')
  }
}
</script>

<template>
  <div class="max-w-[560px]">
    <header class="mb-6">
      <h1 class="text-[28px] md:text-[32px] font-semibold tracking-tight text-ink leading-tight">保存</h1>
      <p class="text-caption text-muted mt-1">来自浏览器时会自动保存当前页面</p>
    </header>

    <el-alert v-if="error" :title="error" type="error" :closable="true" class="mb-4" @close="error = ''" />

    <div v-if="saved" class="bg-surface border border-line rounded-card p-5 mb-4">
      <div class="text-caption text-success mb-1">已保存</div>
      <div class="text-ink font-medium break-all">{{ saved.title || saved.url }}</div>
      <div class="text-caption text-muted mt-1 break-all">{{ saved.siteName || saved.url }}</div>
      <div class="mt-4 flex gap-3">
        <el-button type="primary" @click="router.push('/')">去收件箱</el-button>
        <el-button @click="router.push(`/b/${saved.id}`)">查看详情</el-button>
      </div>
    </div>

    <div v-else class="bg-surface border border-line rounded-card p-5 mb-4 space-y-3">
      <div>
        <label class="text-caption text-muted">链接</label>
        <el-input v-model="rawUrl" class="mt-1" placeholder="https://" />
      </div>
      <div>
        <label class="text-caption text-muted">标题（可选）</label>
        <el-input v-model="title" class="mt-1" placeholder="默认取网页标题" />
      </div>
      <div>
        <label class="text-caption text-muted">备注（可选）</label>
        <el-input v-model="note" class="mt-1" type="textarea" :rows="2" placeholder="写点备注…" />
      </div>
      <el-button type="primary" :loading="saving" class="w-full" @click="save">保存到收件箱</el-button>
    </div>

    <div class="bg-surface border border-line rounded-card p-5">
      <h2 class="text-sm font-semibold text-ink mb-1">书签小工具</h2>
      <p class="text-caption text-muted mb-3">把这个按钮拖到书签栏。以后在任意网页点一下，就会带着链接打开保存页。</p>
      <a
        class="inline-block px-3 py-2 rounded-lg bg-accent text-white text-sm font-medium no-underline cursor-grab active:cursor-grabbing"
        :href="bookmarklet"
        draggable="true"
        title="拖到浏览器书签栏"
        @click.prevent="copyBookmarklet()"
      >
        存入 Linkstash
      </a>
      <p class="text-caption text-muted mt-2">点按钮可复制代码，或直接拖到书签栏。</p>
    </div>
  </div>
</template>
