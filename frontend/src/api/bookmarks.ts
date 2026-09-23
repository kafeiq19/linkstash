import { http } from './client'
import type {
  Bookmark,
  BookmarkPage,
  BookmarkQuery,
  CreateBookmarkBody,
  UpdateBookmarkBody,
} from './types'

export function listBookmarks(query: BookmarkQuery) {
  return http.get<BookmarkPage>('/bookmarks', {
    page: query.page,
    size: query.size,
    status: query.status,
    tag: query.tag,
    q: query.q,
    favorite: query.favorite,
  })
}

export function createBookmark(body: CreateBookmarkBody) {
  return http.post<Bookmark>('/bookmarks', body)
}

export function getBookmark(id: number) {
  return http.get<Bookmark>(`/bookmarks/${id}`)
}

export function updateBookmark(id: number, body: UpdateBookmarkBody) {
  return http.patch<Bookmark>(`/bookmarks/${id}`, body)
}

export function deleteBookmark(id: number) {
  return http.delete<{ ok: boolean }>(`/bookmarks/${id}`)
}
