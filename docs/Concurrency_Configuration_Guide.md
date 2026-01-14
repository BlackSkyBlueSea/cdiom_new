# CDIOM系统多用户并发访问配置说明

## 一、系统并发支持原理

### 1. Spring Boot默认支持并发

Spring Boot使用**内嵌Tomcat服务器**，默认就是**多线程架构**：

- ✅ **每个HTTP请求都在独立的线程中处理**
- ✅ **Tomcat使用线程池管理请求处理线程**
- ✅ **无需额外配置即可支持多用户并发访问**

### 2. 系统架构说明

```
用户1请求 → Tomcat线程池 → 线程1 → Controller → Service → 数据库连接池 → 数据库
用户2请求 → Tomcat线程池 → 线程2 → Controller → Service → 数据库连接池 → 数据库
用户3请求 → Tomcat线程池 → 线程3 → Controller → Service → 数据库连接池 → 数据库
...
```

**关键点：**
- 每个用户请求都是**独立的线程**
- 数据库连接池**共享**，但每个线程使用**独立的连接**
- JWT认证是**无状态的**，线程安全

## 二、已优化的配置

### 1. Tomcat线程池配置

```yaml
server:
  tomcat:
    threads:
      max: 200          # 最大工作线程数（可同时处理200个请求）
      min-spare: 10     # 最小空闲线程数
    max-connections: 10000  # 最大连接数
    accept-count: 100       # 等待队列长度
    connection-timeout: 20000  # 连接超时时间
```

**说明：**
- `max: 200` - 最多可以同时处理200个并发请求
- `max-connections: 10000` - 最多可以接受10000个连接
- `accept-count: 100` - 当所有线程忙碌时，最多等待100个请求

### 2. 数据库连接池配置（HikariCP）

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 10              # 最小空闲连接数
      maximum-pool-size: 50         # 最大连接池大小（可同时50个数据库操作）
      connection-timeout: 30000      # 连接超时时间
      idle-timeout: 600000           # 空闲连接超时时间
      max-lifetime: 1800000          # 连接最大生命周期
      leak-detection-threshold: 60000 # 连接泄漏检测
```

**说明：**
- `maximum-pool-size: 50` - 最多可以同时执行50个数据库操作
- 当连接池满时，新请求会等待，直到有连接释放

### 3. 异步任务线程池配置

已创建`AsyncConfig.java`，配置了异步任务执行线程池：
- 核心线程数：5
- 最大线程数：20
- 队列容量：100

## 三、并发访问能力

### 当前配置支持的并发能力

| 配置项 | 当前值 | 说明 |
|--------|--------|------|
| **Tomcat最大线程数** | 200 | 可同时处理200个HTTP请求 |
| **数据库连接池大小** | 50 | 可同时执行50个数据库操作 |
| **最大连接数** | 10000 | 可接受10000个TCP连接 |
| **等待队列** | 100 | 超出线程数时等待的请求数 |

### 实际并发用户数估算

**理论最大并发用户数：**
- 如果每个请求平均耗时100ms：**200线程 × 10请求/秒 = 2000请求/秒**
- 如果每个请求平均耗时500ms：**200线程 × 2请求/秒 = 400请求/秒**

**实际建议：**
- **轻量级操作**（查询、简单更新）：支持 **100-200** 并发用户
- **重量级操作**（复杂查询、批量操作）：支持 **50-100** 并发用户

## 四、如何验证并发支持

### 1. 使用JMeter进行压力测试

```bash
# 安装JMeter
# 创建测试计划：
# - 线程组：100个线程
# - 循环次数：10次
# - HTTP请求：POST /api/v1/auth/login
# - 查看结果树和聚合报告
```

### 2. 使用Apache Bench (ab) 测试

```bash
# 安装Apache Bench
# 测试登录接口并发
ab -n 1000 -c 50 -p login.json -T application/json http://localhost:8080/api/v1/auth/login
```

### 3. 查看日志验证

查看日志中的线程名称，可以看到不同线程处理不同请求：
```
[http-nio-8080-exec-1] - 用户1的请求
[http-nio-8080-exec-2] - 用户2的请求
[http-nio-8080-exec-3] - 用户3的请求
...
```

## 五、性能优化建议

### 1. 根据实际需求调整配置

**如果并发用户数较多（>200）：**
```yaml
server:
  tomcat:
    threads:
      max: 500          # 增加最大线程数
spring:
  datasource:
    hikari:
      maximum-pool-size: 100  # 增加数据库连接池大小
```

**如果服务器资源有限：**
```yaml
server:
  tomcat:
    threads:
      max: 100          # 减少最大线程数
spring:
  datasource:
    hikari:
      maximum-pool-size: 20   # 减少数据库连接池大小
```

### 2. 数据库优化

- ✅ 确保数据库连接数足够（MySQL默认151个连接）
- ✅ 为常用查询字段添加索引
- ✅ 使用连接池监控工具监控连接使用情况

### 3. 应用层优化

- ✅ 使用缓存（Redis）减少数据库访问
- ✅ 异步处理耗时操作（如日志记录）
- ✅ 使用分页查询避免一次性加载大量数据

## 六、线程安全保证

### 1. JWT认证（无状态）

✅ **线程安全** - JWT是无状态的，每个请求独立验证，不共享状态

### 2. Spring Bean（单例）

✅ **线程安全** - Spring的Service、Controller都是单例，但方法内部是线程安全的
- 每个请求都有独立的参数
- 数据库操作通过连接池隔离

### 3. 数据库连接

✅ **线程安全** - HikariCP连接池保证每个线程使用独立的连接

## 七、常见问题

### Q1: 为什么我的系统只能一个用户登录？

**A:** 这不是并发问题，可能是：
- 前端Token存储方式问题（应该使用Cookie，不是localStorage）
- 浏览器同源策略问题
- 检查`application.yml`中的连接池配置是否正确

### Q2: 如何查看当前并发连接数？

**A:** 
- 查看HikariCP监控：访问 `/actuator/hikaricp`（需要添加actuator依赖）
- 查看Tomcat线程：查看日志中的线程名称
- 查看数据库连接：`SHOW PROCESSLIST;`

### Q3: 如何提高并发能力？

**A:**
1. 增加Tomcat线程数
2. 增加数据库连接池大小
3. 使用Redis缓存
4. 数据库读写分离
5. 使用负载均衡（Nginx + 多个应用实例）

## 八、监控和调优

### 1. 添加Actuator监控（可选）

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,threaddump
```

### 2. 监控指标

- **线程数**：`/actuator/metrics/jvm.threads.live`
- **数据库连接池**：`/actuator/metrics/hikari.connections.active`
- **HTTP请求**：`/actuator/metrics/http.server.requests`

## 总结

✅ **系统已支持多用户并发访问**
✅ **已优化Tomcat线程池和数据库连接池配置**
✅ **当前配置可支持100-200并发用户**
✅ **JWT认证是线程安全的**
✅ **可根据实际需求调整配置参数**

系统默认就支持多用户并发访问，无需额外编程。只需要根据实际并发需求调整配置参数即可。

---

**最后更新**：2026年1月12日
