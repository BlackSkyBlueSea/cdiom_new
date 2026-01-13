# 🔒 安全漏洞快速修复指南

## ⚠️ 紧急操作

由于 GitGuardian 检测到 SMTP 凭证泄露，请立即执行以下操作：

### 1. 立即更换泄露的凭证（最重要！）

**必须立即更换：**
- ✅ **163邮箱授权码**：登录 163 邮箱 -> 设置 -> POP3/SMTP/IMAP -> 重新生成授权码
- ✅ **数据库密码**：修改 MySQL root 密码
- ✅ **JWT 密钥**：生成新的强密钥（至少32字符）
- ✅ **API 密钥**：在对应平台重新生成 API 密钥

### 2. 验证修复

已完成的修复：
- ✅ 从 `application.yml` 中移除了所有硬编码的敏感信息
- ✅ 创建了 `application-local.yml` 用于本地配置（不会被提交）
- ✅ 更新了 `.gitignore` 排除敏感配置文件
- ✅ 创建了配置示例文件 `application.yml.example`

### 3. 配置本地环境

**方式1：使用 application-local.yml（推荐）**

文件已创建在：`cdiom_backend/src/main/resources/application-local.yml`

请编辑此文件，填入新的凭证：
```yaml
spring:
  mail:
    username: 你的新邮箱@163.com
    password: 你的新授权码
  datasource:
    password: 你的新数据库密码

jwt:
  secret: 你的新JWT密钥（至少32字符）

yuanyanyao:
  api:
    app-key: 你的新API密钥

jisuapi:
  api:
    app-key: 你的新API密钥
```

**方式2：使用环境变量**

设置环境变量后运行应用。

### 4. 从 Git 历史中移除敏感信息（可选但推荐）

如果要从 Git 历史中完全移除敏感信息：

```bash
# 方法1：使用 git filter-branch（需要谨慎）
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch cdiom_backend/src/main/resources/application.yml" \
  --prune-empty --tag-name-filter cat -- --all

# 方法2：使用 BFG Repo-Cleaner（更简单，需要先安装）
# 下载 BFG: https://rtyley.github.io/bfg-repo-cleaner/
java -jar bfg.jar --replace-text passwords.txt cdiom_new.git

# 注意：执行后需要强制推送（会改变历史）
git push --force --all
```

⚠️ **警告**：强制推送会改变 Git 历史，需要通知所有团队成员重新克隆仓库。

### 5. 提交修复

```bash
git add .
git commit -m "security: 移除硬编码的敏感信息，使用环境变量配置"
git push
```

## 检查清单

- [ ] 已更换所有泄露的凭证
- [ ] 已配置 `application-local.yml` 或环境变量
- [ ] 已测试应用能正常启动
- [ ] 已提交修复到 Git
- [ ] （可选）已从 Git 历史中移除敏感信息

## 后续预防措施

1. **代码审查**：提交前检查是否包含敏感信息
2. **使用 Git Hooks**：设置 pre-commit hook 检查敏感信息
3. **定期扫描**：使用 GitGuardian 等工具定期扫描
4. **文档化**：确保团队成员了解安全配置要求

## 需要帮助？

如果遇到问题，请参考 `SECURITY_CONFIG.md` 获取详细说明。

