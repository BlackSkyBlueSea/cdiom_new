# 药品数据获取问题排查指南

## 问题现象
前端无法获取数据库中的药品数据

## 排查步骤

### 1. 检查后端服务是否正常运行

**检查方法：**
- 查看后端控制台是否有错误信息
- 确认后端服务已启动（端口8080）
- 检查是否有编译错误

**解决方法：**
```bash
# 如果后端服务未启动，需要重新编译和启动
cd cdiom_backend
mvn clean compile
mvn spring-boot:run
```

### 2. 检查数据库连接

**检查方法：**
- 确认MySQL数据库服务已启动
- 确认数据库 `cdiom_db` 存在
- 确认表 `drug_info` 存在
- 检查 `application.yml` 中的数据库配置是否正确

**检查SQL：**
```sql
-- 检查表是否存在
SHOW TABLES LIKE 'drug_info';

-- 检查表中是否有数据
SELECT COUNT(*) FROM drug_info;

-- 查看前几条数据
SELECT * FROM drug_info LIMIT 5;
```

### 3. 检查数据库中是否有数据

**如果数据库中没有数据：**
1. 执行之前生成的 `drug_info_insert.sql` 脚本
2. 在MySQL Workbench中打开该文件并执行

### 4. 检查API路径是否正确

**前端请求路径：** `/api/v1/drugs`
**后端Controller路径：** `/api/v1/drugs`

**测试方法：**
使用浏览器开发者工具（F12）查看Network标签：
1. 打开药品管理页面
2. 查看是否有 `/api/v1/drugs` 的请求
3. 查看请求的响应状态码和内容

### 5. 检查JWT Token是否有效

**检查方法：**
1. 打开浏览器开发者工具（F12）
2. 查看Application/Storage -> Cookies
3. 确认 `cdiom_token` 是否存在
4. 如果不存在或过期，需要重新登录

### 6. 检查后端日志

**查看后端控制台输出：**
- 是否有SQL执行日志
- 是否有异常堆栈信息
- 是否有MyBatis Plus的查询日志

**如果看到SQL日志，检查：**
- SQL语句是否正确
- 查询结果是否为空
- 是否有WHERE条件导致查询不到数据

### 7. 检查前端控制台错误

**检查方法：**
1. 打开浏览器开发者工具（F12）
2. 查看Console标签
3. 查看是否有JavaScript错误
4. 查看Network标签中的请求详情

**常见错误：**
- 401 Unauthorized：Token无效或过期，需要重新登录
- 404 Not Found：API路径不正确
- 500 Internal Server Error：后端服务器错误，查看后端日志

### 8. 手动测试API

**使用Postman或curl测试：**

```bash
# 先登录获取token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 使用返回的token查询药品列表
curl -X GET "http://localhost:8080/api/v1/drugs?page=1&size=10" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 9. 检查MyBatis Plus配置

**确认点：**
- `@TableName("drug_info")` 注解正确
- 字段映射正确（驼峰命名）
- 逻辑删除字段配置正确（deleted字段）

### 10. 检查Service实现

**确认点：**
- `DrugInfoServiceImpl` 是否正确注入 `DrugInfoMapper`
- 查询条件是否正确
- 分页参数是否正确传递

## 常见问题及解决方案

### 问题1：数据库中没有数据
**解决方案：** 执行 `drug_info_insert.sql` 脚本导入数据

### 问题2：后端服务未重启
**解决方案：** 重启后端服务，确保新代码生效

### 问题3：Token过期
**解决方案：** 重新登录获取新的Token

### 问题4：API路径不匹配
**解决方案：** 确认前端请求路径和后端Controller路径一致

### 问题5：权限问题
**解决方案：** 确认用户已登录且Token有效

### 问题6：数据库连接失败
**解决方案：** 检查 `application.yml` 中的数据库配置

## 快速诊断命令

```sql
-- 在MySQL中执行，检查数据
USE cdiom_db;
SELECT COUNT(*) as total FROM drug_info;
SELECT * FROM drug_info LIMIT 5;
```

```bash
# 检查后端服务是否运行
netstat -ano | findstr :8080

# 检查前端服务是否运行
netstat -ano | findstr :5173
```

## 如果以上都正常，请检查：

1. **浏览器控制台**：查看具体的错误信息
2. **后端日志**：查看是否有异常堆栈
3. **网络请求**：查看请求和响应的详细信息

