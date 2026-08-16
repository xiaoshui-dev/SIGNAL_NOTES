# Signal Notes 宝塔部署教程

本部署包适用于宝塔 Linux 面板，生产结构为：Nginx 托管 Vue 静态文件并反向代理 `/api` 与 `/uploads`，Java 21 运行 Spring Boot，MySQL 8 保存数据。

## 一、部署包内容

```text
signal-notes-baota/
├─ frontend/                         前端生产静态文件
├─ backend/signal-notes-blog.jar     后端可执行程序
├─ database/signal_notes.sql         当前内容的脱敏数据库备份
├─ uploads/                          媒体上传目录
├─ config/
│  ├─ signal-notes.env.example       生产环境变量模板
│  ├─ signal-notes.service           systemd 服务模板
│  └─ nginx-signal-notes.conf        Nginx 站点模板
├─ README-宝塔部署.md
├─ VERSION.txt
└─ SHA256SUMS.txt
```

数据库备份保留文章、分类、标签、站点文案和媒体元数据。为避免把本地隐私数据带到服务器，备份不包含 SMTP 设置、后台用户、联系表单、订阅记录、评论和审计/备份任务。首次启动会按照生产环境变量创建管理员；SMTP 请在上线后从后台重新配置。

## 二、服务器准备

建议最低配置为 2 核 CPU、2 GB 内存、20 GB 磁盘。推荐使用 Ubuntu 22.04/24.04、Debian 12 或 Rocky Linux 9。

1. 在宝塔“软件商店”安装 Nginx 1.24+ 和 MySQL 8.0。
2. 安装 Java 21。可使用宝塔 Java 项目管理器安装 OpenJDK 21，也可通过系统包管理器安装。
3. 在“安全”中只放行 `22`、`80`、`443`。不要对公网开放 `3306` 和 `8080`。
4. 将域名解析到服务器公网 IP。

在终端确认 Java 路径：

```bash
command -v java
java -version
```

`signal-notes.service` 默认使用 `/usr/bin/java`。如果 `command -v java` 返回其他路径，请同步修改服务文件的 `ExecStart`。

## 三、上传并解压

在宝塔“文件”中进入 `/www/wwwroot`，上传压缩包并解压，最终目录必须是：

```text
/www/wwwroot/signal-notes/
```

确认以下文件存在：

```bash
test -f /www/wwwroot/signal-notes/frontend/index.html
test -f /www/wwwroot/signal-notes/backend/signal-notes-blog.jar
test -f /www/wwwroot/signal-notes/database/signal_notes.sql
```

设置运行目录和权限：

```bash
mkdir -p /www/wwwroot/signal-notes/uploads
mkdir -p /www/backup/signal-notes
mkdir -p /www/server/signal-notes
chown -R www:www /www/wwwroot/signal-notes /www/backup/signal-notes
chmod 750 /www/wwwroot/signal-notes/uploads /www/backup/signal-notes
```

## 四、创建并导入数据库

1. 在宝塔“数据库”中新增数据库。
2. 数据库名和用户名建议都填 `signal_notes`。
3. 密码使用宝塔生成的强密码，访问权限选择“本地服务器”。
4. 点击数据库右侧“导入”，选择 `database/signal_notes.sql`。

也可以使用终端导入：

```bash
mysql -u signal_notes -p signal_notes < /www/wwwroot/signal-notes/database/signal_notes.sql
```

导入后检查：

```bash
mysql -u signal_notes -p -D signal_notes -e "SELECT COUNT(*) AS posts FROM posts;"
```

## 五、配置生产环境

复制环境变量模板：

```bash
cp /www/wwwroot/signal-notes/config/signal-notes.env.example /www/server/signal-notes/signal-notes.env
chmod 600 /www/server/signal-notes/signal-notes.env
chown root:root /www/server/signal-notes/signal-notes.env
```

用宝塔文件编辑器打开 `/www/server/signal-notes/signal-notes.env`，至少修改：

- `DB_PASSWORD`：宝塔中创建的数据库密码。
- `ADMIN_USERNAME`：后台登录名。
- `ADMIN_PASSWORD`：至少 12 位的随机强密码。
- `APP_PUBLIC_URL`：最终站点地址，例如 `https://blog.example.com`，结尾不要加 `/`。
- `CORS_ORIGINS`：与 `APP_PUBLIC_URL` 相同；多域名时用英文逗号分隔。

不要把该文件放在网站公开目录，也不要将真实密码写回 `signal-notes.env.example`。

## 六、启动后端

安装 systemd 服务：

```bash
cp /www/wwwroot/signal-notes/config/signal-notes.service /etc/systemd/system/signal-notes.service
systemctl daemon-reload
systemctl enable --now signal-notes
systemctl status signal-notes --no-pager
```

检查健康状态：

```bash
curl --fail http://127.0.0.1:8080/actuator/health
```

正常返回应包含 `"status":"UP"`。启动失败时查看日志：

```bash
journalctl -u signal-notes -n 200 --no-pager
```

注意：数据库中如果已经存在与 `ADMIN_USERNAME` 相同的用户，环境变量不会覆盖其既有密码。此部署包已经移除本地用户数据，因此首次启动会使用你配置的生产密码创建管理员。

## 七、创建宝塔网站并配置 Nginx

1. 在宝塔“网站”中新增站点，填写域名，根目录设为 `/www/wwwroot/signal-notes/frontend`。
2. PHP 版本选择“纯静态”。
3. 打开站点“配置文件”。
4. 以 `config/nginx-signal-notes.conf` 为参考，将其中 `server_name blog.example.com` 改成真实域名。
5. 宝塔通常会生成自己的 SSL 和日志配置。保留宝塔生成的 `ssl_certificate`、`ssl_certificate_key`、日志路径和 SSL 跳转，只需要确保模板中的 `root`、`location /`、`location /api/`、`location /uploads/`、安全响应头和缓存规则存在。
6. 保存后在终端检查并重载：

```bash
nginx -t
nginx -s reload
```

不要把 `/api` 代理到公网 IP；后端只监听 `127.0.0.1:8080`。

## 八、配置 HTTPS

1. 打开站点“SSL”，选择 Let's Encrypt。
2. 申请证书后开启“强制 HTTPS”。
3. 确认 `/www/server/signal-notes/signal-notes.env` 中 `APP_PUBLIC_URL` 和 `CORS_ORIGINS` 均为 `https://` 地址。
4. 修改环境变量后重启后端：

```bash
systemctl restart signal-notes
```

## 九、上线验收

依次检查：

```text
https://你的域名/                         介绍页
https://你的域名/blog                    博客列表
https://你的域名/admin                   后台登录
https://你的域名/api/posts               公开文章接口
https://你的域名/actuator/health          不应由 Nginx 对公网暴露
```

在后台完成以下动作：

1. 使用生产管理员登录并立即确认密码已妥善保管。
2. 在“设置”中重新填写 SMTP 授权码并发送测试邮件。
3. 新建一篇草稿、发布，再从分类页和文章页确认可见。
4. 上传一张图片，确认 `/uploads/...` 可以访问。
5. 生成分享海报，确认二维码指向正式 HTTPS 域名。

## 十、日常运维

常用命令：

```bash
systemctl status signal-notes --no-pager
systemctl restart signal-notes
journalctl -u signal-notes -f
curl --fail http://127.0.0.1:8080/actuator/health
```

更新程序时，先在宝塔中备份数据库和 `/www/wwwroot/signal-notes/uploads`，再替换 `frontend/` 与 `backend/signal-notes-blog.jar`，最后执行：

```bash
systemctl restart signal-notes
nginx -t && nginx -s reload
```

建议每天备份 MySQL 和上传目录，至少保留 7 个日备份和 4 个周备份。数据库备份、环境变量和上传目录都不应放入前端公开目录。

## 十一、常见问题

### 页面刷新后 404

Nginx 的 `location /` 必须包含：

```nginx
try_files $uri $uri/ /index.html;
```

### 页面能打开但没有文章

检查数据库是否导入、后端是否正常，以及 Nginx 是否正确代理 `/api/`：

```bash
curl -i http://127.0.0.1:8080/api/posts
curl -i https://你的域名/api/posts
```

### 上传失败或图片 404

检查目录权限与反向代理：

```bash
chown -R www:www /www/wwwroot/signal-notes/uploads
systemctl restart signal-notes
```

### 后端无法连接 MySQL

核对数据库名称、用户名、密码和 `DB_HOST=127.0.0.1`，并确认 MySQL 正在运行：

```bash
systemctl status mysqld --no-pager || systemctl status mysql --no-pager
```

### 修改环境变量后没有生效

环境变量由 systemd 在进程启动时读取，修改后必须执行：

```bash
systemctl restart signal-notes
```
