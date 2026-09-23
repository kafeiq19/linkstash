<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as bookmarksApi from '@/api/bookmarks'
import type { Bookmark, BookmarkStatus } from '@/api/types'
import { useBookmarksStore } from '@/stores/bookmarks'
import { useTagsStore } from '@/stores/tags'
import { displayTitle, hostOf, formatDate } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const store = useBookmarksStore()
const tagsStore = useTagsStore()

const bookmark = ref<Bookmark | null>(null)
const loading = ref(true)
const error = ref('')

const draftTitle = ref('')
const draftNote = ref('')
const selectedTagNames = ref<string[]>([])
const tagDialog = ref(false)

const id = computed(() => Number(route.params.id))

const title = computed(() => (bookmark.value ? displayTitle(bookmark.value) : ''))
const host = computed(() => (bookmark.value ? hostOf(bookmark.value.url) : ''))

async function load() {
  loading.value = true
  error.value = ''
  try {
    bookmark.value = await bookmarksApi.getBookmark(id.value)
    draftTitle.value = bookmark.value.title || ''
    draftNote.value = bookmark.value.note || ''
    selectedTagNames.value = bookmark.value.tags.map((t) => t.name)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
    bookmark.value = null
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void load()
  void tagsStore.fetchTags()
})

watch(id, () => void load())

async function saveMeta() {
  if (!bookmark.value) return
  try {
    const updated = await store.update(bookmark.value.id, {
      title: draftTitle.value.trim(),
      note: draftNote.value,
    })
    bookmark.value = updated
    ElMessage.success('已保存')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}

async function setStatus(status: BookmarkStatus) {
  if (!bookmark.value) return
  try {
    const updated = await store.update(bookmark.value.id, { status })
    bookmark.value = updated
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function toggleFavorite() {
  if (!bookmark.value) return
  try {
    const updated = await store.update(bookmark.value.id, {
      favorite: !bookmark.value.favorite,
    })
    bookmark.value = updated
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function saveTags() {
  if (!bookmark.value) return
  try {
    const updated = await store.update(bookmark.value.id, {
      tagNames: [...selectedTagNames.value],
    })
    bookmark.value = updated
    tagDialog.value = false
    ElMessage.success('标签已更新')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '更新标签失败')
  }
}

async function onDelete() {
  if (!bookmark.value) return
  try {
    await ElMessageBox.confirm('删除这条书签？此操作不可恢复。', '删除书签', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger',
    })
  } catch {
    return
  }
  try {
    await store.remove(bookmark.value.id)
    ElMessage.success('已删除')
    void router.push('/')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '删除失败')
  }
}
</script>

<template>
  <div>
    <button class="text-caption text-muted hover:text-accent mb-4" @click="router.back()">
      ← 返回列表
    </button>

    <el-alert v-if="error" :title="error" type="error" class="mb-4" />

    <div v-if="loading" class="bg-surface border border-line rounded-card p-8 animate-pulse">
      <div class="h-6 bg-paper rounded w-1/2 mb-4" />
      <div class="h-4 bg-paper rounded w-1/3 mb-2" />
      <div class="h-4 bg-paper rounded w-full" />
    </div>

    <article
      v-else-if="bookmark"
      class="bg-surface border border-line rounded-card shadow-card p-6 md:p-8"
    >
      <div class="flex items-start gap-4">
        <img
          v-if="bookmark.favicon"
          :src="bookmark.favicon"
          alt=""
          class="w-10 h-10 rounded-lg object-contain bg-paper border border-line"
        />
        <div class="flex-1 min-w-0">
          <h1 class="text-[28px] font-semibold tracking-tight text-ink leading-tight break-words">
            {{ title }}
          </h1>
          <div class="flex items-center gap-2 mt-2 text-caption text-muted flex-wrap">
            <a
              :href="bookmark.url"
              target="_blank"
              rel="noopener"
              class="text-accent hover:underline break-all"
              >{{ bookmark.url }}</a
            >
            <span>·</span>
            <span>{{ host }}</span>
            <span>·</span>
            <span>{{ formatDate(bookmark.createdAt) }}</span>
          </div>
        </div>
        <button
          class="fav-star shrink-0"
          :class="{ 'is-active': bookmark.favorite }"
          @click="toggleFavorite"
        >
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
            <path
              d="M12 2.5l2.9 5.88 6.5.95-4.7 4.58 1.11 6.47L12 17.33 6.19 20.38l1.11-6.47-4.7-4.58 6.5-.95L12 2.5z"
              :fill="bookmark.favorite ? 'currentColor' : 'none'"
              stroke="currentColor"
              stroke-width="1.6"
              stroke-linejoin="round"
            />
          </svg>
        </button>
      </div>

      <p v-if="bookmark.description" class="mt-4 text-sm text-muted leading-relaxed">
        {{ bookmark.description }}
      </p>

      <div class="mt-6 space-y-4">
        <div>
          <label class="block text-caption text-muted mb-1.5">标题</label>
          <el-input v-model="draftTitle" placeholder="标题" />
        </div>
        <div>
          <label class="block text-caption text-muted mb-1.5">备注</label>
          <el-input v-model="draftNote" type="textarea" :rows="3" placeholder="写点备注…" />
        </div>
        <div class="flex flex-wrap gap-2">
          <el-button type="primary" @click="saveMeta">保存修改</el-button>
          <el-button @click="tagDialog = true">管理标签</el-button>
          <el-button
            v-if="bookmark.status !== 'read'"
            @click="setStatus('read')"
            >标为已读</el-button
          >
          <el-button v-else @click="setStatus('unread')">标为未读</el-button>
          <el-button v-if="bookmark.status !== 'archived'" @click="setStatus('archived')"
            >归档</el-button
          >
          <el-button v-else @click="setStatus('unread')">取消归档</el-button>
          <el-button type="danger" plain @click="onDelete">删除</el-button>
        </div>
      </div>

      <div v-if="bookmark.tags.length" class="mt-6 flex flex-wrap gap-1.5">
        <span
          v-for="t in bookmark.tags"
          :key="t.id"
          class="px-2.5 py-0.5 rounded-full bg-paper border border-line text-caption text-muted"
        >
          {{ t.name }}
        </span>
      </div>
    </article>

    <el-dialog v-model="tagDialog" title="管理标签" width="360px">
      <div class="space-y-2 max-h-[280px] overflow-y-auto">
        <el-checkbox
          v-for="t in tagsStore.tags"
          :key="t.id"
          :model-value="selectedTagNames.includes(t.name)"
          @change="
            (val: boolean | string | number) => {
              const name = t.name
              if (val) {
                if (!selectedTagNames.includes(name)) selectedTagNames.push(name)
              } else {
                selectedTagNames = selectedTagNames.filter((n) => n !== name)
              }
            }
          "
        >
          {{ t.name }}
        </el-checkbox>
        <p v-if="!tagsStore.tags.length" class="text-caption text-muted py-2">先在侧栏创建标签</p>
      </div>
      <template #footer>
        <el-button @click="tagDialog = false">取消</el-button>
        <el-button type="primary" @click="saveTags">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
