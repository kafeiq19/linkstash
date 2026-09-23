import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as bookmarksApi from '@/api/bookmarks'
import type {
  Bookmark,
  BookmarkQuery,
  StatusFilter,
  UpdateBookmarkBody,
} from '@/api/types'

export type ListMode = 'home' | 'favorites' | 'archived'

export const useBookmarksStore = defineStore('bookmarks', () => {
  const items = ref<Bookmark[]>([])
  const total = ref(0)
  const page = ref(1)
  const size = ref(20)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // filters
  const status = ref<StatusFilter>('all')
  const tag = ref('')
  const q = ref('')
  const mode = ref<ListMode>('home')

  function buildQuery(): BookmarkQuery {
    const query: BookmarkQuery = {
      page: page.value,
      size: size.value,
    }
    if (tag.value) query.tag = tag.value
    if (q.value) query.q = q.value

    // API accepts only unread|read|archived; 'all' means non-archived (omit status)
    const concreteStatus =
      status.value === 'all' ? undefined : (status.value as 'unread' | 'read' | 'archived')

    if (mode.value === 'archived') {
      query.status = 'archived'
    } else if (mode.value === 'favorites') {
      query.favorite = true
      if (concreteStatus) query.status = concreteStatus
    } else if (concreteStatus) {
      query.status = concreteStatus
    }
    return query
  }

  let fetchSeq = 0

  async function fetchList() {
    const seq = ++fetchSeq
    loading.value = true
    error.value = null
    try {
      const data = await bookmarksApi.listBookmarks(buildQuery())
      if (seq !== fetchSeq) return
      items.value = data.items
      total.value = data.total
    } catch (e) {
      if (seq !== fetchSeq) return
      error.value = e instanceof Error ? e.message : '加载失败'
      items.value = []
      total.value = 0
    } finally {
      if (seq === fetchSeq) loading.value = false
    }
  }

  function setFilters(next: {
    mode?: ListMode
    status?: StatusFilter
    tag?: string
    q?: string
    page?: number
  }) {
    if (next.mode !== undefined && next.mode !== mode.value) {
      mode.value = next.mode
      page.value = 1
      // drop previous list immediately so home cards don't flash on favorites/archived
      items.value = []
      total.value = 0
    }
    if (next.status !== undefined) {
      status.value = next.status
      page.value = 1
    }
    if (next.tag !== undefined) {
      tag.value = next.tag
      page.value = 1
    }
    if (next.q !== undefined) {
      q.value = next.q
      page.value = 1
    }
    if (next.page !== undefined) page.value = next.page
    return fetchList()
  }

  /** Optimistic insert of a placeholder card, then refresh after metadata may arrive. */
  async function addBookmark(url: string, tagNames?: string[]) {
    const placeholder: Bookmark = {
      id: -Date.now(),
      url,
      title: null,
      description: null,
      favicon: null,
      siteName: null,
      status: 'unread',
      favorite: false,
      note: '',
      tags: [],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }
    items.value = [placeholder, ...items.value]
    total.value += 1

    try {
      const created = await bookmarksApi.createBookmark({
        url,
        tagNames: tagNames?.length ? tagNames : undefined,
      })
      const idx = items.value.findIndex((b) => b.id === placeholder.id)
      if (idx >= 0) items.value[idx] = created
      else items.value = [created, ...items.value]

      // Signature moment: placeholder → metadata fade-in. Poll once after a short delay.
      window.setTimeout(() => {
        void refreshOne(created.id)
      }, 1500)
      return created
    } catch (e) {
      items.value = items.value.filter((b) => b.id !== placeholder.id)
      total.value = Math.max(0, total.value - 1)
      throw e
    }
  }

  async function refreshOne(id: number) {
    try {
      const fresh = await bookmarksApi.getBookmark(id)
      const idx = items.value.findIndex((b) => b.id === id)
      if (idx >= 0) items.value[idx] = fresh
    } catch {
      // metadata may still be fetching; ignore
    }
  }

  function patchLocal(updated: Bookmark) {
    const idx = items.value.findIndex((b) => b.id === updated.id)
    if (idx >= 0) items.value[idx] = updated
  }

  async function update(id: number, body: UpdateBookmarkBody) {
    const updated = await bookmarksApi.updateBookmark(id, body)
    // status changes may push item out of current filter — refetch
    const statusFilterMismatch =
      (status.value === 'unread' && updated.status !== 'unread') ||
      (status.value === 'read' && updated.status !== 'read') ||
      (status.value === 'all' && updated.status === 'archived' && mode.value !== 'archived')
    const leavesList =
      (mode.value === 'archived' && updated.status !== 'archived') ||
      (mode.value !== 'archived' && updated.status === 'archived') ||
      (mode.value === 'favorites' && !updated.favorite) ||
      statusFilterMismatch
    if (leavesList) {
      void fetchList()
    } else {
      patchLocal(updated)
    }
    return updated
  }

  async function remove(id: number) {
    await bookmarksApi.deleteBookmark(id)
    items.value = items.value.filter((b) => b.id !== id)
    total.value = Math.max(0, total.value - 1)
  }

  return {
    items,
    total,
    page,
    size,
    loading,
    error,
    status,
    tag,
    q,
    mode,
    fetchList,
    setFilters,
    addBookmark,
    refreshOne,
    update,
    remove,
    patchLocal,
  }
})
