# CDIOM - 开发指南

本文档详细描述了CDIOM系统的开发指南，包含如何添加新业务模块、前端页面开发、开发规范、代码规范、贡献指南等完整的开发指南内容。

## 目录

- [项目架构概述](#项目架构概述)
- [后端开发指南](#后端开发指南)
  - [添加新的业务模块](#添加新的业务模块)
  - [开发规范](#开发规范)
  - [代码示例](#代码示例)
- [前端开发指南](#前端开发指南)
  - [添加新页面](#添加新页面)
  - [开发规范](#开发规范-1)
  - [代码示例](#代码示例-1)
- [开发注意事项](#开发注意事项)
- [贡献指南](#贡献指南)
- [常见问题](#常见问题)

---

## 项目架构概述

### 技术栈

**后端：**
- Spring Boot 3.2.8
- MyBatis-Plus 3.5.5
- Spring Security 6.2.3
- JWT认证
- MySQL 8.0+

**前端：**
- React 18.2.0
- Vite 7.3.1
- Ant Design 5.12.8
- React Router 6.22.0
- Axios 1.6.5

### 项目结构

```
cdiom_new/
├── cdiom_backend/          # 后端项目
│   └── src/main/java/com/cdiom/backend/
│       ├── controller/     # 控制器层
│       ├── service/         # 服务层
│       ├── mapper/          # 数据访问层
│       ├── model/           # 实体类
│       ├── config/          # 配置类
│       ├── common/          # 公共类
│       └── util/            # 工具类
└── cdiom_frontend/          # 前端项目
    └── src/
        ├── pages/           # 页面组件
        ├── components/      # 公共组件
        ├── utils/           # 工具函数
        └── App.jsx          # 根组件
```

---

## 后端开发指南

### 添加新的业务模块

#### 1. 创建数据库表

首先在数据库中创建对应的表结构，建议使用SQL脚本管理：

```sql
CREATE TABLE your_table (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL COMMENT '名称',
    status TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    INDEX idx_status (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='你的表';
```

#### 2. 创建实体类（Model）

在 `cdiom_backend/src/main/java/com/cdiom/backend/model/` 目录下创建实体类：

```java
package com.cdiom.backend.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 你的实体类
 * 
 * @author cdiom
 */
@Data
@TableName("your_table")
public class YourModel {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 名称
     */
    private String name;
    
    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;
}
```

**注意事项：**
- 使用 `@TableName` 指定表名
- 使用 `@TableId(type = IdType.AUTO)` 指定主键自增
- 使用 `@TableLogic` 标记逻辑删除字段
- 使用 `@TableField(fill = FieldFill.INSERT)` 自动填充创建时间
- 使用 `@TableField(fill = FieldFill.INSERT_UPDATE)` 自动填充更新时间

#### 3. 创建Mapper接口

在 `cdiom_backend/src/main/java/com/cdiom/backend/mapper/` 目录下创建Mapper接口：

```java
package com.cdiom.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cdiom.backend.model.YourModel;
import org.apache.ibatis.annotations.Mapper;

/**
 * 你的Mapper接口
 * 
 * @author cdiom
 */
@Mapper
public interface YourMapper extends BaseMapper<YourModel> {
    // 如果需要自定义SQL，可以在这里添加方法
    // 例如：
    // @Select("SELECT * FROM your_table WHERE name = #{name}")
    // YourModel selectByName(String name);
}
```

**注意事项：**
- 继承 `BaseMapper<YourModel>` 获得基础CRUD方法
- 使用 `@Mapper` 注解标记为Mapper接口
- 如需自定义SQL，使用MyBatis注解或XML映射文件

#### 4. 创建Service接口和实现

在 `cdiom_backend/src/main/java/com/cdiom/backend/service/` 目录下创建Service接口：

```java
package com.cdiom.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.model.YourModel;

/**
 * 你的Service接口
 * 
 * @author cdiom
 */
public interface YourService {
    
    /**
     * 分页查询
     */
    Page<YourModel> list(Page<YourModel> page, String keyword);
    
    /**
     * 根据ID查询
     */
    YourModel getById(Long id);
    
    /**
     * 新增
     */
    void create(YourModel model);
    
    /**
     * 更新
     */
    void update(YourModel model);
    
    /**
     * 删除（逻辑删除）
     */
    void delete(Long id);
}
```

在 `cdiom_backend/src/main/java/com/cdiom/backend/service/impl/` 目录下创建Service实现：

```java
package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.mapper.YourMapper;
import com.cdiom.backend.model.YourModel;
import com.cdiom.backend.service.YourService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 你的Service实现
 * 
 * @author cdiom
 */
@Service
@RequiredArgsConstructor
public class YourServiceImpl implements YourService {
    
    private final YourMapper yourMapper;
    
    @Override
    public Page<YourModel> list(Page<YourModel> page, String keyword) {
        LambdaQueryWrapper<YourModel> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(YourModel::getName, keyword);
        }
        wrapper.orderByDesc(YourModel::getCreatedAt);
        return yourMapper.selectPage(page, wrapper);
    }
    
    @Override
    public YourModel getById(Long id) {
        return yourMapper.selectById(id);
    }
    
    @Override
    public void create(YourModel model) {
        yourMapper.insert(model);
    }
    
    @Override
    public void update(YourModel model) {
        yourMapper.updateById(model);
    }
    
    @Override
    public void delete(Long id) {
        yourMapper.deleteById(id); // MyBatis-Plus会自动进行逻辑删除
    }
}
```

**注意事项：**
- 使用 `@Service` 注解标记为Service组件
- 使用 `@RequiredArgsConstructor` 自动生成构造函数（Lombok）
- 使用 `LambdaQueryWrapper` 构建查询条件，类型安全
- 逻辑删除由MyBatis-Plus自动处理

#### 5. 创建Controller

在 `cdiom_backend/src/main/java/com/cdiom/backend/controller/` 目录下创建Controller：

```java
package com.cdiom.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.model.YourModel;
import com.cdiom.backend.service.YourService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 你的Controller
 * 
 * @author cdiom
 */
@RestController
@RequestMapping("/api/v1/your-module")
@RequiredArgsConstructor
public class YourController {
    
    private final YourService yourService;
    
    /**
     * 分页查询
     */
    @GetMapping("/list")
    @RequiresPermission("your:view")
    public Result<Page<YourModel>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        Page<YourModel> pageObj = new Page<>(page, size);
        Page<YourModel> result = yourService.list(pageObj, keyword);
        return Result.success(result);
    }
    
    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    @RequiresPermission("your:view")
    public Result<YourModel> getById(@PathVariable Long id) {
        YourModel model = yourService.getById(id);
        return Result.success(model);
    }
    
    /**
     * 新增
     */
    @PostMapping
    @RequiresPermission("your:manage")
    public Result<?> create(@RequestBody YourModel model) {
        yourService.create(model);
        return Result.success();
    }
    
    /**
     * 更新
     */
    @PutMapping("/{id}")
    @RequiresPermission("your:manage")
    public Result<?> update(@PathVariable Long id, @RequestBody YourModel model) {
        model.setId(id);
        yourService.update(model);
        return Result.success();
    }
    
    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    @RequiresPermission("your:manage")
    public Result<?> delete(@PathVariable Long id) {
        yourService.delete(id);
        return Result.success();
    }
}
```

**注意事项：**
- 使用 `@RestController` 标记为REST控制器
- 使用 `@RequestMapping("/api/v1/your-module")` 指定API前缀
- 使用 `@RequiresPermission` 注解进行权限控制
- 统一使用 `Result<T>` 作为响应格式
- 遵循RESTful API设计规范

### 开发规范

#### 1. 分层架构

- **Controller层**：负责接收请求、参数校验、返回响应
- **Service层**：负责业务逻辑处理
- **Mapper层**：负责数据库操作
- **Model层**：负责数据模型定义

#### 2. 统一响应格式

所有API接口统一使用 `Result<T>` 格式：

```java
// 成功响应
return Result.success(data);
return Result.success("操作成功", data);

// 失败响应
return Result.error("操作失败");
return Result.error(500, "操作失败");
```

**响应格式：**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {}
}
```

**状态码说明：**
- `200`: 操作成功
- `401`: 未授权（Token无效或过期）
- `403`: 无权限
- `500`: 操作失败

#### 3. 异常处理

统一使用 `GlobalExceptionHandler` 处理异常：

```java
// 业务异常
throw new ServiceException(500, "业务异常信息");

// 参数验证异常（自动处理）
@Valid @RequestBody YourModel model
```

**异常处理规则：**
- 业务异常：使用 `ServiceException`
- 参数验证异常：使用 `@Valid` 注解，自动处理
- 权限异常：由Spring Security自动处理
- 其他异常：由 `GlobalExceptionHandler` 统一处理

#### 4. 权限控制

使用 `@RequiresPermission` 注解进行接口级权限控制：

```java
@RequiresPermission("your:view")    // 查看权限
@RequiresPermission("your:manage")  // 管理权限
```

**权限命名规范：**
- 模块名:操作名，例如：`user:view`、`user:manage`、`drug:create`

#### 5. 逻辑删除

所有删除操作均为逻辑删除，使用 `@TableLogic` 注解：

```java
@TableLogic
private Integer deleted; // 0-未删除，1-已删除
```

#### 6. 自动填充

创建时间和更新时间自动填充，使用 `@TableField` 注解：

```java
@TableField(fill = FieldFill.INSERT)
private LocalDateTime createdAt;

@TableField(fill = FieldFill.INSERT_UPDATE)
private LocalDateTime updatedAt;
```

#### 7. 代码规范

- **命名规范**：使用驼峰命名法，类名首字母大写，方法名首字母小写
- **注释规范**：类和方法必须添加JavaDoc注释
- **异常处理**：必须捕获并处理异常，不能直接抛出
- **日志记录**：重要操作必须记录日志，使用SLF4J

### 代码示例

#### 完整示例：用户管理模块

参考现有的 `SysUserController`、`SysUserService`、`SysUserMapper`、`SysUser` 实现。

---

## 前端开发指南

### 添加新页面

#### 1. 创建页面组件

在 `cdiom_frontend/src/pages/` 目录下创建页面组件：

```jsx
// src/pages/YourPage.jsx
import { useState, useEffect } from 'react'
import { Table, Button, Input, Space, message } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import request from '../utils/request'

const YourPage = () => {
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(false)
  const [keyword, setKeyword] = useState('')
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  })

  // 加载数据
  const loadData = async () => {
    setLoading(true)
    try {
      const res = await request.get('/your-module/list', {
        params: {
          page: pagination.current,
          size: pagination.pageSize,
          keyword,
        },
      })
      if (res.code === 200) {
        setData(res.data.records)
        setPagination({
          ...pagination,
          total: res.data.total,
        })
      }
    } catch (error) {
      message.error('加载数据失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [pagination.current, keyword])

  // 表格列定义
  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
    },
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Button
            type="link"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record.id)}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ]

  // 编辑
  const handleEdit = (record) => {
    // 编辑逻辑
  }

  // 删除
  const handleDelete = async (id) => {
    try {
      const res = await request.delete(`/your-module/${id}`)
      if (res.code === 200) {
        message.success('删除成功')
        loadData()
      }
    } catch (error) {
      message.error('删除失败')
    }
  }

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Input.Search
          placeholder="搜索名称"
          allowClear
          onSearch={(value) => {
            setKeyword(value)
            setPagination({ ...pagination, current: 1 })
          }}
          style={{ width: 300 }}
        />
        <Button type="primary" icon={<PlusOutlined />}>
          新增
        </Button>
      </Space>
      <Table
        columns={columns}
        dataSource={data}
        loading={loading}
        rowKey="id"
        pagination={{
          current: pagination.current,
          pageSize: pagination.pageSize,
          total: pagination.total,
          onChange: (page, pageSize) => {
            setPagination({ ...pagination, current: page, pageSize })
          },
        }}
      />
    </div>
  )
}

export default YourPage
```

#### 2. 添加路由

在 `cdiom_frontend/src/App.jsx` 中添加路由：

```jsx
import YourPage from './pages/YourPage'

// 在Routes中添加
<Route path="your-page" element={<YourPage />} />
```

#### 3. 添加菜单项

在 `cdiom_frontend/src/components/Layout.jsx` 中添加菜单项：

```jsx
{
  key: '/app/your-page',
  icon: <YourIcon />,
  label: '你的页面',
  permission: 'your:view', // 权限控制
}
```

### 开发规范

#### 1. 组件化开发

- 使用函数式组件和Hooks
- 组件职责单一，可复用
- 使用自定义Hooks封装业务逻辑

#### 2. UI组件库

- 统一使用Ant Design组件库
- 遵循Ant Design设计规范
- 自定义样式使用CSS Modules或styled-components

#### 3. HTTP请求

- API请求统一使用 `request` 工具（封装了axios）
- 请求路径统一使用 `/api/v1` 前缀
- 自动处理Token和错误响应

```jsx
import request from '../utils/request'

// GET请求
const res = await request.get('/your-module/list', {
  params: { page: 1, size: 10 }
})

// POST请求
const res = await request.post('/your-module', data)

// PUT请求
const res = await request.put(`/your-module/${id}`, data)

// DELETE请求
const res = await request.delete(`/your-module/${id}`)
```

#### 4. 路由管理

- 使用React Router进行路由管理
- 使用 `PrivateRoute` 组件进行路由守卫
- 路由路径统一使用小写字母和连字符

#### 5. Token管理

- Token存储在Cookie中（key: `cdiom_token`）
- Token有效期8小时
- 前端自动检测Token过期并跳转到登录页

#### 6. 权限控制

- 使用 `auth.js` 工具函数检查权限
- 菜单项支持 `permission` 属性进行权限控制
- 按钮操作支持权限检查

```jsx
import { hasPermission } from '../utils/auth'

// 检查权限
if (hasPermission('your:manage')) {
  // 显示管理按钮
}
```

#### 7. 状态管理

- 简单状态使用 `useState`
- 复杂状态使用 `useReducer` 或状态管理库
- 全局状态使用Context API

#### 8. 代码规范

- 使用ESLint进行代码检查
- 使用Prettier进行代码格式化
- 组件文件使用PascalCase命名
- 工具函数使用camelCase命名

### 代码示例

#### 完整示例：用户管理页面

参考现有的 `UserManagement.jsx` 实现。

---

## 开发注意事项

### 通用注意事项

1. **API前缀**：所有API接口前缀为 `/api/v1`
2. **响应格式**：统一使用 `Result<T>` 格式
3. **异常处理**：统一使用 `GlobalExceptionHandler` 处理异常
4. **Token存储**：Token存储在Cookie中（key: `cdiom_token`），有效期8小时
5. **逻辑删除**：所有删除操作均为逻辑删除，数据不会真正删除

### 后端注意事项

1. **数据库操作**：使用MyBatis-Plus进行数据库操作，避免直接写SQL
2. **事务管理**：重要业务操作使用 `@Transactional` 注解
3. **参数验证**：使用 `@Valid` 注解进行参数验证
4. **日志记录**：重要操作必须记录日志
5. **性能优化**：分页查询必须使用分页插件，避免全表查询

### 前端注意事项

1. **组件复用**：公共组件放在 `components/common/` 目录
2. **错误处理**：API请求必须处理错误情况
3. **加载状态**：异步操作必须显示加载状态
4. **用户体验**：操作成功/失败必须提示用户
5. **响应式设计**：支持PC端和Pad端访问

### 安全注意事项

1. **密码加密**：所有密码都使用BCrypt加密存储，强度10轮
2. **JWT密钥**：生产环境必须修改JWT密钥
3. **权限控制**：所有接口必须进行权限验证
4. **SQL注入**：使用MyBatis-Plus参数化查询，避免SQL注入
5. **XSS防护**：前端对用户输入进行转义处理

---

## 贡献指南

欢迎贡献代码！请遵循以下规范：

### 代码规范

1. **Java代码规范**：
   - 遵循阿里巴巴Java开发手册
   - 使用4个空格缩进
   - 类和方法必须添加JavaDoc注释
   - 使用Lombok简化代码

2. **JavaScript代码规范**：
   - 遵循ESLint规则
   - 使用2个空格缩进
   - 使用函数式组件和Hooks
   - 避免使用var，使用const和let

### 提交规范

1. **提交信息格式**：
   ```
   <type>(<scope>): <subject>
   
   <body>
   
   <footer>
   ```

2. **提交类型（type）**：
   - `feat`: 新功能
   - `fix`: 修复bug
   - `docs`: 文档更新
   - `style`: 代码格式调整
   - `refactor`: 代码重构
   - `test`: 测试相关
   - `chore`: 构建/工具相关

3. **提交示例**：
   ```
   feat(user): 添加用户管理功能
   
   - 实现用户CRUD功能
   - 添加用户权限管理
   - 完善用户状态管理
   ```

### 测试要求

1. **功能测试**：确保新功能正常工作
2. **回归测试**：确保不影响现有功能
3. **边界测试**：测试边界情况和异常情况
4. **性能测试**：确保性能不受影响

### 文档更新

1. **代码注释**：新代码必须添加注释
2. **API文档**：新接口必须更新API文档
3. **README**：重大变更必须更新README
4. **CHANGELOG**：版本更新必须更新CHANGELOG

### Pull Request流程

1. Fork项目到个人仓库
2. 创建功能分支（`git checkout -b feature/your-feature`）
3. 提交代码（遵循提交规范）
4. 推送到个人仓库（`git push origin feature/your-feature`）
5. 创建Pull Request
6. 等待代码审查和合并

---

## 常见问题

### Q1: 如何添加新的权限？

**A**: 
1. 在数据库 `sys_permission` 表中添加权限记录
2. 在Controller方法上添加 `@RequiresPermission` 注解
3. 在前端菜单项或按钮上添加 `permission` 属性

### Q2: 如何处理文件上传？

**A**: 
1. 使用 `FileUploadController` 提供的上传接口
2. 前端使用 `Upload` 组件上传文件
3. 文件存储在 `uploads` 目录（可配置）

### Q3: 如何实现分页查询？

**A**: 
1. 后端使用MyBatis-Plus的 `Page` 对象
2. 前端使用Ant Design的 `Table` 组件
3. 参考现有的分页查询实现

### Q4: 如何添加新的数据库表？

**A**: 
1. 创建SQL脚本在 `db/` 目录
2. 创建对应的实体类（Model）
3. 创建Mapper接口
4. 创建Service和Controller

### Q5: 如何调试API接口？

**A**: 
1. 使用Postman或类似工具测试API
2. 查看后端日志输出
3. 使用Swagger UI（如果已配置）

---

**文档版本**: v1.0.0  
**最后更新**: 2026年1月20日

