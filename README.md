# Linkstash

自托管「稍后读」书签站：保存链接、自动抓取标题/摘要/站点图标，用标签与搜索管理阅读队列。多用户隔离。

- Author: [kafeiq19](https://github.com/kafeiq19)

## Stack

- **Frontend** — Vue 3 · Vite · TypeScript · Pinia · Element Plus · Tailwind CSS
- **Backend** — Java 17 · Spring Boot 3 · MyBatis-Plus · SQLite · JWT

## Structure

```
linkstash/
  backend/    # Spring Boot API
  frontend/   # Vue 3 SPA
  docs/       # compose spec
```

## Quick start

### Backend

```bash
cd backend
mvn spring-boot:run
# API: http://localhost:8080
```

SQLite 数据文件默认在 `backend/data/linkstash.db`（首次启动自动建表）。

### Frontend

```bash
cd frontend
npm install
npm run dev
# UI: http://localhost:5173  (代理 /api → 8080)
```

### Tests

```bash
cd backend && mvn test
cd frontend && npm run build
```

## API overview

Base `/api`，JSON 包体 `{code, message, data}`。除 `/api/auth/*` 外需 `Authorization: Bearer <jwt>`。

| Method | Path | 说明 |
| --- | --- | --- |
| POST | `/api/auth/register` | 注册 |
| POST | `/api/auth/login` | 登录 |
| GET | `/api/auth/me` | 当前用户 |
| GET | `/api/bookmarks` | 列表（page/size/status/tag/q/favorite） |
| POST | `/api/bookmarks` | 保存 URL |
| GET | `/api/bookmarks/{id}` | 详情 |
| PATCH | `/api/bookmarks/{id}` | 更新状态/收藏/标题/笔记 |
| DELETE | `/api/bookmarks/{id}` | 删除 |
| GET/POST/PATCH/DELETE | `/api/tags`… | 标签管理 |

## License

MIT
