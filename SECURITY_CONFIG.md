# 安全配置说明

## ⚠️ 重要安全提示

**请勿将包含敏感信息的配置文件提交到 Git 仓库！**

## 配置方式

### 方式1：使用 application-local.yml（推荐）

1. 复制示例配置文件：
   ```bash
   cp cdiom_backend/src/main/resources/application.yml.example cdiom_backend/src/main/resources/application-local.yml
   ```

2. 编辑 `application-local.yml`，填入实际的配置值：
   - SMTP 邮箱用户名和授权码
   - 数据库密码
   - JWT 密钥
   - API 密钥等

3. `application-local.yml` 已被 `.gitignore` 排除，不会被提交到 Git

### 方式2：使用环境变量

在运行应用前设置环境变量：

**Windows (PowerShell):**
```powershell
$env:MAIL_USERNAME="your-email@163.com"
$env:MAIL_PASSWORD="your-smtp-auth-code"
$env:DB_PASSWORD="your-db-password"
$env:JWT_SECRET="your-jwt-secret-key"
$env:YUANYANYAO_APP_KEY="your-api-key"
$env:JISUAPI_APP_KEY="your-api-key"
```

**Linux/Mac:**
```bash
export MAIL_USERNAME="your-email@163.com"
export MAIL_PASSWORD="your-smtp-auth-code"
export DB_PASSWORD="your-db-password"
export JWT_SECRET="your-jwt-secret-key"
export YUANYANYAO_APP_KEY="your-api-key"
export JISUAPI_APP_KEY="your-api-key"
```

## 需要配置的敏感信息

### 1. SMTP 邮箱配置
- `MAIL_USERNAME`: 163邮箱地址
- `MAIL_PASSWORD`: SMTP授权码（不是登录密码）

### 2. 数据库配置
- `DB_USERNAME`: 数据库用户名（默认：root）
- `DB_PASSWORD`: 数据库密码

### 3. JWT 配置
- `JWT_SECRET`: JWT签名密钥（生产环境请使用至少32字符的强密钥）

### 4. API 密钥
- `YUANYANYAO_APP_KEY`: 万维易源API密钥
- `JISUAPI_APP_KEY`: 极速数据API密钥

## 如果敏感信息已经泄露

如果敏感信息已经被提交到 Git 仓库，需要：

1. **立即更换所有泄露的凭证**：
   - 更换邮箱授权码
   - 更换数据库密码
   - 更换 JWT 密钥
   - 更换 API 密钥

2. **从 Git 历史中移除敏感信息**（需要谨慎操作）：
   ```bash
   # 使用 git filter-branch 或 BFG Repo-Cleaner
   # 注意：这会重写 Git 历史，需要强制推送
   ```

3. **通知团队成员**更新本地配置

## 安全检查清单

- [ ] 所有敏感信息已从 `application.yml` 中移除
- [ ] 已创建 `application-local.yml` 并配置实际值
- [ ] `.gitignore` 已排除敏感配置文件
- [ ] 已更换所有泄露的凭证
- [ ] 团队成员已了解安全配置要求

## 参考

- [Spring Boot 外部化配置](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [GitGuardian 安全最佳实践](https://www.gitguardian.com/)

