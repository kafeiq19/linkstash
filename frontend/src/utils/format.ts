/** Lightweight display helpers. */

export function displayTitle(b: {
  title: string | null
  url: string
  pending?: boolean
}): string {
  if (b.title && b.title.trim()) return b.title
  try {
    const u = new URL(b.url)
    return u.hostname.replace(/^www\./, '') + u.pathname
  } catch {
    return b.url
  }
}

export function hostOf(url: string): string {
  try {
    return new URL(url).hostname.replace(/^www\./, '')
  } catch {
    return url
  }
}

export function formatDate(iso: string): string {
  try {
    const d = new Date(iso)
    return d.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    })
  } catch {
    return iso
  }
}

export function isValidUrl(raw: string): boolean {
  const s = raw.trim()
  if (!s) return false
  try {
    const u = new URL(s.startsWith('http') ? s : `https://${s}`)
    return Boolean(u.hostname && u.hostname.includes('.'))
  } catch {
    return false
  }
}

export function normalizeUrl(raw: string): string {
  const s = raw.trim()
  return /^https?:\/\//i.test(s) ? s : `https://${s}`
}
