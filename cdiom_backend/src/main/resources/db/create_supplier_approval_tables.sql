-- ============================================
-- 供应商准入审批流程和廉政风险防控表
-- 实现分权制衡、全程留痕、监督审计
-- ============================================

USE cdiom_db;

-- ============================================
-- 1. 供应商准入审批申请表
-- ============================================
CREATE TABLE IF NOT EXISTS supplier_approval_application (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    supplier_id BIGINT DEFAULT NULL COMMENT '供应商ID（新供应商为NULL，已有供应商修改协议时关联）',
    supplier_name VARCHAR(200) NOT NULL COMMENT '供应商名称',
    contact_person VARCHAR(50) DEFAULT NULL COMMENT '联系人',
    phone VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    address VARCHAR(500) DEFAULT NULL COMMENT '地址',
    credit_code VARCHAR(50) DEFAULT NULL COMMENT '统一社会信用代码',
    license_image VARCHAR(500) DEFAULT NULL COMMENT '许可证图片路径',
    license_expiry_date DATE DEFAULT NULL COMMENT '许可证到期日期',
    application_type VARCHAR(20) DEFAULT 'NEW' COMMENT '申请类型：NEW-新供应商准入/MODIFY-协议修改',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '审批状态：PENDING-待审核/QUALITY_CHECKED-资质已核验/PRICE_REVIEWED-价格已审核/APPROVED-已通过/REJECTED-已驳回',
    applicant_id BIGINT NOT NULL COMMENT '申请人ID（采购员）',
    applicant_name VARCHAR(50) DEFAULT NULL COMMENT '申请人姓名',
    apply_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    quality_checker_id BIGINT DEFAULT NULL COMMENT '资质核验人ID（仓库管理员）',
    quality_checker_name VARCHAR(50) DEFAULT NULL COMMENT '资质核验人姓名',
    quality_check_time DATETIME DEFAULT NULL COMMENT '资质核验时间',
    quality_check_result VARCHAR(20) DEFAULT NULL COMMENT '资质核验结果：PASS-通过/FAIL-不通过',
    quality_check_opinion VARCHAR(500) DEFAULT NULL COMMENT '资质核验意见',
    price_reviewer_id BIGINT DEFAULT NULL COMMENT '价格审核人ID（采购/财务负责人）',
    price_reviewer_name VARCHAR(50) DEFAULT NULL COMMENT '价格审核人姓名',
    price_review_time DATETIME DEFAULT NULL COMMENT '价格审核时间',
    price_review_result VARCHAR(20) DEFAULT NULL COMMENT '价格审核结果：PASS-通过/FAIL-不通过',
    price_review_opinion VARCHAR(500) DEFAULT NULL COMMENT '价格审核意见',
    price_warning VARCHAR(500) DEFAULT NULL COMMENT '价格预警信息',
    final_approver_id BIGINT DEFAULT NULL COMMENT '最终审批人ID（超级管理员/合规岗）',
    final_approver_name VARCHAR(50) DEFAULT NULL COMMENT '最终审批人姓名',
    final_approve_time DATETIME DEFAULT NULL COMMENT '最终审批时间',
    final_approve_result VARCHAR(20) DEFAULT NULL COMMENT '最终审批结果：APPROVED-通过/REJECTED-驳回',
    final_approve_opinion VARCHAR(500) DEFAULT NULL COMMENT '最终审批意见',
    reject_reason VARCHAR(500) DEFAULT NULL COMMENT '驳回原因',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除/1-已删除',
    PRIMARY KEY (id),
    KEY idx_supplier_id (supplier_id),
    KEY idx_status (status),
    KEY idx_applicant_id (applicant_id),
    KEY idx_apply_time (apply_time),
    CONSTRAINT fk_approval_supplier FOREIGN KEY (supplier_id) REFERENCES supplier(id) ON DELETE SET NULL,
    CONSTRAINT fk_approval_applicant FOREIGN KEY (applicant_id) REFERENCES sys_user(id),
    CONSTRAINT fk_approval_quality_checker FOREIGN KEY (quality_checker_id) REFERENCES sys_user(id),
    CONSTRAINT fk_approval_price_reviewer FOREIGN KEY (price_reviewer_id) REFERENCES sys_user(id),
    CONSTRAINT fk_approval_final_approver FOREIGN KEY (final_approver_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商准入审批申请表';

-- ============================================
-- 2. 供应商准入审批明细表（协议价格明细）
-- ============================================
CREATE TABLE IF NOT EXISTS supplier_approval_item (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    application_id BIGINT NOT NULL COMMENT '审批申请ID',
    drug_id BIGINT NOT NULL COMMENT '药品ID',
    drug_name VARCHAR(200) DEFAULT NULL COMMENT '药品名称（冗余字段，便于查询）',
    proposed_price DECIMAL(10, 2) NOT NULL COMMENT '申请价格',
    reference_price DECIMAL(10, 2) DEFAULT NULL COMMENT '参考价格（集采价/市场价/历史最低价）',
    price_difference_rate DECIMAL(5, 2) DEFAULT NULL COMMENT '价格差异率（(申请价-参考价)/参考价*100）',
    price_warning_level VARCHAR(20) DEFAULT NULL COMMENT '价格预警级别：NORMAL-正常/WARNING-警告/CRITICAL-严重',
    price_difference_reason VARCHAR(500) DEFAULT NULL COMMENT '价格差异说明',
    agreement_file_url VARCHAR(500) DEFAULT NULL COMMENT '协议文件URL',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_application_id (application_id),
    KEY idx_drug_id (drug_id),
    CONSTRAINT fk_approval_item_application FOREIGN KEY (application_id) REFERENCES supplier_approval_application(id) ON DELETE CASCADE,
    CONSTRAINT fk_approval_item_drug FOREIGN KEY (drug_id) REFERENCES drug_info(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商准入审批明细表';

-- ============================================
-- 3. 供应商黑名单表
-- ============================================
CREATE TABLE IF NOT EXISTS supplier_blacklist (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    supplier_id BIGINT DEFAULT NULL COMMENT '供应商ID（可为NULL，支持按名称黑名单）',
    supplier_name VARCHAR(200) NOT NULL COMMENT '供应商名称',
    credit_code VARCHAR(50) DEFAULT NULL COMMENT '统一社会信用代码',
    blacklist_reason VARCHAR(500) NOT NULL COMMENT '列入黑名单原因',
    blacklist_type VARCHAR(20) DEFAULT 'FULL' COMMENT '黑名单类型：FULL-完全禁止/PARTIAL-部分药品禁止',
    effective_date DATE DEFAULT NULL COMMENT '生效日期',
    expiry_date DATE DEFAULT NULL COMMENT '到期日期（NULL表示永久有效）',
    creator_id BIGINT NOT NULL COMMENT '创建人ID',
    creator_name VARCHAR(50) DEFAULT NULL COMMENT '创建人姓名',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除/1-已删除',
    PRIMARY KEY (id),
    KEY idx_supplier_id (supplier_id),
    KEY idx_supplier_name (supplier_name),
    KEY idx_credit_code (credit_code),
    KEY idx_effective_date (effective_date),
    KEY idx_expiry_date (expiry_date),
    CONSTRAINT fk_blacklist_supplier FOREIGN KEY (supplier_id) REFERENCES supplier(id) ON DELETE CASCADE,
    CONSTRAINT fk_blacklist_creator FOREIGN KEY (creator_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商黑名单表';

-- ============================================
-- 4. 价格预警配置表
-- ============================================
CREATE TABLE IF NOT EXISTS price_warning_config (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    drug_id BIGINT DEFAULT NULL COMMENT '药品ID（NULL表示全局配置）',
    drug_name VARCHAR(200) DEFAULT NULL COMMENT '药品名称（冗余字段）',
    reference_price_type VARCHAR(20) DEFAULT 'MARKET' COMMENT '参考价格类型：MARKET-市场价/COLLECTIVE-集采价/HISTORY_MIN-历史最低价',
    reference_price DECIMAL(10, 2) DEFAULT NULL COMMENT '参考价格',
    warning_threshold DECIMAL(5, 2) DEFAULT 10.00 COMMENT '预警阈值（百分比，超过此值触发预警）',
    critical_threshold DECIMAL(5, 2) DEFAULT 20.00 COMMENT '严重预警阈值（百分比，超过此值触发严重预警）',
    is_active TINYINT DEFAULT 1 COMMENT '是否启用：0-禁用/1-启用',
    creator_id BIGINT DEFAULT NULL COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除/1-已删除',
    PRIMARY KEY (id),
    KEY idx_drug_id (drug_id),
    KEY idx_is_active (is_active),
    CONSTRAINT fk_price_warning_drug FOREIGN KEY (drug_id) REFERENCES drug_info(id) ON DELETE CASCADE,
    CONSTRAINT fk_price_warning_creator FOREIGN KEY (creator_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='价格预警配置表';

-- ============================================
-- 5. 审批流程日志表（详细记录每个审批环节）
-- ============================================
CREATE TABLE IF NOT EXISTS supplier_approval_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    application_id BIGINT NOT NULL COMMENT '审批申请ID',
    step_name VARCHAR(50) NOT NULL COMMENT '审批环节名称：APPLY-申请/QUALITY_CHECK-资质核验/PRICE_REVIEW-价格审核/FINAL_APPROVE-最终审批',
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    operator_name VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
    operator_role VARCHAR(50) DEFAULT NULL COMMENT '操作人角色',
    operation_type VARCHAR(20) NOT NULL COMMENT '操作类型：SUBMIT-提交/APPROVE-通过/REJECT-驳回/REVOKE-撤回',
    operation_result VARCHAR(20) DEFAULT NULL COMMENT '操作结果：PASS-通过/FAIL-不通过',
    operation_opinion VARCHAR(500) DEFAULT NULL COMMENT '操作意见',
    ip_address VARCHAR(50) DEFAULT NULL COMMENT '操作IP地址',
    operation_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_application_id (application_id),
    KEY idx_operator_id (operator_id),
    KEY idx_operation_time (operation_time),
    CONSTRAINT fk_approval_log_application FOREIGN KEY (application_id) REFERENCES supplier_approval_application(id) ON DELETE CASCADE,
    CONSTRAINT fk_approval_log_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商审批流程日志表';

-- ============================================
-- 6. 初始化价格预警全局配置
-- ============================================
INSERT INTO price_warning_config (drug_id, reference_price_type, warning_threshold, critical_threshold, is_active, creator_id) VALUES
(NULL, 'MARKET', NULL, 10.00, 20.00, 1, NULL)
ON DUPLICATE KEY UPDATE warning_threshold=VALUES(warning_threshold), critical_threshold=VALUES(critical_threshold);

