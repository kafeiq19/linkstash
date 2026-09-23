/** API contract types matching linkstash-mvp spec. */

export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

export interface User {
  id: number
  username: string
}

export interface AuthResult {
  token: string
  user: User
}

export type BookmarkStatus = 'unread' | 'read' | 'archived'

export interface TagRef {
  id: number
  name: string
}

export interface Tag extends TagRef {
  count: number
}

export interface Bookmark {
  id: number
  url: string
  title: string | null
  description: string | null
  favicon: string | null
  siteName: string | null
  status: BookmarkStatus
  favorite: boolean
  note: string
  tags: TagRef[]
  createdAt: string
  updatedAt: string
}

export interface BookmarkPage {
  total: number
  items: Bookmark[]
}

export type StatusFilter = 'all' | 'unread' | 'read' | 'archived'

export interface BookmarkQuery {
  page?: number
  size?: number
  /** `all` = non-archived (unread+read); omitted means backend default */
  status?: StatusFilter
  tag?: string
  q?: string
  favorite?: boolean
}

export interface CreateBookmarkBody {
  url: string
  title?: string
  note?: string
  tagNames?: string[]
}

export interface UpdateBookmarkBody {
  title?: string
  note?: string
  status?: BookmarkStatus
  favorite?: boolean
  /** Extension for tag assign/replace (T8). Full replace of bookmark tags. */
  tagNames?: string[]
}
