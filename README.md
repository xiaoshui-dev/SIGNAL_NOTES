# 脉冲笔记 / Signal Notes

一个先介绍页、再进入博客阅读体验的科技内容站点。前端使用 Vue 3 + Vite，后端使用 Spring Boot 3 + JPA，数据库使用 MySQL 8.4，迁移由 Flyway 管理。

## 一键开发启动（推荐）

在 Windows 资源管理器中双击项目根目录的 `start-blog.cmd`。脚本会检查 Docker、Java、Maven、Node.js 和可用的前端包管理器，启动或复用 MySQL、Spring Boot 和 Vite，并在服务就绪后打开浏览器。

前端启动优先使用 Node.js 自带的 npm；如果 npm 不可用但系统安装了 pnpm，脚本才会使用 pnpm，不需要额外安装 pnpm。

开发模式固定使用以下地址：

- 博客：`http://127.0.0.1:5174/`
- 后台：`http://127.0.0.1:5174/admin`
- 后端健康检查：`http://127.0.0.1:8081/actuator/health`
- MySQL：`127.0.0.1:3307`

停止本次启动的前后端：

```powershell
.\stop-blog.cmd
```

停止脚本不会删除数据，不会停止 MySQL，也不会触碰 `8080` 上已有的 Traefik。运行日志位于 `.runtime/logs/`；该目录不会提交到 Git。

也可以在 PowerShell 中运行：

```powershell
.\start-blog.ps1 -NoBrowser   # 启动但不自动打开浏览器
.\start-blog.ps1 -SkipMysql   # MySQL 已由其他方式启动时使用
```

如果 `8081` 或 `5174` 被其他程序占用，脚本会显示进程信息并退出，不会强制结束占用者。

## 手动本地开发

```powershell
# 前端
cd frontend
npm install
npm run dev -- --host 127.0.0.1 --port 5174 --strictPort

# 后端（需要本机 MySQL，或先启动 Compose 的 mysql 服务）
cd backend
$env:DB_PORT="3307"
$env:SERVER_PORT="8081"
mvn spring-boot:run
```

开发入口是：[http://127.0.0.1:5174/](http://127.0.0.1:5174/)，后台：[http://127.0.0.1:5174/admin](http://127.0.0.1:5174/admin)。演示管理员为 `admin` / `signal2026`，生产环境必须通过 `ADMIN_USERNAME`、`ADMIN_PASSWORD` 覆盖。

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
