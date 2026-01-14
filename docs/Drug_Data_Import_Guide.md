# 药品信息数据导入说明

## 文件说明

1. **crawl_drug_data.py** - Python 爬虫脚本，用于爬取药品信息并生成 SQL 脚本
2. **drug_info_insert.sql** - 生成的 SQL 插入脚本，包含100种常用药品信息
3. **requirements_crawler.txt** - Python 依赖包列表

## 使用方法

### 方法一：直接使用生成的 SQL 文件（推荐）

1. 打开 MySQL Workbench
2. 连接到 `cdiom_db` 数据库
3. 打开 `drug_info_insert.sql` 文件
4. 执行整个脚本（或按需选择部分数据执行）

### 方法二：重新生成 SQL 文件

如果需要重新生成或修改数据：

1. 安装 Python 依赖：
```bash
pip install -r requirements_crawler.txt
```

2. 运行爬虫脚本：
```bash
python crawl_drug_data.py
```

3. 脚本会自动生成 `drug_info_insert.sql` 文件

## 数据说明

生成的 SQL 脚本包含以下内容：

- **100种常用药品信息**
- 每条记录包含以下字段：
  - `national_code`: 国家本位码（唯一，自动生成）
  - `trace_code`: 药品追溯码（唯一，自动生成）
  - `product_code`: 商品码（自动生成）
  - `drug_name`: 药品名称
  - `dosage_form`: 剂型（片剂、胶囊、颗粒等）
  - `specification`: 规格
  - `approval_number`: 批准文号（模拟生成）
  - `manufacturer`: 生产厂家
  - `expiry_date`: 有效期（未来1-3年随机生成）
  - `is_special`: 是否特殊药品（0-普通/1-特殊）
  - `storage_requirement`: 存储要求（根据剂型自动判断）
  - `unit`: 单位（根据规格自动判断：片/粒/袋/支/盒）

## 注意事项

1. **唯一约束**：
   - `national_code` 和 `trace_code` 字段有唯一约束
   - 如果数据库中已存在相同的数据，插入会失败
   - 可以使用 `INSERT IGNORE` 或 `ON DUPLICATE KEY UPDATE` 来处理重复数据

2. **外键约束**：
   - `supplier_id` 字段有外键约束，如果为 NULL 则不受影响
   - `create_by` 字段有外键约束，如果为 NULL 则不受影响

3. **事务处理**：
   - SQL 脚本使用事务（START TRANSACTION ... COMMIT）
   - 如果某条数据插入失败，可以回滚整个事务

4. **数据来源**：
   - 脚本会尝试爬取 https://data.pharnexcloud.com/6/table/159 网站
   - 如果爬取失败，会使用预设的常用药品数据
   - 部分字段（如批准文号、有效期等）是模拟生成的

## 修改建议

如果需要修改生成的数据，可以编辑 `crawl_drug_data.py` 文件：

1. 修改 `COMMON_DRUGS` 列表添加更多药品
2. 修改 `get_common_drugs()` 函数中的药品列表
3. 调整字段生成逻辑（如有效期范围、存储要求判断等）

## 故障排除

如果执行 SQL 时遇到错误：

1. **唯一约束冲突**：
   - 检查数据库中是否已存在相同的 `national_code` 或 `trace_code`
   - 可以删除现有数据或修改 SQL 脚本中的这些字段值

2. **外键约束错误**：
   - 确保 `supplier_id` 和 `create_by` 的值在对应的表中存在
   - 或者保持为 NULL（脚本中已设置为 NULL）

3. **字符编码问题**：
   - 确保数据库和表的字符集为 `utf8mb4`
   - 确保 MySQL Workbench 使用 UTF-8 编码打开 SQL 文件

---

**最后更新**：2026年1月9日

