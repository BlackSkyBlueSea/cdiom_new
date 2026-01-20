# CDIOM - 部署指南

本文档详细描述了CDIOM系统的部署说明，包含开发环境部署、生产环境部署、配置说明、Nginx配置等完整的部署指南。

## 目录

- [环境要求](#环境要求)
- [开发环境部署](#开发环境部署)
  - [后端部署](#后端部署)
  - [前端部署](#前端部署)
- [生产环境部署](#生产环境部署)
  - [后端部署](#后端部署-1)
  - [前端部署](#前端部署-1)
  - [Nginx配置](#nginx配置)
- [配置说明](#配置说明)
  - [数据库配置](#数据库配置)
  - [JWT配置](#jwt配置)
  - [邮箱配置](#邮箱配置)
  - [第三方API配置](#第三方api配置)
  - [环境变量配置](#环境变量配置)
- [系统服务配置](#系统服务配置)
- [常见问题](#常见问题)

---

## 环境要求

### 后端环境

- **JDK**: 17+
- **Maven**: 3.6+
- **MySQL**: 8.0+
- **操作系统**: Windows / Linux / macOS

### 前端环境

- **Node.js**: 18+
- **npm**: 9+ 或 **yarn**: 1.22+
- **操作系统**: Windows / Linux / macOS

### 生产环境推荐

- **服务器**: Linux (Ubuntu 20.04+ / CentOS 7+)
- **Web服务器**: Nginx 1.18+
- **应用服务器**: Java 17 (OpenJDK 或 Oracle JDK)
- **数据库**: MySQL 8.0+ (推荐使用主从复制)

---

## 开发环境部署

### 后端部署

#### 1. 环境准备

确保已安装以下软件：
- JDK 17+
- Maven 3.6+
- MySQL 8.0+

#### 2. 数据库初始化

执行数据库初始化脚本：

```bash
# 方式一：使用完整初始化脚本（推荐）
mysql -u root -p < cdiom_backend/src/main/resources/db/init_simple.sql

# 创建供应商-药品关联表（第20张表，v1.3.0新增）
mysql -u root -p < cdiom_backend/src/main/resources/db/create_supplier_drug_relation.sql

# 创建用户权限关联表和细粒度权限（v1.5.0新增）
mysql -u root -p < cdiom_backend/src/main/resources/db/add_user_permission_system.sql

# 初始化权限数据
mysql -u root -p < cdiom_backend/src/main/resources/db/init_permissions.sql

# 初始化超级管理员
mysql -u root -p < cdiom_backend/src/main/resources/db/init_super_admin.sql
```

#### 3. 配置文件设置

复制示例配置文件并填入实际配置：

```bash
cd cdiom_backend/src/main/resources
cp application.yml.example application-local.yml
```

编辑 `application-local.yml`，填入以下配置：
- 数据库用户名和密码
- JWT密钥（至少32字符）
- 邮箱用户名和授权码（如果需要使用邮箱验证功能）
- 第三方API密钥（如果需要使用第三方API功能）

#### 4. 启动服务

```bash
cd cdiom_backend
mvn clean install
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动

### 前端部署

#### 1. 安装依赖

```bash
cd cdiom_frontend
npm install
```

#### 2. 启动开发服务器

```bash
npm run dev
```

前端服务将在 `http://localhost:5173` 启动

#### 3. 配置代理（可选）

如果需要修改后端API地址，编辑 `vite.config.js`：

```javascript
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

---

## 生产环境部署

### 后端部署

#### 1. 打包应用

```bash
cd cdiom_backend
mvn clean package -DskipTests
```

打包完成后，会在 `target` 目录下生成 `cdiom_backend-1.0.0.jar` 文件。

#### 2. 配置文件准备

**方式一：使用 application-local.yml（推荐）**

```bash
# 在服务器上创建配置文件
cd cdiom_backend/src/main/resources
cp application.yml.example application-local.yml
# 编辑 application-local.yml，填入生产环境配置
```

**方式二：使用环境变量（更安全）**

设置环境变量（见下方"环境变量配置"章节）

#### 3. 运行应用

**方式一：直接运行**

```bash
java -jar cdiom_backend-1.0.0.jar
```

**方式二：后台运行（推荐）**

```bash
nohup java -jar cdiom_backend-1.0.0.jar > app.log 2>&1 &
```

**方式三：使用系统服务（推荐）**

见下方"系统服务配置"章节

#### 4. 验证部署

访问健康检查接口：

```bash
curl http://localhost:8080/api/v1/health
```

### 前端部署

#### 1. 构建生产版本

```bash
cd cdiom_frontend
npm run build
```

构建完成后，会在 `dist` 目录下生成生产版本文件。

#### 2. 部署到Web服务器

**方式一：使用Nginx（推荐）**

将 `dist` 目录内容复制到Nginx的网站根目录：

```bash
# 复制文件到Nginx目录
cp -r dist/* /usr/share/nginx/html/cdiom/
```

**方式二：使用其他Web服务器**

将 `dist` 目录内容部署到Apache、Tomcat等Web服务器。

#### 3. 配置API代理

编辑Nginx配置文件（见下方"Nginx配置"章节）

---

## Nginx配置

### 完整配置示例

```nginx
# 后端API代理
server {
    listen 80;
    server_name api.your-domain.com;
    
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket支持（用于日志推送）
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        
        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
}

# 前端静态文件
server {
    listen 80;
    server_name your-domain.com;
    root /path/to/cdiom_frontend/dist;
    index index.html;
    
    # 前端路由支持（React Router）
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    # API代理
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

### HTTPS配置（推荐）

```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;
    
    # SSL证书配置
    ssl_certificate /path/to/ssl/cert.pem;
    ssl_certificate_key /path/to/ssl/key.pem;
    
    # SSL优化配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    
    root /path/to/cdiom_frontend/dist;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

# HTTP重定向到HTTPS
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}
```

---

## 配置说明

### 数据库配置

在 `application.yml` 或 `application-local.yml` 中配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/cdiom_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your-db-password  # 生产环境建议使用环境变量
    hikari:
      minimum-idle: 10
      maximum-pool-size: 50
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

**生产环境建议：**
- 使用环境变量配置数据库密码
- 调整连接池大小根据实际并发量
- 启用SSL连接（useSSL=true）
- 配置主从复制提高可用性

### JWT配置

在 `application-local.yml` 中配置：

```yaml
jwt:
  secret: your-jwt-secret-key-minimum-32-characters  # 至少32字符，生产环境使用强密钥
  expiration: 28800000  # Token过期时间（毫秒），默认8小时
```

**安全建议：**
- 生产环境使用至少32字符的强随机密钥
- 定期更换JWT密钥（需要所有用户重新登录）
- 使用环境变量存储JWT密钥

### 邮箱配置

在 `application-local.yml` 中配置：

```yaml
spring:
  mail:
    username: your-email@163.com  # 163邮箱地址
    password: your-smtp-auth-code  # SMTP授权码（不是登录密码）
```

**获取163邮箱授权码：**
1. 登录163邮箱
2. 进入"设置" -> "POP3/SMTP/IMAP"
3. 开启SMTP服务
4. 获取授权码

### 第三方API配置

在 `application-local.yml` 中配置：

```yaml
# 万维易源API配置
yuanyanyao:
  api:
    app-key: your-yuanyanyao-api-key

# 极速数据API配置
jisuapi:
  api:
    app-key: your-jisuapi-key
```

**说明：**
- 如果不配置API密钥，系统仍可正常使用，但药品信息查询功能将无法调用第三方API
- 系统已实现本地数据库优先查询，减少API调用次数

### 环境变量配置

生产环境建议使用环境变量配置敏感信息：

**Linux/Mac:**

```bash
export MAIL_USERNAME="your-email@163.com"
export MAIL_PASSWORD="your-smtp-auth-code"
export DB_USERNAME="root"
export DB_PASSWORD="your-db-password"
export JWT_SECRET="your-jwt-secret-key-minimum-32-characters"
export YUANYANYAO_APP_KEY="your-api-key"
export JISUAPI_APP_KEY="your-api-key"
```

**Windows (PowerShell):**

```powershell
$env:MAIL_USERNAME="your-email@163.com"
$env:MAIL_PASSWORD="your-smtp-auth-code"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your-db-password"
$env:JWT_SECRET="your-jwt-secret-key-minimum-32-characters"
$env:YUANYANYAO_APP_KEY="your-api-key"
$env:JISUAPI_APP_KEY="your-api-key"
```

**在 application.yml 中引用环境变量：**

```yaml
spring:
  mail:
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
  datasource:
    username: ${DB_USERNAME:root}  # 默认值为root
    password: ${DB_PASSWORD}
jwt:
  secret: ${JWT_SECRET}
yuanyanyao:
  api:
    app-key: ${YUANYANYAO_APP_KEY:}
jisuapi:
  api:
    app-key: ${JISUAPI_APP_KEY:}
```

---

## 系统服务配置

### Linux系统服务（systemd）

创建服务文件 `/etc/systemd/system/cdiom.service`：

```ini
[Unit]
Description=CDIOM Backend Application
After=network.target mysql.service

[Service]
Type=simple
User=cdiom
WorkingDirectory=/opt/cdiom
ExecStart=/usr/bin/java -jar /opt/cdiom/cdiom_backend-1.0.0.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=cdiom

# 环境变量
Environment="MAIL_USERNAME=your-email@163.com"
Environment="MAIL_PASSWORD=your-smtp-auth-code"
Environment="DB_PASSWORD=your-db-password"
Environment="JWT_SECRET=your-jwt-secret-key"

[Install]
WantedBy=multi-user.target
```

**管理服务：**

```bash
# 启动服务
sudo systemctl start cdiom

# 停止服务
sudo systemctl stop cdiom

# 重启服务
sudo systemctl restart cdiom

# 查看服务状态
sudo systemctl status cdiom

# 开机自启
sudo systemctl enable cdiom

# 查看日志
sudo journalctl -u cdiom -f
```

### Windows系统服务

使用 NSSM (Non-Sucking Service Manager) 创建Windows服务：

```bash
# 下载NSSM：https://nssm.cc/download

# 安装服务
nssm install CDIOM "C:\Program Files\Java\jdk-17\bin\java.exe" "-jar C:\cdiom\cdiom_backend-1.0.0.jar"

# 启动服务
nssm start CDIOM

# 停止服务
nssm stop CDIOM

# 删除服务
nssm remove CDIOM
```

---

## 常见问题

### 1. 数据库连接失败

**问题**：应用启动时提示数据库连接失败

**解决方案**：
- 检查MySQL服务是否运行：`systemctl status mysql`
- 检查数据库配置是否正确（用户名、密码、数据库名）
- 检查防火墙是否开放3306端口
- 检查MySQL是否允许远程连接（生产环境）

### 2. 端口被占用

**问题**：8080端口已被占用

**解决方案**：
- 修改 `application.yml` 中的 `server.port` 配置
- 或使用 `java -jar app.jar --server.port=8081` 指定端口

### 3. 前端无法连接后端

**问题**：前端页面无法调用后端API

**解决方案**：
- 检查后端服务是否正常运行
- 检查 `vite.config.js` 中的代理配置
- 检查CORS配置（生产环境需要配置允许的域名）
- 检查Nginx配置（如果使用Nginx）

### 4. JWT Token过期

**问题**：Token过期后无法访问接口

**解决方案**：
- Token默认有效期为8小时，过期后需要重新登录
- 可以在 `application.yml` 中调整 `jwt.expiration` 配置
- 前端会自动检测Token过期并跳转到登录页

### 5. 文件上传失败

**问题**：文件上传时提示文件过大

**解决方案**：
- 检查 `application.yml` 中的文件大小限制配置
- 默认最大文件大小为10MB，可根据需要调整

### 6. 内存不足

**问题**：应用运行一段时间后内存不足

**解决方案**：
- 增加JVM堆内存：`java -Xms512m -Xmx1024m -jar app.jar`
- 检查是否有内存泄漏
- 调整数据库连接池大小

### 7. 日志文件过大

**问题**：日志文件占用磁盘空间过大

**解决方案**：
- 配置日志轮转（logback-spring.xml）
- 定期清理旧日志文件
- 使用日志收集工具（如ELK）

---

**文档版本**: v1.0.0  
**最后更新**: 2026年1月20日

