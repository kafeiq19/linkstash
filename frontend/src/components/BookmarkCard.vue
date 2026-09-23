<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Bookmark, BookmarkStatus } from '@/api/types'
import { useBookmarksStore } from '@/stores/bookmarks'
import { useTagsStore } from '@/stores/tags'
import { displayTitle, hostOf, formatDate } from '@/utils/format'

const props = defineProps<{
  bookmark: Bookmark
  pendingMeta?: boolean
}>()

const emit = defineEmits<{
  changed: []
  refresh: [id: number]
}>()

const store = useBookmarksStore()
const tagsStore = useTagsStore()

const editing = ref(false)
const draftTitle = ref('')
const draftNote = ref('')
const tagDrawer = ref(false)
const selectedTagNames = ref<string[]>([])
const starPulse = ref(false)
const metaVisible = ref(!props.pendingMeta)

const title = computed(() => displayTitle(props.bookmark))
const host = computed(() => hostOf(props.bookmark.url))

function startEdit() {
  draftTitle.value = props.bookmark.title || ''
  draftNote.value = props.bookmark.note || ''
  editing.value = true
}

async function saveEdit() {
  try {
    await store.update(props.bookmark.id, {
      title: draftTitle.value.trim() || props.bookmark.title || '',
      note: draftNote.value,
    })
    editing.value = false
    ElMessage.success('已保存')
    emit('changed')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}

async function toggleRead() {
  const next: BookmarkStatus = props.bookmark.status === 'read' ? 'unread' : 'read'
  try {
    await store.update(props.bookmark.id, { status: next })
    emit('changed')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function toggleFavorite() {
  starPulse.value = true
  window.setTimeout(() => (starPulse.value = false), 400)
  try {
    await store.update(props.bookmark.id, { favorite: !props.bookmark.favorite })
    emit('changed')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function toggleArchive() {
  const next: BookmarkStatus = props.bookmark.status === 'archived' ? 'unread' : 'archived'
  try {
    await store.update(props.bookmark.id, { status: next })
    ElMessage.success(next === 'archived' ? '已归档' : '已移出归档')
    emit('changed')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function onDelete() {
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
    await store.remove(props.bookmark.id)
    ElMessage.success('已删除')
    emit('changed')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '删除失败')
  }
}

function openTags() {
  selectedTagNames.value = props.bookmark.tags.map((t) => t.name)
  tagDrawer.value = true
}

async function saveTags() {
  try {
    await store.update(props.bookmark.id, { tagNames: [...selectedTagNames.value] })
    tagDrawer.value = false
    ElMessage.success('标签已更新')
    emit('changed')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '更新标签失败')
  }
}

function onMetaEnter() {
  metaVisible.value = true
}

// When pendingMeta becomes false (after refresh), fade metadata in
watch(
  () => props.pendingMeta,
  (pending) => {
    if (!pending) metaVisible.value = true
  },
)
</script>

<template>
  <article
    class="bg-surface border border-line rounded-card shadow-card hover:shadow-card-hover transition-shadow p-4 md:p-5"
  >
    <div class="flex gap-3 md:gap-4">
      <!-- favicon -->
      <div class="shrink-0 pt-0.5">
        <img
          v-if="bookmark.favicon && !pendingMeta"
          :src="bookmark.favicon"
          alt=""
          class="w-8 h-8 rounded-md object-contain bg-paper border border-line"
          @error="($event.target as HTMLImageElement).style.display = 'none'"
        />
        <div
          v-else
          class="w-8 h-8 rounded-md bg-paper border border-line flex items-center justify-center text-muted text-caption"
        >
          {{ host.slice(0, 1).toUpperCase() }}
        </div>
      </div>

      <div class="flex-1 min-w-0">
        <div class="flex items-start gap-2">
          <div class="flex-1 min-w-0">
            <template v-if="editing">
              <el-input v-model="draftTitle" size="large" class="mb-2" placeholder="标题" />
            </template>
            <template v-else>
              <h2 class="text-[15px] font-semibold text-ink leading-snug break-words">
                <router-link
                  :to="`/b/${bookmark.id > 0 ? bookmark.id : ''}`"
                  class="hover:text-accent transition-colors"
                  :class="{ 'opacity-60': pendingMeta }"
                >
                  {{ title }}
                </router-link>
              </h2>
            </template>
            <div class="flex items-center gap-2 mt-1 text-caption text-muted">
              <a
                :href="bookmark.url"
                target="_blank"
                rel="noopener"
                class="hover:text-accent truncate max-w-[240px]"
                >{{ host }}</a
              >
              <span>·</span>
              <span class="shrink-0">{{ formatDate(bookmark.createdAt) }}</span>
              <span
                v-if="bookmark.status === 'unread'"
                class="shrink-0 px-1.5 py-0.5 rounded bg-accent/10 text-accent"
                >未读</span
              >
              <span
                v-else-if="bookmark.status === 'read'"
                class="shrink-0 px-1.5 py-0.5 rounded bg-success/10 text-success"
                >已读</span
              >
              <span
                v-else
                class="shrink-0 px-1.5 py-0.5 rounded bg-black/[0.06] text-muted"
                >归档</span
              >
            </div>
          </div>

          <div class="flex items-center gap-0.5 shrink-0 -mt-1 -mr-1">
            <button
              class="fav-star"
              :class="{ 'is-active': bookmark.favorite, 'just-toggled': starPulse }"
              :title="bookmark.favorite ? '取消收藏' : '收藏'"
              @click="toggleFavorite"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
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
        </div>

        <!-- description: fade in when metadata arrives -->
        <transition name="meta-fade" @after-enter="onMetaEnter">
          <p
            v-if="(bookmark.description || bookmark.siteName) && metaVisible && !pendingMeta"
            class="mt-2 text-[13px] text-muted leading-relaxed line-clamp-2"
          >
            <span v-if="bookmark.siteName" class="text-ink/70 font-medium"
              >{{ bookmark.siteName }} ·
            </span>
            {{ bookmark.description }}
          </p>
          <p
            v-else-if="pendingMeta"
            class="mt-2 text-[13px] text-muted/70 animate-pulse"
          >
            正在抓取标题与摘要…
          </p>
        </transition>

        <template v-if="editing">
          <el-input
            v-model="draftNote"
            type="textarea"
            :rows="2"
            placeholder="备注"
            class="mt-2"
          />
          <div class="mt-2 flex gap-2">
            <el-button type="primary" size="small" @click="saveEdit">保存</el-button>
            <el-button size="small" @click="editing = false">取消</el-button>
          </div>
        </template>

        <!-- note preview -->
        <p
          v-else-if="bookmark.note"
          class="mt-2 text-[13px] text-ink/80 bg-paper border border-line rounded-lg px-3 py-2"
        >
          {{ bookmark.note }}
        </p>

        <!-- tags -->
        <div v-if="bookmark.tags.length" class="mt-3 flex flex-wrap gap-1.5">
          <span
            v-for="t in bookmark.tags"
            :key="t.id"
            class="px-2 py-0.5 rounded-full bg-paper border border-line text-caption text-muted"
          >
            {{ t.name }}
          </span>
        </div>

        <!-- actions -->
        <div class="mt-3 flex flex-wrap items-center gap-1 text-caption">
          <button
            class="px-2 py-1 rounded-md text-muted hover:bg-black/[0.04] hover:text-ink transition-colors"
            @click="toggleRead"
          >
            {{ bookmark.status === 'read' ? '标为未读' : '标为已读' }}
          </button>
          <button
            class="px-2 py-1 rounded-md text-muted hover:bg-black/[0.04] hover:text-ink transition-colors"
            @click="startEdit"
          >
            编辑
          </button>
          <button
            class="px-2 py-1 rounded-md text-muted hover:bg-black/[0.04] hover:text-ink transition-colors"
            @click="openTags"
          >
            标签
          </button>
          <button
            class="px-2 py-1 rounded-md text-muted hover:bg-black/[0.04] hover:text-ink transition-colors"
            @click="toggleArchive"
          >
            {{ bookmark.status === 'archived' ? '取消归档' : '归档' }}
          </button>
          <a
            :href="bookmark.url"
            target="_blank"
            rel="noopener"
            class="px-2 py-1 rounded-md text-muted hover:bg-black/[0.04] hover:text-ink transition-colors"
            >打开</a
          >
          <button
            class="px-2 py-1 rounded-md text-muted hover:bg-danger/10 hover:text-danger transition-colors ml-auto"
            @click="onDelete"
          >
            删除
          </button>
        </div>
      </div>
    </div>

    <!-- tag assign dialog -->
    <el-dialog v-model="tagDrawer" title="管理标签" width="360px">
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
        <p v-if="!tagsStore.tags.length" class="text-caption text-muted py-2">
          先在左侧创建标签
        </p>
      </div>
      <template #footer>
        <el-button @click="tagDrawer = false">取消</el-button>
        <el-button type="primary" @click="saveTags">保存</el-button>
      </template>
    </el-dialog>
  </article>
</template>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
