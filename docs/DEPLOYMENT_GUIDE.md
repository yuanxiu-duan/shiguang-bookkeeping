# 拾光账本 部署运维手册

| 项目 | 内容 |
|------|------|
| 文档版本 | V1.1 |
| 编写日期 | 2026-07-01 |
| 适用版本 | App v1.0.4 / Web v1.0 |

---

## 1. 环境要求

### 1.1 硬件要求

| 环境 | CPU | 内存 | 磁盘 | 说明 |
|------|-----|------|------|------|
| 开发 | 4 核 | 8 GB | 50 GB | 本地开发 |
| 测试 | 2 核 | 4 GB | 50 GB | 测试环境 |
| 生产（小型） | 2 核 | 4 GB | 100 GB | < 1000 用户 |
| 生产（中型） | 4 核 | 8 GB | 200 GB | < 10000 用户 |

### 1.2 软件要求

| 软件 | 版本 | 用途 |
|------|------|------|
| JDK | 21 (LTS) | 后端运行环境 |
| Maven | 3.9+ | 后端构建 |
| MySQL | 8.0+ | 数据库 |
| Node.js | 18+ | 前端构建 |
| npm/pnpm | npm 9+ / pnpm 8+ | 前端包管理 |
| Nginx | 1.20+ | 反向代理 + 静态资源 |
| Docker | 24+ | 容器化部署（可选） |
| Docker Compose | 2.20+ | 容器编排（可选） |

### 1.3 JDK 安装路径（Windows 开发机）
```
C:\Program Files\Eclipse Adoptium\jdk-21.0.6.7-hotspot
```

### 1.4 Maven 配置路径
```
<你的 Maven settings.xml 路径>
```

---

## 2. 后端部署（jz-server）

### 2.1 本地开发部署

#### 2.1.1 准备数据库
```sql
CREATE DATABASE IF NOT EXISTS moneykeeper DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### 2.1.2 配置数据库连接
编辑 `jz-server/src/main/resources/application-local.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://<host>:<port>/moneykeeper?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: <your-password>
```

#### 2.1.3 启动后端

**方式一：双击启动脚本**
```
双击根目录 start-server.bat
```

**方式二：PowerShell 启动**
```powershell
& "$PWD\start-server.bat"
# 或
Start-Process .\start-server.bat
```

**方式三：Maven 命令**
```powershell
cd jz-server
mvn spring-boot:run -s "<你的 Maven settings.xml 路径>"
```

#### 2.1.4 验证启动
- 访问 `http://localhost:8070/api/auth/public-key`
- 返回 RSA 公钥 JSON 即启动成功

### 2.2 生产部署

#### 2.2.1 打包
```powershell
cd jz-server
mvn clean package -s "<你的 Maven settings.xml 路径>" -DskipTests
```
产物：`jz-server/target/jz-server-1.0.0.jar`

#### 2.2.2 配置环境变量
```bash
export SPRING_DATASOURCE_URL="jdbc:mysql://<host>:3306/moneykeeper?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
export SPRING_DATASOURCE_USERNAME="root"
export SPRING_DATASOURCE_PASSWORD="<password>"
export APP_JWT_SECRET="<your-jwt-secret>"
export APP_JWT_EXPIRATION="86400000"
export APP_CORS_ALLOWED_ORIGINS="https://your-domain.com"
```

#### 2.2.3 启动
```bash
java -Xms256m -Xmx512m -jar jz-server-1.0.0.jar --spring.profiles.active=prod
```

#### 2.2.4 后台运行（Linux）
```bash
nohup java -Xms256m -Xmx512m -jar jz-server-1.0.0.jar --spring.profiles.active=prod > /var/log/jz-server.log 2>&1 &
```

#### 2.2.5 systemd 服务（推荐）
创建 `/etc/systemd/system/jz-server.service`：
```ini
[Unit]
Description=MoneyKeeper Server
After=network.target mysql.service

[Service]
Type=simple
User=www
WorkingDirectory=/opt/jz-server
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar jz-server-1.0.0.jar --spring.profiles.active=prod
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启用：
```bash
sudo systemctl daemon-reload
sudo systemctl enable jz-server
sudo systemctl start jz-server
sudo systemctl status jz-server
```

### 2.3 Docker 部署

#### 2.3.1 构建镜像
```bash
cd jz-server
docker build -t jz-server:1.0.0 .
```

#### 2.3.2 单容器运行
```bash
docker run -d \
  --name jz-server \
  -p 8070:8070 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host:3306/moneykeeper?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=<password> \
  -e APP_JWT_SECRET=<secret> \
  -v /opt/jz-server/exports:/app/exports \
  jz-server:1.0.0
```

#### 2.3.3 Docker Compose 部署
项目根目录已有 `docker-compose.yml`，启动：
```bash
docker-compose up -d
```
将启动 MySQL + 后端两个容器。

---

## 3. Web 管理端部署（jz-web）

### 3.1 本地开发
```powershell
cd jz-web
npm install
npm run dev
```
访问 `http://localhost:6173`

### 3.2 生产构建
```powershell
cd jz-web
npm install
npm run build
```
产物：`jz-web/dist/`

### 3.3 Nginx 部署

#### 3.3.1 复制静态资源
```bash
cp -r jz-web/dist/* /var/www/jz-web/
```

#### 3.3.2 Nginx 配置
参考 `jz-web/nginx.conf`：
```nginx
server {
    listen 80;
    server_name your-domain.com;  # 修改为你的域名

    root /var/www/jz-web;
    index index.html;

    # 前端路由
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理
    location /api/ {
        proxy_pass http://127.0.0.1:8070/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_read_timeout 120s;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    # Gzip 压缩
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml text/javascript image/svg+xml;
    gzip_min_length 1024;
}
```

#### 3.3.3 HTTPS 配置（推荐）
```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;

    # ... 其余配置同上
}

# HTTP 跳转 HTTPS
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$host$request_uri;
}
```

#### 3.3.4 重载 Nginx
```bash
sudo nginx -t          # 测试配置
sudo nginx -s reload   # 重载
```

---

## 4. uni-app 端构建（jz-app）

### 4.1 H5 构建
```powershell
cd jz-app
npm install
npm run build:h5
```
产物：`jz-app/dist/build/h5/`
部署：将产物上传到服务器，Nginx 配置同 Web 端。

### 4.2 App 构建（HBuilderX）

1. 使用 HBuilderX 打开 `jz-app` 目录
2. 菜单：发行 → 原生 App-云打包
3. 选择 Android/iOS 平台
4. 配置签名证书
5. 提交打包
6. 产物：`jz-app/unpackage/release/apk/*.apk`

### 4.3 微信小程序构建

1. HBuilderX 打开 `jz-app`
2. 菜单：发行 → 小程序-微信
3. 填入微信小程序 AppID
4. 提交后会在 `unpackage/dist/build/mp-weixin` 生成代码
5. 用微信开发者工具打开该目录
6. 上传 → 微信公众平台提交审核

### 4.4 配置后端地址

编辑 `jz-app/utils/config.js`：
```javascript
const API_BASE_URL = 'https://your-domain.com/api'
```

---

## 5. 数据库部署

### 5.1 创建数据库
```sql
CREATE DATABASE IF NOT EXISTS moneykeeper DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'moneykeeper'@'%' IDENTIFIED BY '<password>';
GRANT ALL PRIVILEGES ON moneykeeper.* TO 'moneykeeper'@'%';
FLUSH PRIVILEGES;
```

### 5.2 初始化数据
首次启动后端会自动执行：
- `init.sql` 中的默认分类（13 条）
- Flyway 迁移脚本（V100/V200/V300）
- `AdminInitializer` 自动创建管理员账号

### 5.3 数据库备份

#### 5.3.1 手动备份
```bash
mysqldump -u root -p moneykeeper > moneykeeper_$(date +%Y%m%d).sql
```

#### 5.3.2 定时备份（crontab）
```bash
# 每日凌晨 3 点备份
0 3 * * * mysqldump -u root -p<password> moneykeeper > /backup/moneykeeper_$(date +\%Y\%m\%d).sql

# 保留 30 天
0 4 * * * find /backup -name "moneykeeper_*.sql" -mtime +30 -delete
```

### 5.4 数据恢复
```bash
mysql -u root -p moneykeeper < moneykeeper_20260625.sql
```

---

## 6. 配置说明

### 6.1 后端配置项

| 配置项 | 环境变量 | 默认值 | 说明 |
|--------|----------|--------|------|
| server.port | - | 8070 | 服务端口 |
| spring.datasource.url | SPRING_DATASOURCE_URL | - | 数据库连接 |
| spring.datasource.username | SPRING_DATASOURCE_USERNAME | - | 数据库用户 |
| spring.datasource.password | SPRING_DATASOURCE_PASSWORD | - | 数据库密码 |
| app.jwt.secret | APP_JWT_SECRET | - | JWT 密钥 |
| app.jwt.expiration | APP_JWT_EXPIRATION | 86400000 | JWT 有效期(ms) |
| app.cors.allowed-origins | APP_CORS_ALLOWED_ORIGINS | - | CORS 白名单 |
| app.export.output-dir | - | exports | 导出目录 |
| app.export.schedule-delay-ms | - | 60000 | 导出任务间隔(ms) |
| app.export.max-tasks-per-run | - | 1 | 单次处理任务数 |
| app.export.zip-threshold-bytes | - | 10485760 | zip 压缩阈值 |
| spring.mail.host | - | smtp.qq.com | SMTP 主机 |
| spring.mail.port | - | 587 | SMTP 端口 |
| spring.mail.username | - | - | SMTP 用户名 |
| spring.mail.password | - | - | SMTP 授权码 |

### 6.2 邮件配置（QQ 邮箱示例）
```yaml
spring:
  mail:
    host: smtp.qq.com
    port: 587
    username: your-qq@qq.com
    password: <授权码>  # 非 QQ 密码，需在 QQ 邮箱设置中开启 SMTP 并获取授权码
    default-encoding: UTF-8
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

### 6.3 管理员初始化
启动时 `AdminInitializer` 自动创建管理员账号，配置在 `application.yml`：
```yaml
app:
  admin:
    username: admin
    password: <initial-password>
    email: admin@example.com
```

---

## 7. 运维操作

### 7.1 日志查看

#### 后端日志
```bash
# systemd
journalctl -u jz-server -f

# Docker
docker logs -f jz-server

# 文件日志
tail -f /var/log/jz-server.log
```

#### Nginx 日志
```bash
tail -f /var/log/nginx/access.log
tail -f /var/log/nginx/error.log
```

### 7.2 服务管理

```bash
# 启动
sudo systemctl start jz-server

# 停止
sudo systemctl stop jz-server

# 重启
sudo systemctl restart jz-server

# 状态
sudo systemctl status jz-server
```

### 7.3 健康检查
```bash
# 检查端口
netstat -tlnp | grep 8070

# 检查接口
curl http://localhost:8070/api/auth/public-key
```

### 7.4 数据库维护

#### 查看连接数
```sql
SHOW STATUS LIKE 'Threads_connected';
SHOW PROCESSLIST;
```

#### 慢查询日志
```sql
SHOW VARIABLES LIKE 'slow_query%';
SHOW VARIABLES LIKE 'long_query_time';
```

#### 清理过期验证码
```sql
DELETE FROM verification_codes 
WHERE expired_at < DATE_SUB(NOW(), INTERVAL 1 DAY) OR used = TRUE;
```

---

## 8. 升级流程

### 8.1 后端升级
```bash
# 1. 备份
mysqldump -u root -p moneykeeper > backup_$(date +%Y%m%d).sql

# 2. 拉取新代码
git pull origin main

# 3. 重新打包
cd jz-server
mvn clean package -DskipTests

# 4. 重启服务
sudo systemctl restart jz-server
```

### 8.2 前端升级
```bash
# 1. 拉取新代码
git pull origin main

# 2. 重新构建
cd jz-web
npm install
npm run build

# 3. 部署
cp -r dist/* /var/www/jz-web/

# 4. 无需重启 Nginx（静态资源）
```

### 8.3 数据库升级
Flyway 会在后端启动时自动执行新的迁移脚本，无需手动操作。

---

## 9. 故障排查

### 9.1 常见问题

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 启动失败：数据库连接拒绝 | MySQL 未启动或配置错误 | 检查 MySQL 状态与连接参数 |
| 401 未登录 | token 过期或无效 | 重新登录获取新 token |
| 接口 403 | 权限不足 | 检查用户角色 |
| CORS 错误 | 跨域未配置 | 检查 app.cors.allowed-origins |
| 邮件发送失败 | SMTP 配置错误 | 检查授权码与端口 |
| Flyway 报错 | 迁移脚本冲突 | 检查 V*.sql 文件 |
| 文件上传失败 | 超过 500MB | 调整 multipart 限制 |
| 导出任务不执行 | 定时任务未启动 | 检查后端日志 |

### 9.2 端口占用
```bash
# Windows
netstat -ano | findstr :8070
taskkill /F /PID <PID>

# Linux
lsof -i :8070
kill -9 <PID>
```

### 9.3 内存溢出
```bash
# 调整 JVM 参数
java -Xms512m -Xmx1024m -XX:+HeapDumpOnOutOfMemoryError -jar jz-server-1.0.0.jar
```

---

## 10. 安全加固清单

| 项目 | 检查项 | 状态 |
|------|--------|------|
| JWT 密钥 | 使用强随机密钥，不使用默认值 | □ |
| 数据库密码 | 使用强密码，不使用默认值 | □ |
| CORS 白名单 | 仅包含必要域名 | □ |
| HTTPS | 启用 HTTPS，禁用 HTTP | □ |
| 防火墙 | 仅开放 80/443 端口 | □ |
| MySQL 端口 | 不对外暴露 3306 | □ |
| 管理员密码 | 修改默认管理员密码 | □ |
| 文件权限 | jar 包与配置文件权限 600 | □ |
| 日志脱敏 | 敏感信息不输出到日志 | □ |
| 定期备份 | 数据库每日备份 | □ |

---

## 11. 监控建议

### 11.1 基础监控
- CPU / 内存 / 磁盘使用率
- 网络流量
- 进程状态

### 11.2 应用监控
- 接口响应时间
- 错误率
- JVM 堆内存
- 数据库连接数

### 11.3 业务监控
- 注册用户数
- 日活/月活
- 交易记录数
- 导出任务积压数

---

## 12. 相关文档
- [系统架构设计](./SYSTEM_ARCHITECTURE.md)
- [数据库设计](./DATABASE_DESIGN.md)
- [接口文档](./API_SPECIFICATION.md)
- [项目交付清单](./DELIVERABLES.md)
