import { http } from './client'
import type { Tag, TagRef } from './types'

export function listTags() {
  return http.get<Tag[]>('/tags')
}

export function createTag(name: string) {
  return http.post<TagRef>('/tags', { name })
}

export function renameTag(id: number, name: string) {
  return http.patch<TagRef>(`/tags/${id}`, { name })
}

export function deleteTag(id: number) {
  return http.delete<{ ok: boolean }>(`/tags/${id}`)
}
