<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useBookmarksStore, type ListMode } from '@/stores/bookmarks'
import { useTagsStore } from '@/stores/tags'
import BookmarkCard from '@/components/BookmarkCard.vue'
import SkeletonList from '@/components/SkeletonList.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const store = useBookmarksStore()
const tagsStore = useTagsStore()
const { items, total, page, size, loading, error, mode, tag, q, status } = storeToRefs(store)

const title = computed(() => {
  const m = mode.value as ListMode
  if (m === 'favorites') return '收藏'
  if (m === 'archived') return '归档'
  return '收件箱'
})

const subtitle = computed(() => {
  const bits: string[] = []
  if (q.value) bits.push(`搜索「${q.value}」`)
  if (tag.value) bits.push(`标签 #${tag.value}`)
  if (mode.value === 'home' && status.value !== 'all') {
    bits.push(status.value === 'unread' ? '未读' : '已读')
  }
  return bits.length ? bits.join(' · ') : `${total.value} 条`
})

const pageCount = computed(() => Math.max(1, Math.ceil(total.value / size.value)))
const bootstrapped = ref(false)

watch(
  () => loading.value,
  (v) => {
    if (!v) bootstrapped.value = true
  },
)

watch(
  () => route.query.tag,
  (t) => {
    const name = typeof t === 'string' ? t : ''
    if (name !== tag.value) void store.setFilters({ tag: name })
  },
)

function onPage(p: number) {
  void store.setFilters({ page: p })
}

function onUpdated() {
  void tagsStore.fetchTags()
}
</script>

<template>
  <div>
    <header class="mb-6">
      <h1 class="text-[28px] md:text-[32px] font-semibold tracking-tight text-ink leading-tight">
        {{ title }}
      </h1>
      <p class="text-caption text-muted mt-1">{{ subtitle }}</p>
    </header>

    <el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="true"
      class="mb-4"
      @close="store.fetchList()"
    />

    <div class="relative" :class="loading ? 'opacity-60 pointer-events-none' : ''">
      <!-- First visit only: skeleton. Later switches keep the list and just dim it. -->
      <SkeletonList v-if="loading && items.length === 0 && !bootstrapped" />

      <template v-else>
        <div class="space-y-3">
          <BookmarkCard
            v-for="b in items"
            :key="b.id"
            :bookmark="b"
            :pending-meta="b.id < 0"
            @changed="onUpdated"
            @refresh="(id: number) => store.refreshOne(id)"
          />
        </div>

        <div v-if="loading && items.length === 0" class="py-10 text-center text-caption text-muted">
          加载中…
        </div>

        <EmptyState
          v-if="!loading && items.length === 0"
          :title="tag || q ? '没有匹配的书签' : mode === 'favorites' ? '还没有收藏' : mode === 'archived' ? '归档是空的' : '还没有书签'"
          :hint="
            tag || q
              ? '试试调整筛选或搜索关键词'
              : mode === 'favorites'
                ? '在书签卡片上点星标即可收藏'
                : mode === 'archived'
                  ? '归档后的条目会出现在这里'
                  : '在顶部粘贴一个 URL，开始你的稍后读清单'
          "
        />
      </template>
    </div>

    <div v-if="pageCount > 1" class="mt-8 flex justify-center">
      <el-pagination
        layout="prev, pager, next"
        :total="total"
        :page-size="size"
        :current-page="page"
        @current-change="onPage"
      />
    </div>
  </div>
</template>
