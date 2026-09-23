---
feature: linkstash-mvp
status: in-progress
updated: 2026-07-22
branch: feat/mvp
commits: 
---

# Linkstash MVP

## Report

## [S1] Problem

用户在多设备浏览网页时，缺少一个「稍后读」闭环：把链接快速存下来，自动带走标题/摘要/站点图标，再按未读/已读/收藏/标签/搜索找回。需要一个可自己部署的前后端分离书签站，支持多用户数据隔离，并开源到 GitHub。

## [S2] Design

### Product

产品名 **Linkstash**。公开 monorepo，仓库名 `linkstash`。作者 GitHub：**kafeiq19**。

MVP 闭环：

1. 注册 / 登录（多用户，数据按 `user_id` 隔离）
2. 粘贴 URL 保存书签；服务端异步抓取 `title` / `description` / `favicon` / `siteName`
3. 书签列表：分页、状态筛选（收件箱默认未读+已读；未读 / 已读 / 归档）、标签筛选、关键词搜索（title/description/url）
4. 单条书签：切换已读/归档、收藏、编辑标题与备注、删除、绑定/解绑标签
5. 标签管理：创建、重命名、删除（仅影响本人数据）

### Stack

| Layer | Choice |
| --- | --- |
| Frontend | Vue 3 + Vite + TypeScript + Pinia + Vue Router + Element Plus + Tailwind CSS |
| Backend | Java 17 + Spring Boot 3 + MyBatis-Plus + sqlite-jdbc |
| Auth | 注册/登录 + JWT（`Authorization: Bearer`），Spring Security 过滤器 |
| Meta fetch | Jsoup 抓取 Open Graph / `<title>` / favicon |
| DB | SQLite 文件库（`backend/data/linkstash.db`），启动时执行 schema |
| Repo | monorepo：`frontend/` + `backend/` + `docs/` |

### API contract

Base: `/api`。除 `auth/*` 外均需 JWT。响应统一：

```json
{ "code": 0, "message": "ok", "data": {} }
```

错误：`code != 0`，HTTP 4xx/5xx；鉴权失败 401；他人资源 404（不泄露存在性）。

#### Auth

| Method | Path | Body / Query | Result |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | `{username, password}` | `{token, user:{id, username}}` |
| POST | `/api/auth/login` | `{username, password}` | 同上 |
| GET | `/api/auth/me` | — | `user:{id, username}` |

- `username`：3–32 位，字母数字下划线；`password`：≥8 位
- 密码 `BCrypt` 存储；JWT **HS256**（显式固定算法），载荷 `sub=userId`，默认 7 天

#### Bookmarks

| Method | Path | Body / Query | Result |
| --- | --- | --- | --- |
| GET | `/api/bookmarks` | `page, size, status, tag, q, favorite` | `{total, items:[Bookmark]}`（`status` 省略=收件箱未归档；`unread\|read\|archived` 为精确筛选） |
| POST | `/api/bookmarks` | `{url, title?, note?, tagNames?[]}` | `Bookmark`（metadata 可先空，异步补全） |
| GET | `/api/bookmarks/{id}` | — | `Bookmark` |
| PATCH | `/api/bookmarks/{id}` | `{title?, note?, status?, favorite?, tagNames?[]}` | `Bookmark`（`tagNames` 传入时整组替换） |
| DELETE | `/api/bookmarks/{id}` | — | `{ok:true}` |

`status`: `unread` | `read` | `archived`。创建后默认 `unread`，`favorite=false`。`PATCH` 中 `tagNames` 非 null 时整组替换书签标签。

`Bookmark`：

```json
{
  "id": 1,
  "url": "https://example.com/post",
  "title": "…",
  "description": "…",
  "favicon": "https://…/favicon.ico",
  "siteName": "Example",
  "status": "unread",
  "favorite": false,
  "note": "",
  "tags": [{"id": 1, "name": "read"}],
  "createdAt": "2026-07-22T10:00:00Z",
  "updatedAt": "2026-07-22T10:00:00Z"
}
```

#### Tags

| Method | Path | Body | Result |
| --- | --- | --- | --- |
| GET | `/api/tags` | — | `[{id, name, count}]` |
| POST | `/api/tags` | `{name}` | `Tag` |
| PATCH | `/api/tags/{id}` | `{name}` | `Tag` |
| DELETE | `/api/tags/{id}` | — | `{ok:true}` |

创建书签时可用 `tagNames` upsert；标签名 trim 后 1–24 字符，同用户唯一。

### Data model (SQLite)

```sql
user(
  id INTEGER PK AUTOINCREMENT,
  username TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  created_at TEXT NOT NULL
)

bookmark(
  id INTEGER PK AUTOINCREMENT,
  user_id INTEGER NOT NULL,
  url TEXT NOT NULL,
  title TEXT,
  description TEXT,
  favicon TEXT,
  site_name TEXT,
  status TEXT NOT NULL DEFAULT 'unread',  -- unread|read|archived
  favorite INTEGER NOT NULL DEFAULT 0,
  note TEXT DEFAULT '',
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
)

tag(
  id INTEGER PK AUTOINCREMENT,
  user_id INTEGER NOT NULL,
  name TEXT NOT NULL,
  created_at TEXT NOT NULL,
  UNIQUE(user_id, name)
)

bookmark_tag(
  bookmark_id INTEGER NOT NULL,
  tag_id INTEGER NOT NULL,
  PRIMARY KEY(bookmark_id, tag_id)
)
```

索引：`bookmark(user_id, status)`, `bookmark(user_id, favorite)`, `tag(user_id, name)`。

### Metadata fetch

- `POST /api/bookmarks` 事务内写入占位记录后，用 `@Async` 补全元数据
- 优先 `og:title` → `title`；`og:description` → `meta[name=description]`；favicon 相对路径补全为绝对 URL
- 抓取超时 5s，失败保留原始 URL 与空元数据；不阻塞创建接口
- 不跟过多跳、不执行页面 JS

### Frontend behavior & UX

路由：

- `/login`、`/register`
- `/` 书签主列表（默认未读+已读）
- `/archived` 归档
- `/favorites` 收藏
- `/b/:id` 详情/编辑（也可靠弹窗完成核心操作）

布局：

- 顶栏：Logo、全局搜索、粘贴 URL 快速添加
- 左侧栏：状态筛选、标签列表（含计数）、新建标签
- 主区：卡片列表（favicon、标题、域名、摘要两行、标签 chips、状态/收藏操作）

**视觉方向**

- 风格锚点：Raindrop.io / Pocket 的干净卡片列表，带一点纸感编辑器气质（Notion 页边距）
- 色板：背景 `#F4F2EE` 纸色，表面 `#FFFFFF`，墨色 `#1C1C1E`，弱化 `#6B6B76`，描边 `#E4E1DA`，主强调靛蓝 `#3B5BDB`，收藏琥珀 `#E8A838`，成功 `#2F9E44`，危险 `#E03131`
- 字体：Latin `Inter` / CJK `PingFang SC, Microsoft YaHei`；展示 28–32/600，正文 14/400，辅助 12/400
- 布局：顶栏 56px；左栏 240px；内容最大宽 880px；卡片圆角 12px，阴影极轻
- 记忆点：① 粘贴 URL 后卡片占位 → 元数据淡入；② 收藏星从描边到实心的琥珀填充微交互

状态约定：加载 skeleton、空态一句话+添加引导、错误 toast；表单用 Element Plus。

### Error & edge cases

- 重复用户名注册 → 400
- 错误密码/未知用户 → 401，消息不区分两者
- 访问非本人书签/标签 → 404
- 非法 status → 400
- 同 URL 允许重复保存（用户可能要不同标签/笔记）
- CORS：开发期允许 Vite origin（默认 5173）

### Testing boundaries

- 后端：注册登录 JWT 流程；书签 CRUD 与用户隔离；标签 CRUD；搜索/筛选；metadata service 对静态 HTML 解析单测
- 前端：`vite build` 必须通过；鉴权路由守卫与 API client 单测可选，以构建与手工主路径为准
- 不做：全文 FTS、浏览器扩展、导入导出、分享页、OAuth

## [S3] Out of Scope

- 浏览器扩展 / 剪藏 API
- 全文索引（FTS5）、语义搜索
- 书签导入导出、Rabook/Instapaper 兼容
- 公开分享页、社交、点赞关注
- OAuth / 邮件验证 / 找回密码
- 多租户组织、团队共享
- Docker/K8s 生产部署清单（README 可写本地启动）
- 移动端 App / PWA 安装完整支持（Web 响应式即可）

## Tasks

- [ ] T1: 后端工程骨架（Spring Boot 3 + MyBatis-Plus + SQLite + 安全过滤器） — acceptance: `mvn test` 可启动上下文并连上 SQLite schema (covers: S2)
- [ ] T2: 用户注册/登录/JWT/me API — acceptance: 注册、登录、带 token 访问 me 的集成测试通过 (covers: S2; depends: T1)
- [ ] T3: 书签 CRUD、筛选、搜索 API 与用户隔离 — acceptance: 跨用户访问返回 404，筛选/搜索集成测试通过 (covers: S2; depends: T2)
- [ ] T4: 标签 CRUD 与书签打标 — acceptance: 同用户标签唯一，删除标签解绑关系 (covers: S2; depends: T3)
- [ ] T5: 链接元数据异步抓取 — acceptance: 给定 HTML fixture 能解析 title/description/favicon (covers: S2; depends: T1)
- [ ] T6: 前端工程骨架与鉴权页 — acceptance: 登录注册可拿到 token 并进入主列表，刷新后保持会话 (covers: S2; depends: T2)
- [ ] T7: 书签列表/筛选/搜索/添加 UI — acceptance: 可添加 URL 并看到列表与筛选生效 (covers: S2; depends: T6)
- [ ] T8: 书签操作与标签管理 UI — acceptance: 已读/收藏/归档/删除/打标/标签管理主路径可走通 (covers: S2; depends: T7)
- [ ] T9: README 本地启动说明 — acceptance: 按 README 能前后端联调跑通 (covers: S2)
- [ ] T10: 全量验证 — acceptance: 后端 `mvn test` 与前端 `npm run build` 均通过 (covers: S2; depends: T8, T9)
