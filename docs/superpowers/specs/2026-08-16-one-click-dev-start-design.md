# Signal Notes 一键开发启动设计

## 1. 背景与目标

Signal Notes 当前需要分别启动 MySQL、Spring Boot 后端和 Vite 前端。目标是提供一个 Windows 本地双击入口，让开发者无需记忆命令即可进入可用的博客页面，同时保留热更新和独立服务日志。

本次功能只服务于本地开发，不改变 Docker 生产/完整容器运行方式，也不影响机器上已有的 Traefik 或其他服务。

## 2. 用户入口与成功标准

### 入口

- `start-blog.cmd`：推荐的双击入口。
- `start-blog.ps1`：可在 PowerShell 中直接执行，也承载全部启动逻辑。
- `stop-blog.cmd` / `stop-blog.ps1`：停止本次启动的前后端进程。

### 成功标准

双击 `start-blog.cmd` 后：

1. Docker 中的 `signal-notes-mysql` 处于 healthy；如果容器已健康则直接复用，不删除 volume。
2. Spring Boot 后端在 `127.0.0.1:8081` 启动，`/actuator/health` 返回 `UP`。
3. Vite 前端在 `127.0.0.1:5174` 启动，页面标题包含 `Signal Notes`。
4. 脚本自动打开 `http://127.0.0.1:5174/`，并在终端显示服务地址、日志位置和停止命令。
5. 再次执行启动脚本不会重复启动已有的 Signal Notes 服务。

## 3. 方案比较

### 方案 A：CMD 入口 + PowerShell 编排（推荐）

`start-blog.cmd` 只负责调用 `start-blog.ps1`，PowerShell 负责依赖检查、Docker、进程、健康检查和浏览器打开。优点是 Windows 双击体验好、无需额外运行时、可使用原生进程和端口 API；缺点是脚本需要处理 PowerShell 的进程树和转义规则。

### 方案 B：只提供 PowerShell 脚本

实现最少，但用户需要右键选择 PowerShell 或调整执行策略，双击体验不稳定，不作为默认入口。

### 方案 C：Node.js 启动器

跨平台和日志处理较方便，但会把 Node 作为额外启动依赖，且项目已有 pnpm/Vite，不需要再引入一套启动器运行时。

最终采用方案 A。

## 4. 运行拓扑与固定端口

```text
start-blog.cmd
  └─ start-blog.ps1
      ├─ docker compose up -d mysql
      ├─ mvn spring-boot:run  -> 127.0.0.1:8081
      └─ pnpm dev --host 127.0.0.1 --port 5174 --strictPort
```

脚本使用以下本地开发值：

| 服务 | 地址 | 启动参数 |
| --- | --- | --- |
| MySQL | `127.0.0.1:3307` | Compose 的 `mysql` 服务，使用现有 `.env` 的数据库凭据，并强制端口映射为 `3307:3306` |
| 后端 | `127.0.0.1:8081` | `DB_PORT=3307`、`SERVER_PORT=8081` |
| 前端 | `127.0.0.1:5174` | `pnpm dev --host 127.0.0.1 --port 5174 --strictPort` |

`8080` 明确列为不可触碰端口，因为当前由 Traefik 使用。脚本不会停止、重启或修改该端口上的进程。

## 5. 启动流程

### 5.1 前置检查

脚本检查 Docker Desktop、`docker compose`、Java 21、Maven、Node.js 和 pnpm 是否可调用，并验证 `frontend`、`backend` 和 `docker-compose.yml` 存在。缺少依赖时输出安装项和实际检测结果，然后保留终端窗口。

### 5.2 数据库

脚本执行等价于 `MYSQL_PORT=3307 docker compose up -d mysql`，随后轮询 `docker inspect` 的 health 状态，超时后输出容器最近日志。端口覆盖只作用于当前 Compose 命令，不会改写 `.env`。不会执行 `docker compose down -v`、删除 volume 或重建已有数据。

### 5.3 后端

启动前检查 `8081`：

- 如果本地 PID 记录对应的进程仍存在，且健康检查成功，则复用。
- 如果健康检查已经返回 `UP` 但没有本地 PID 记录，则视为外部已运行服务并复用，停止脚本不结束它。
- 如果端口被其他进程占用且不是可识别的 Signal Notes 后端，则报告 PID 和进程名并退出，不强制结束。
- 端口空闲时，从 `backend` 目录启动 Maven，并注入 `DB_PORT=3307`、`SERVER_PORT=8081`。

### 5.4 前端

启动前检查 `5174`：

- 如果本地 PID 记录对应的 Vite 进程仍存在且页面标题为 `Signal Notes`，则复用。
- 如果页面可访问但不是 Signal Notes，视为外部占用并退出，不覆盖其他项目。
- 端口空闲时，从 `frontend` 目录启动 pnpm，并启用 `--strictPort`，避免 Vite 静默换端口导致后端代理和浏览器地址不一致。

### 5.5 收尾

后端和前端都通过健康检查后，脚本打印：

- 博客地址：`http://127.0.0.1:5174/`
- 管理后台：`http://127.0.0.1:5174/admin`
- 后端健康地址：`http://127.0.0.1:8081/actuator/health`
- 日志目录：`.runtime/logs/`
- 停止命令：`stop-blog.cmd`

随后使用默认浏览器打开首页。浏览器打开失败不视为服务启动失败，终端会保留可复制的 URL。

## 6. 进程、日志与停止规则

启动器创建 `.runtime/logs/` 和 `.runtime/pids/`，这两个目录加入 `.gitignore`。每次服务分别写入：

- `.runtime/logs/backend.log`
- `.runtime/logs/backend-error.log`
- `.runtime/logs/frontend.log`
- `.runtime/logs/frontend-error.log`

PID 文件至少记录 PID、服务名、启动时间、工作目录和端口。停止脚本只读取由启动脚本创建且仍能通过工作目录/命令行校验的 PID，并使用 Windows 进程树结束对应 Maven/Java 或 pnpm/Node 进程。外部启动的服务、MySQL 容器和 `8080` 上的 Traefik 不在停止范围内。

如果 PID 文件过期或进程已退出，停止脚本只清理记录，不尝试结束新的复用 PID。启动脚本会在每次运行前清理过期 PID 记录。

## 7. 错误处理与用户体验

- 任一依赖缺失、Docker 启动失败、端口冲突或健康检查超时都会返回非零退出码。
- 错误信息包含服务名、端口、检测结果和下一步建议。
- 启动脚本不隐藏失败终端；成功服务的输出进入日志文件，避免双击后弹出多个不可控窗口。
- 支持 `-NoBrowser` 参数，便于自动化验证或远程环境使用。
- 支持 `-SkipMysql` 参数，仅在用户明确知道 MySQL 已由其他方式提供时使用；默认仍检查并启动 Compose 的 mysql 服务。

## 8. 验证范围

实现后至少验证：

1. PowerShell 语法检查和脚本帮助输出。
2. 冷启动：MySQL 已存在但前后端未启动时，一键启动三项检查均通过。
3. 热启动：重复执行时复用已有服务，不产生重复进程。
4. 端口冲突：占用 `5174` 或 `8081` 时能报告并退出，不影响占用者。
5. 停止：停止脚本只结束本次启动的前后端，MySQL 和 `8080` 保持运行。
6. 浏览器入口、后端健康检查和前端页面标题检查通过。
7. README 使用说明与实际文件名、端口和日志路径一致。

## 9. 非目标

- 不把开发启动器改造成生产部署工具。
- 不自动安装 Docker、Java、Maven、Node.js 或 pnpm。
- 不改变 Compose 默认生产端口映射，不停止 Traefik。
- 不清理数据库、上传文件、备份文件或 Docker volumes。
