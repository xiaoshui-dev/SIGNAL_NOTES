# Signal Notes 需求覆盖矩阵

这份矩阵以 `BLOG_REQUIREMENTS.md` 的 P0/P1 和“按界面的验收用例”为发布门槛。每一项都对应真实路由、API、数据库迁移或自动化检查；P2 仅列为后续扩展，不伪装成首发能力。

| 需求域 | 实现位置 | 验证方式 |
| --- | --- | --- |
| 介绍页 / 科技首屏 / 动效 / 主题 | `frontend/src/views/LandingView.vue`, `frontend/src/assets/styles.css` | 浏览器桌面 + 375px 截图、键盘路径 |
| 博客列表 / 分类筛选 / 搜索 / 订阅 | `frontend/src/views/BlogView.vue`, `frontend/src/api.js`, `/api/posts`, `/api/subscriptions` | 前端构建、API 集成测试、浏览器交互 |
| 文章 Markdown / XSS 清理 / 目录 / 分享海报 | `frontend/src/views/ArticleView.vue`, `frontend/src/components/SharePoster.vue` | DOMPurify 单元路径、海报尺寸/二维码检查 |
| 分类、标签、归档、作者、关于、联系、隐私 | `frontend/src/router.js`, `BlogView.vue`, `backend/src/main/java/.../controller` | 路由 smoke test、空/错误状态检查 |
| 404/403/500/维护状态 | `frontend/src/views/StatusView.vue`, 路由兜底 | 直接访问异常路由、API 失败状态 |
| 管理登录与最小权限 | `SecurityConfig.java`, `AdminView.vue` | 未授权 401、Basic Auth、登录失败交互 |
| 文章草稿、审核、发布、下线、回收站、定时、置顶 | `Post.java`, `PostService.java`, `AdminView.vue`, Flyway V3+ | 状态流转测试、后台交互 |
| 文章预览、版本、恢复、自动保存、发布检查 | `AdminView.vue`, `PostRevision.java`, revision API | 前端状态测试、集成 API |
| 分类/标签管理 | 管理 API 与 `AdminTaxonomyView.vue` | CRUD 集成测试、重复值错误 |
| 媒体上传与安全校验 | `MediaController.java`, `AdminView.vue`, Nginx 配置 | MIME/大小校验、上传 API |
| 评论审核、回复、举报、反馈工单 | 评论/联系 API 与后台队列 | 状态变更集成测试、表单错误状态 |
| 仪表盘趋势、日志、任务、备份 | 管理 dashboard/log/task/backup API | API smoke test、备份校验状态 |
| SEO / RSS / sitemap / robots | `index.html`, `public/*.xml`, 动态 metadata helper | 构建产物检查、页面 head 检查 |
| 响应式、无障碍、错误与加载状态 | 全局样式和各视图 | 375px 无溢出、Tab/focus、浏览器 console |

## 仍属于 P2 的明确范围

多作者审批编排、2FA、专业全文搜索、国际化、多站点、推荐系统、PWA、点赞/收藏持久化和自动摘要需要独立的账户、索引或产品决策，不把本地演示状态标为已完成。

