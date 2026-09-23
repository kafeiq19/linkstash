<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useTagsStore } from '@/stores/tags'
import { useBookmarksStore, type ListMode } from '@/stores/bookmarks'
import type { StatusFilter } from '@/api/types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const tagsStore = useTagsStore()
const store = useBookmarksStore()

const searchInput = ref('')
const addUrl = ref('')
const adding = ref(false)

const mode = computed<ListMode>(() => {
  const m = route.meta.mode
  return (m as ListMode) || 'home'
})

const statusOptions = computed(() => {
  if (mode.value === 'archived') return [{ value: 'archived', label: '已归档' }]
  return [
    { value: 'all', label: '全部' },
    { value: 'unread', label: '未读' },
    { value: 'read', label: '已读' },
  ]
})

const currentStatus = computed<StatusFilter>(() => store.status)

onMounted(() => {
  void tagsStore.fetchTags()
  void store.setFilters({ mode: mode.value, tag: (route.query.tag as string) || '', q: '' })
})

watch(
  () => [route.meta.mode, route.query.tag] as const,
  () => {
    const nextMode = (route.meta.mode as ListMode) || 'home'
    const nextTag = typeof route.query.tag === 'string' ? route.query.tag : ''
    if (nextMode === store.mode && nextTag === store.tag) return
    void store.setFilters({ mode: nextMode, tag: nextTag })
  },
)

function onSearch() {
  void store.setFilters({ q: searchInput.value.trim() })
}

function clearSearch() {
  searchInput.value = ''
  void store.setFilters({ q: '' })
}

function onStatus(s: StatusFilter) {
  void store.setFilters({ status: s })
}

function onSelectTag(name: string) {
  const next = store.tag === name ? '' : name
  void router.replace({
    path: route.path,
    query: { ...route.query, tag: next || undefined },
  })
}

async function onAdd() {
  const raw = addUrl.value.trim()
  if (!raw) return
  adding.value = true
  try {
    // ensure URL has scheme
    const url = /^https?:\/\//i.test(raw) ? raw : `https://${raw}`
    await store.addBookmark(url)
    addUrl.value = ''
    ElMessage.success('已保存，正在抓取标题…')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    adding.value = false
  }
}

function onAddKeyup(e: KeyboardEvent) {
  if (e.key === 'Enter') void onAdd()
}

async function onCreateTag() {
  try {
    const { value } = await ElMessageBox.prompt('标签名称（1–24 字符）', '新建标签', {
      confirmButtonText: '创建',
      cancelButtonText: '取消',
      inputPattern: /^.{1,24}$/,
      inputErrorMessage: '请输入 1–24 个字符',
    })
    await tagsStore.create(value)
    ElMessage.success('标签已创建')
  } catch {
    /* cancelled */
  }
}

async function onRenameTag(id: number, name: string) {
  try {
    const { value } = await ElMessageBox.prompt('重命名标签', '编辑标签', {
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      inputValue: name,
      inputPattern: /^.{1,24}$/,
      inputErrorMessage: '请输入 1–24 个字符',
    })
    await tagsStore.rename(id, value)
    ElMessage.success('已重命名')
    void store.fetchList()
  } catch {
    /* cancelled */
  }
}

async function onDeleteTag(id: number, name: string) {
  try {
    await ElMessageBox.confirm(`删除标签「${name}」？书签不会被删除。`, '删除标签', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await tagsStore.remove(id)
    ElMessage.success('标签已删除')
    if (store.tag === name) void store.setFilters({ tag: '' })
    else void store.fetchList()
  } catch {
    /* cancelled */
  }
}

async function onLogout() {
  try {
    await ElMessageBox.confirm('退出当前账号？', '退出登录', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  auth.logout()
  void router.replace({ name: 'login' })
}

function go(name: string) {
  void router.push({ name })
}
</script>

<template>
  <div class="min-h-screen bg-paper flex flex-col">
    <!-- Top bar 56px -->
    <header
      class="h-14 shrink-0 bg-surface border-b border-line flex items-center gap-4 px-5 sticky top-0 z-30"
    >
      <router-link to="/" class="flex items-center gap-2 shrink-0">
        <span
          class="w-7 h-7 rounded-lg bg-accent text-white flex items-center justify-center text-sm font-semibold"
          >L</span
        >
        <span class="font-semibold text-ink tracking-tight hidden sm:inline">Linkstash</span>
      </router-link>

      <div class="flex-1 max-w-[360px] mx-auto">
        <el-input
          v-model="searchInput"
          placeholder="搜索标题 / 描述 / 链接…"
          clearable
          @keyup.enter="onSearch"
          @clear="clearSearch"
        />
      </div>

      <div class="flex items-center gap-3 shrink-0">
        <div class="hidden md:flex items-center gap-2 w-[280px]">
          <el-input
            v-model="addUrl"
            placeholder="粘贴 URL 快速添加…"
            clearable
            :disabled="adding"
            @keyup="onAddKeyup"
          />
          <el-button type="primary" :loading="adding" @click="onAdd">保存</el-button>
        </div>
        <el-dropdown @command="(c: string) => (c === 'logout' ? onLogout() : null)">
          <span class="flex items-center gap-1.5 cursor-pointer text-ink text-sm">
            <span
              class="w-7 h-7 rounded-full bg-accent/10 text-accent flex items-center justify-center text-xs font-semibold"
              >{{ (auth.user?.username || '?').slice(0, 1).toUpperCase() }}</span
            >
            <span class="hidden lg:inline">{{ auth.user?.username }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>{{ auth.user?.username }}</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- Mobile add row -->
    <div class="md:hidden px-4 py-2 border-b border-line bg-surface flex gap-2">
      <el-input v-model="addUrl" placeholder="粘贴 URL…" clearable @keyup="onAddKeyup" />
      <el-button type="primary" :loading="adding" @click="onAdd">保存</el-button>
    </div>

    <div class="flex-1 flex min-h-0">
      <!-- Left rail 240px -->
      <aside
        class="w-[240px] shrink-0 border-r border-line bg-paper px-3 py-5 overflow-y-auto hidden md:block"
      >
        <nav class="space-y-0.5 mb-6">
          <button
            v-for="opt in [
              { name: 'home', label: '收件箱' },
              { name: 'favorites', label: '收藏' },
              { name: 'archived', label: '归档' },
            ]"
            :key="opt.name"
            class="w-full text-left px-3 py-2 rounded-lg text-sm transition-colors"
            :class="
              route.name === opt.name
                ? 'bg-accent/10 text-accent font-medium'
                : 'text-ink hover:bg-black/[0.04]'
            "
            @click="go(opt.name)"
          >
            {{ opt.label }}
          </button>
        </nav>

        <div v-if="mode !== 'archived'" class="mb-6">
          <div class="px-3 mb-1.5 text-caption text-muted">状态</div>
          <button
            v-for="opt in statusOptions"
            :key="opt.value"
            class="w-full text-left px-3 py-1.5 rounded-lg text-sm transition-colors"
            :class="
              currentStatus === opt.value
                ? 'bg-black/[0.06] text-ink font-medium'
                : 'text-muted hover:bg-black/[0.04]'
            "
            @click="onStatus(opt.value as StatusFilter)"
          >
            {{ opt.label }}
          </button>
        </div>

        <div>
          <div class="flex items-center justify-between px-3 mb-1.5">
            <span class="text-caption text-muted">标签</span>
            <button
              class="text-caption text-accent hover:underline"
              title="新建标签"
              @click="onCreateTag"
            >
              + 新建
            </button>
          </div>
          <div v-if="tagsStore.tags.length === 0" class="px-3 py-2 text-caption text-muted">
            暂无标签
          </div>
          <div
            v-for="t in tagsStore.tags"
            :key="t.id"
            class="group flex items-center justify-between px-3 py-1.5 rounded-lg cursor-pointer transition-colors"
            :class="
              store.tag === t.name ? 'bg-accent/10 text-accent' : 'text-ink hover:bg-black/[0.04]'
            "
            @click="onSelectTag(t.name)"
          >
            <span class="text-sm truncate">{{ t.name }}</span>
            <span class="flex items-center gap-1">
              <span class="text-caption text-muted">{{ t.count }}</span>
              <span class="hidden group-hover:flex items-center gap-0.5">
                <button
                  class="p-0.5 rounded hover:bg-black/10 text-muted"
                  title="重命名"
                  @click.stop="onRenameTag(t.id, t.name)"
                >
                  ✎
                </button>
                <button
                  class="p-0.5 rounded hover:bg-danger/10 text-muted hover:text-danger"
                  title="删除"
                  @click.stop="onDeleteTag(t.id, t.name)"
                >
                  ×
                </button>
              </span>
            </span>
          </div>
        </div>
      </aside>

      <!-- Content max-width 880px -->
      <main class="flex-1 min-w-0 overflow-y-auto">
        <div class="max-w-[880px] mx-auto px-5 py-8 md:px-10 md:py-10">
          <router-view />
        </div>
      </main>
    </div>
  </div>
</template>
