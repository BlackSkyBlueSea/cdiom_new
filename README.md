<div align="center">

# 🏥 CDIOM - 临床药品出入库管理系统

**Clinical Drug In-Out Management System**

[![Version](https://img.shields.io/badge/version-1.6.0-blue.svg)](https://github.com/BlackSkyBlueSea/cdiom_new)
[![License](https://img.shields.io/badge/license-ISC-green.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.8-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.2.0-61dafb.svg)](https://react.dev/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-4479A1.svg)](https://www.mysql.com/)

基于 Spring Boot + React + MySQL 的医疗药品仓库管理系统

[功能特性](#-功能特性) • [快速开始](#-快速开始) • [技术栈](#️-技术栈) • [项目结构](#-项目结构) • [文档](#-文档) • [贡献指南](#-贡献指南)

</div>

---

## 📋 项目简介

CDIOM（Clinical Drug In-Out Management System）是一个面向医疗机构的药品仓库管理系统，旨在实现药品信息管理、库存监控、出入库审批、批次追溯及多角色权限管控等核心功能。

### 🎯 核心价值

- **规范化管理**：遵循GSP（药品经营质量管理规范）要求，确保药品管理合规
- **全流程追溯**：支持批次管理、效期预警、操作日志记录，实现药品全生命周期追溯
- **多角色协同**：支持系统管理员、仓库管理员、采购专员、医护人员、供应商等多角色协同工作
- **智能化操作**：集成第三方药品信息API，支持扫码识别，自动数据填充

---

## ✨ 功能特性

### 🔐 系统管理
- ✅ **用户管理**：用户CRUD、状态管理、权限管理、邮箱验证
- ✅ **角色管理**：角色CRUD、状态管理、权限分配
- ✅ **权限控制**：基于RBAC的权限系统，支持细粒度权限控制
- ✅ **参数配置**：系统参数配置管理
- ✅ **通知公告**：系统通知公告发布和管理
- ✅ **日志记录**：操作日志、登录日志记录和查询

### 💊 业务管理
- ✅ **药品信息管理**：药品CRUD、扫码识别、第三方API集成、多维度查询
- ✅ **库存管理**：库存查询、近效期预警、库存统计、Excel导出
- ✅ **入库管理**：入库记录、验收流程、效期校验、特殊药品双人操作
- ✅ **出库管理**：出库申请、审批流程、FIFO库存扣减、特殊药品双人审批
- ✅ **采购订单管理**：订单CRUD、状态流转、条形码生成、物流跟踪
- ✅ **供应商管理**：供应商CRUD、审核流程、关联药品管理、价格协议
- ✅ **库存调整**：库存盘点、盘盈盘亏记录、审核功能

### 📊 数据可视化
- ✅ **多角色仪表盘**：系统管理员、仓库管理员、采购专员、医护人员、供应商专用仪表盘
- ✅ **数据统计**：订单统计、库存统计、出入库统计、趋势分析
- ✅ **图表展示**：使用Ant Design Charts实现数据可视化

### 🔒 安全特性
- ✅ **JWT认证**：无状态Token认证，8小时有效期
- ✅ **密码加密**：BCrypt加密存储，强度10轮
- ✅ **登录锁定**：连续5次失败锁定1小时，防止暴力破解
- ✅ **权限拦截**：接口级权限控制，支持权限注解
- ✅ **操作审计**：完整的操作日志和登录日志记录

### 🚀 技术特性
- ✅ **前后端分离**：RESTful API设计，前后端独立部署
- ✅ **响应式设计**：支持PC端和Pad端访问
- ✅ **实时监控**：WebSocket实时日志推送，后端监控
- ✅ **文件上传**：图片上传、文件管理
- ✅ **数据导出**：Excel导出功能（药品、库存、订单）

---

## 🚀 快速开始

### 环境要求

| 环境 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 17+ | Java开发环境 |
| MySQL | 8.0+ | 数据库 |
| Node.js | 18+ | 前端运行环境 |
| Maven | 3.6+ | 后端构建工具 |

### 一键启动

#### 1️⃣ 克隆项目

```bash
git clone https://github.com/BlackSkyBlueSea/cdiom_new.git
cd cdiom_new
```

#### 2️⃣ 初始化数据库

```bash
# 执行数据库初始化脚本（推荐方式）
mysql -u root -p < cdiom_backend/src/main/resources/db/init_simple.sql
mysql -u root -p < cdiom_backend/src/main/resources/db/create_supplier_drug_relation.sql
mysql -u root -p < cdiom_backend/src/main/resources/db/add_user_permission_system.sql
mysql -u root -p < cdiom_backend/src/main/resources/db/init_permissions.sql
mysql -u root -p < cdiom_backend/src/main/resources/db/init_super_admin.sql
```

> 📖 **详细说明**：更多数据库初始化说明请参考 [Database_Design.md](./docs/Database_Design.md)

#### 3️⃣ 配置应用

**后端配置** (`cdiom_backend/src/main/resources/application.yml`):

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/cdiom_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your-password  # 修改为你的数据库密码
```

**前端配置** (`cdiom_frontend/vite.config.js`):

```javascript
// 默认已配置，如需修改后端地址，请修改 proxy 配置
```

#### 4️⃣ 启动服务

**启动后端**:

```bash
cd cdiom_backend
mvn clean install
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动

**启动前端**:

```bash
cd cdiom_frontend
npm install
npm run dev
```

前端服务将在 `http://localhost:5173` 启动

#### 5️⃣ 访问系统

- **前端地址**：http://localhost:5173
- **默认账号**：`admin` / `admin123`

> ⚠️ **安全提示**：首次登录后请立即修改默认密码！

> 📖 **详细部署说明**：更多部署配置请参考 [Deployment_Guide.md](./docs/Deployment_Guide.md)

---

## 🛠️ 技术栈

### 后端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| **Java** | 17 | 编程语言 |
| **Spring Boot** | 3.2.8 | 应用框架 |
| **Spring Security** | 6.2.3 | 安全框架 |
| **MyBatis-Plus** | 3.5.15 | ORM框架 |
| **MySQL** | 8.0.33 | 数据库 |
| **JWT** | 0.12.3 | Token认证 |
| **Hutool** | 5.8.28 | 工具类库 |
| **Lombok** | 1.18.30 | 代码简化 |
| **Apache POI** | 5.2.5 | Excel导出 |
| **ZXing** | 3.5.3 | 条形码生成 |
| **WebSocket** | - | 实时日志推送 |
| **Spring Mail** | - | 邮件验证码 |

### 前端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| **React** | 18.2.0 | UI框架 |
| **React Router** | 6.23.1 | 路由管理 |
| **Ant Design** | 5.20.6 | UI组件库 |
| **@ant-design/charts** | 2.6.7 | 数据可视化 |
| **Axios** | 1.7.2 | HTTP客户端 |
| **Vite** | 7.3.1 | 构建工具 |
| **Day.js** | 1.11.11 | 日期处理 |
| **js-cookie** | 3.0.5 | Cookie管理 |
| **Less** | 4.2.0 | CSS预处理器 |

---

## 📁 项目结构

```
cdiom_new/
├── cdiom_backend/                    # 后端项目（Spring Boot）
│   ├── src/main/java/com/cdiom/backend/
│   │   ├── controller/               # 控制器层（RESTful API）
│   │   ├── service/                  # 服务层（业务逻辑）
│   │   ├── mapper/                   # 数据访问层（MyBatis Mapper）
│   │   ├── model/                    # 实体类（数据库表映射）
│   │   ├── config/                   # 配置类（Spring配置）
│   │   ├── common/                   # 公共类（统一响应、异常处理）
│   │   └── util/                     # 工具类
│   └── src/main/resources/
│       ├── application.yml           # 应用配置文件
│       └── db/                       # 数据库脚本目录
│
├── cdiom_frontend/                   # 前端项目（React + Vite）
│   ├── src/
│   │   ├── pages/                    # 页面组件
│   │   ├── components/                # 公共组件
│   │   ├── utils/                     # 工具函数
│   │   └── App.jsx                   # 根组件
│   ├── vite.config.js                # Vite配置
│   └── package.json                  # 依赖配置
│
└── docs/                             # 项目文档目录
    ├── README.md                     # 文档索引
    ├── Core_Business_Requirements_Analysis.md    # 需求分析
    ├── Database_Design.md             # 数据库设计
    ├── Function_Modules.md            # 功能模块
    ├── API_Documentation.md           # API接口文档
    ├── System_Test_Report.md          # 系统测试
    ├── CHANGELOG.md                   # 版本历史
    ├── Deployment_Guide.md            # 部署指南
    └── Development_Guide.md           # 开发指南
```

> 📖 **详细说明**：更多项目结构说明请参考 [Project_Structure.md](./docs/Project_Structure.md)，包含后端、前端、数据库脚本、文档等所有目录和文件的详细说明。 

---

## 📚 文档

### 📋 需求分析阶段

> 📋 **核心业务需求分析**：详细的需求分析文档请参考 [Core_Business_Requirements_Analysis.md](./docs/Core_Business_Requirements_Analysis.md)，包含库存管理、入库管理、出库管理的完整业务流程和处理流程。

### 🎨 设计阶段

> 🗄️ **数据库设计**：详细的数据库设计文档请参考 [Database_Design.md](./docs/Database_Design.md)，包含数据库表分类、表结构说明、表关系、设计特点等完整的数据库设计内容。

> 📦 **功能模块详细说明**：详细的功能模块描述请参考 [Function_Modules.md](./docs/Function_Modules.md)，包含所有后端和前端模块的完整功能特性说明、项目进度统计和待实现功能规划。

> 📝 **API接口文档**：详细的API接口文档请参考 [API_Documentation.md](./docs/API_Documentation.md)，包含所有接口的详细说明，包括请求参数、响应格式、权限要求、业务说明等完整的接口文档内容。

### 🧪 测试阶段

> 🧪 **系统测试**：详细的系统测试文档请参考 [System_Test_Report.md](./docs/System_Test_Report.md)，包含功能测试、性能测试、安全测试、兼容性测试等完整的测试用例和测试结果。

### 📋 维护和版本管理

> 📋 **版本历史文档**：详细的版本历史文档请参考 [CHANGELOG.md](./docs/CHANGELOG.md)，包含所有版本的完整更新内容，包括功能更新、bug修复、性能优化、数据库变更等详细的版本变更记录。

### 🚀 部署和运维

> 🚀 **部署指南**：详细的部署说明请参考 [Deployment_Guide.md](./docs/Deployment_Guide.md)，包含开发环境部署、生产环境部署、Nginx配置、系统服务配置等完整的部署指南。

### 💻 开发指南

> 💻 **开发指南**：详细的开发指南请参考 [Development_Guide.md](./docs/Development_Guide.md)，包含如何添加新业务模块、前端页面开发、开发规范、代码规范、贡献指南等完整的开发指南内容。

---

## 📊 项目进度

### 项目完成度概览

**总体完成度：约 94-97%**

| 模块 | 完成度 | 状态 |
|------|--------|------|
| 核心业务模块 | 100% | ✅ 已完成 |
| 功能增强 | 87.5% | ✅ 7/8 已完成 |
| 技术架构 | 100% | ✅ 已完成 |

### 已完成功能

- ✅ **10个核心业务模块**：系统管理、权限管理、药品管理、库存管理、供应商管理、入库管理、出库管理、采购订单管理、库存调整、仪表盘
- ✅ **7项功能增强**：数据可视化、权限系统、文件上传、条形码服务、IP定位服务、数据导出、响应式优化
- ✅ **5项技术架构**：后端架构、前端架构、数据库设计、安全机制、API集成

### 待实现功能

- ⏳ **扫码枪适配（HID键盘模式）**：实现扫码枪自动识别和填充
- ⏳ **常用药品收藏功能**：用户收藏常用药品，快速访问

> 📖 **详细说明**：更多项目进度信息请参考 [Function_Modules.md](./docs/Function_Modules.md#项目进度)

---

## ⚠️ 注意事项

### 安全相关

1. **JWT密钥**：建议在生产环境中修改 `application.yml` 中的JWT密钥
2. **默认密码**：默认管理员密码为`admin123`，首次登录后建议修改
3. **密码加密**：所有密码都使用BCrypt加密存储，强度10轮
4. **登录锁定**：连续5次登录失败会锁定1小时，需要管理员解锁

### 开发相关

> 💻 **开发相关注意事项**：详细的开发注意事项请参考 [Development_Guide.md](./docs/Development_Guide.md#开发注意事项)，包含通用注意事项、后端注意事项、前端注意事项、安全注意事项等完整的开发注意事项说明。

### 数据库相关

> 📖 **数据库相关注意事项**：详细的数据库相关注意事项请参考 [Database_Design.md](./docs/Database_Design.md#注意事项)，包含数据库配置、初始化脚本、业务规则、性能优化等完整的注意事项说明。

---

## ❓ 常见问题

### Q1: 登录失败怎么办？

**A**: 检查以下几点：
1. 确认数据库已初始化，admin用户已创建
2. 确认密码是否为BCrypt加密后的值（不是明文）
3. 检查用户是否被锁定（连续5次失败会锁定1小时）
4. 检查用户状态是否为正常（status=1）

### Q2: 如何修改管理员密码？

**A**: 有两种方式：
1. **通过系统界面**：登录后进入用户管理页面修改
2. **通过数据库**：执行SQL更新密码（需要先BCrypt加密）

### Q3: 数据库初始化失败？

**A**: 检查以下几点：
1. MySQL版本是否为8.0+
2. 字符集是否为utf8mb4
3. 是否有足够的权限创建数据库和表
4. 检查SQL脚本中的中文注释是否导致编码问题（可使用init_simple.sql）

### Q4: 前端无法连接后端？

**A**: 检查以下几点：
1. 确认后端服务已启动（http://localhost:8080）
2. 检查 `vite.config.js` 中的代理配置
3. 检查后端CORS配置（如果需要）
4. 检查浏览器控制台的错误信息

### Q5: Token过期怎么办？

**A**: Token有效期为8小时，过期后需要重新登录。前端会自动检测Token过期并跳转到登录页。

> 📖 **更多问题**：更多常见问题请参考 [Development_Guide.md](./docs/Development_Guide.md#常见问题) 和 [Deployment_Guide.md](./docs/Deployment_Guide.md#常见问题)

---

## 🤝 贡献指南

欢迎贡献代码！请遵循以下规范：

> 💻 **详细贡献指南**：详细的贡献指南请参考 [Development_Guide.md](./docs/Development_Guide.md#贡献指南)，包含代码规范、提交规范、测试要求、文档更新、Pull Request流程等完整的贡献指南内容。

### 快速开始贡献

1. **Fork** 本项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 **Pull Request**

---

## 📮 联系方式

如有问题或建议，请通过以下方式联系：

- **项目Issues**：[GitHub Issues](https://github.com/BlackSkyBlueSea/cdiom_new/issues)
- **邮箱**：[youliyin@outlook.com](mailto:youliyin@outlook.com)

---

## 📄 许可证

本项目采用 [ISC](LICENSE) 许可证。

---

## 👥 作者

**CDIOM开发团队**

- 项目维护者：[youliyin@outlook.com](mailto:youliyin@outlook.com)

---

## 📝 版本信息

**当前版本**：v1.6.0  
**最后更新**：2026年1月20日  
**文档版本**：v1.6.0

> 📋 **版本历史**：查看完整版本历史请参考 [CHANGELOG.md](./docs/CHANGELOG.md)

---

<div align="center">

**如果这个项目对你有帮助，请给一个 ⭐ Star！**

Made with ❤️ by CDIOM Team

</div>
