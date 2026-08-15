# 脉冲笔记 / Signal Notes

一个先介绍页、再进入博客阅读体验的科技内容站点。前端使用 Vue 3 + Vite，后端使用 Spring Boot 3 + JPA，数据库使用 MySQL 8.4，迁移由 Flyway 管理。

## 本地开发

```powershell
# 前端
cd frontend
pnpm install
pnpm dev

# 后端（需要本机 MySQL，或先启动 Compose 的 mysql 服务）
cd backend
mvn spring-boot:run
```

默认入口：[http://127.0.0.1:5173/](http://127.0.0.1:5173/)，后台：[http://127.0.0.1:5173/admin](http://127.0.0.1:5173/admin)。若 5173 被占用，Vite 会选择下一可用端口。演示管理员为 `admin` / `signal2026`，生产环境必须通过 `ADMIN_USERNAME`、`ADMIN_PASSWORD` 覆盖。

## 一键运行

复制 `.env.example` 为 `.env` 并修改密码，然后运行：

```powershell
docker compose up --build
```

浏览器访问 `http://127.0.0.1:4173/`。首次启动时 Flyway 会自动创建 `signal_notes` 表并写入演示文章。停止服务使用 `docker compose down`；需要连数据一起清理时才使用 `docker compose down -v`。

## 测试与构建

```powershell
cd frontend; pnpm build
cd ../backend; mvn test
```

公开 API 位于 `/api/posts`、`/api/comments`、`/api/contact` 和 `/api/subscriptions`；`/api/admin/**` 使用 HTTP Basic Auth。上传只接受经过文件签名校验的 JPG、PNG、WebP，最大 10MB，媒体与备份分别保存到独立卷。完整需求覆盖与边界见 [BLOG_COVERAGE.md](BLOG_COVERAGE.md)。
