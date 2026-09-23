import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as tagsApi from '@/api/tags'
import type { Tag } from '@/api/types'

export const useTagsStore = defineStore('tags', () => {
  const tags = ref<Tag[]>([])
  const loading = ref(false)

  async function fetchTags() {
    loading.value = true
    try {
      tags.value = await tagsApi.listTags()
    } catch {
      tags.value = []
    } finally {
      loading.value = false
    }
  }

  async function create(name: string) {
    const created = await tagsApi.createTag(name.trim())
    await fetchTags()
    return created
  }

  async function rename(id: number, name: string) {
    const updated = await tagsApi.renameTag(id, name.trim())
    await fetchTags()
    return updated
  }

  async function remove(id: number) {
    await tagsApi.deleteTag(id)
    await fetchTags()
  }

  return { tags, loading, fetchTags, create, rename, remove }
})
