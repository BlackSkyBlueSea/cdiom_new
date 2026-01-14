import { getUserRoleId, getUser } from './auth'
import request from './request'

/**
 * 权限代码定义
 * 与后端权限代码保持一致
 */
export const PERMISSIONS = {
  // 用户管理
  USER_MANAGE: 'user:manage',
  USER_VIEW: 'user:view',
  USER_CREATE: 'user:create',
  USER_UPDATE: 'user:update',
  USER_DELETE: 'user:delete',
  
  // 角色管理
  ROLE_MANAGE: 'role:manage',
  ROLE_VIEW: 'role:view',
  ROLE_CREATE: 'role:create',
  ROLE_UPDATE: 'role:update',
  ROLE_DELETE: 'role:delete',
  
  // 药品管理
  DRUG_VIEW: 'drug:view',
  DRUG_MANAGE: 'drug:manage',
  DRUG_CREATE: 'drug:create',
  DRUG_UPDATE: 'drug:update',
  DRUG_DELETE: 'drug:delete',
  
  // 供应商管理
  SUPPLIER_AUDIT: 'supplier:audit',
  
  // 系统配置
  CONFIG_MANAGE: 'config:manage',
  CONFIG_VIEW: 'config:view',
  CONFIG_CREATE: 'config:create',
  CONFIG_UPDATE: 'config:update',
  CONFIG_DELETE: 'config:delete',
  
  // 通知公告
  NOTICE_VIEW: 'notice:view',
  NOTICE_MANAGE: 'notice:manage',
  NOTICE_CREATE: 'notice:create',
  NOTICE_UPDATE: 'notice:update',
  NOTICE_DELETE: 'notice:delete',
  
  // 日志查看
  LOG_OPERATION_VIEW: 'log:operation:view',
  LOG_LOGIN_VIEW: 'log:login:view',
  
  // 出库管理权限
  OUTBOUND_VIEW: 'outbound:view',
  OUTBOUND_APPLY: 'outbound:apply',
  OUTBOUND_APPROVE: 'outbound:approve',
  OUTBOUND_APPROVE_SPECIAL: 'outbound:approve:special',
  OUTBOUND_EXECUTE: 'outbound:execute',
  OUTBOUND_REJECT: 'outbound:reject',
  
  // 入库管理权限
  INBOUND_VIEW: 'inbound:view',
  INBOUND_CREATE: 'inbound:create',
  INBOUND_APPROVE: 'inbound:approve',
  INBOUND_EXECUTE: 'inbound:execute',
  
  // 库存管理权限
  INVENTORY_VIEW: 'inventory:view',
  INVENTORY_ADJUST: 'inventory:adjust',
  INVENTORY_ADJUST_APPROVE: 'inventory:adjust:approve',
}

/**
 * 角色权限映射
 * 定义每个角色拥有的权限代码
 * 系统管理员只负责系统功能，不涉及业务功能
 * 超级管理员（roleId=6）拥有所有权限，通过代码判断，不需要在此配置
 */
const ROLE_PERMISSIONS = {
  1: [ // 系统管理员 - 只拥有系统功能权限
    PERMISSIONS.USER_MANAGE,
    PERMISSIONS.USER_VIEW,
    PERMISSIONS.USER_CREATE,
    PERMISSIONS.USER_UPDATE,
    PERMISSIONS.USER_DELETE,
    PERMISSIONS.ROLE_MANAGE,
    PERMISSIONS.ROLE_VIEW,
    PERMISSIONS.ROLE_CREATE,
    PERMISSIONS.ROLE_UPDATE,
    PERMISSIONS.ROLE_DELETE,
    PERMISSIONS.CONFIG_MANAGE,
    PERMISSIONS.CONFIG_VIEW,
    PERMISSIONS.CONFIG_CREATE,
    PERMISSIONS.CONFIG_UPDATE,
    PERMISSIONS.CONFIG_DELETE,
    PERMISSIONS.NOTICE_VIEW,
    PERMISSIONS.NOTICE_MANAGE,
    PERMISSIONS.NOTICE_CREATE,
    PERMISSIONS.NOTICE_UPDATE,
    PERMISSIONS.NOTICE_DELETE,
    PERMISSIONS.LOG_OPERATION_VIEW,
    PERMISSIONS.LOG_LOGIN_VIEW,
  ],
  6: [ // 超级管理员 - 拥有所有权限（通过代码判断，这里配置所有权限代码）
    PERMISSIONS.USER_MANAGE,
    PERMISSIONS.USER_VIEW,
    PERMISSIONS.USER_CREATE,
    PERMISSIONS.USER_UPDATE,
    PERMISSIONS.USER_DELETE,
    PERMISSIONS.ROLE_MANAGE,
    PERMISSIONS.ROLE_VIEW,
    PERMISSIONS.ROLE_CREATE,
    PERMISSIONS.ROLE_UPDATE,
    PERMISSIONS.ROLE_DELETE,
    PERMISSIONS.DRUG_VIEW,
    PERMISSIONS.DRUG_MANAGE,
    PERMISSIONS.DRUG_CREATE,
    PERMISSIONS.DRUG_UPDATE,
    PERMISSIONS.DRUG_DELETE,
    PERMISSIONS.SUPPLIER_AUDIT,
    PERMISSIONS.CONFIG_MANAGE,
    PERMISSIONS.CONFIG_VIEW,
    PERMISSIONS.CONFIG_CREATE,
    PERMISSIONS.CONFIG_UPDATE,
    PERMISSIONS.CONFIG_DELETE,
    PERMISSIONS.NOTICE_VIEW,
    PERMISSIONS.NOTICE_MANAGE,
    PERMISSIONS.NOTICE_CREATE,
    PERMISSIONS.NOTICE_UPDATE,
    PERMISSIONS.NOTICE_DELETE,
    PERMISSIONS.LOG_OPERATION_VIEW,
    PERMISSIONS.LOG_LOGIN_VIEW,
  ],
  2: [ // 仓库管理员
    PERMISSIONS.DRUG_VIEW,
    PERMISSIONS.DRUG_MANAGE,
    PERMISSIONS.DRUG_CREATE,
    PERMISSIONS.DRUG_UPDATE,
    PERMISSIONS.DRUG_DELETE,
    PERMISSIONS.SUPPLIER_AUDIT, // 供应商审核权限
    PERMISSIONS.NOTICE_VIEW,
    PERMISSIONS.NOTICE_CREATE,
  ],
  3: [ // 采购专员
    PERMISSIONS.DRUG_VIEW, // 查看供应商列表
    PERMISSIONS.DRUG_MANAGE, // 创建、更新供应商（但不包括删除和审核）
    PERMISSIONS.NOTICE_VIEW,
    PERMISSIONS.NOTICE_CREATE,
  ],
  4: [ // 医护人员
    PERMISSIONS.NOTICE_VIEW,
    PERMISSIONS.NOTICE_CREATE,
  ],
  5: [ // 供应商
    PERMISSIONS.NOTICE_VIEW,
    PERMISSIONS.NOTICE_CREATE,
  ],
}

// 缓存用户权限，避免频繁请求
let cachedUserPermissions = null
let cachedUserId = null

/**
 * 从后端获取当前用户的权限列表
 */
export const fetchUserPermissions = async () => {
  try {
    const user = getUser()
    if (!user) {
      return []
    }
    
    // 如果用户ID没变，使用缓存
    if (cachedUserPermissions && cachedUserId === user.id) {
      return cachedUserPermissions
    }
    
    const res = await request.get('/auth/permissions')
    if (res.code === 200) {
      cachedUserPermissions = res.data || []
      cachedUserId = user.id
      return cachedUserPermissions
    }
    return []
  } catch (error) {
    console.error('获取用户权限失败:', error)
    // 如果后端请求失败，回退到基于角色的权限判断
    const roleId = getUserRoleId()
    if (!roleId) {
      return []
    }
    return ROLE_PERMISSIONS[roleId] || []
  }
}

/**
 * 清除权限缓存（登录/登出时调用）
 */
export const clearPermissionCache = () => {
  cachedUserPermissions = null
  cachedUserId = null
}

/**
 * 获取当前用户的权限列表
 * 优先从后端获取，失败则使用角色权限
 */
export const getUserPermissions = () => {
  // 如果已有缓存，直接返回
  if (cachedUserPermissions) {
    return cachedUserPermissions
  }
  
  // 否则使用基于角色的权限（向后兼容）
  const roleId = getUserRoleId()
  if (!roleId) {
    return []
  }
  return ROLE_PERMISSIONS[roleId] || []
}

/**
 * 检查用户是否有指定权限
 * @param {string|string[]} permissionCodes - 权限代码或权限代码数组
 * @returns {boolean}
 */
export const hasPermission = (permissionCodes) => {
  const userPermissions = getUserPermissions()
  const roleId = getUserRoleId()
  
  // 超级管理员（roleId=6）拥有所有权限
  if (roleId === 6) {
    return true
  }
  
  // 如果权限列表包含通配符，表示拥有所有权限
  if (userPermissions.includes('*')) {
    return true
  }
  
  const codes = Array.isArray(permissionCodes) ? permissionCodes : [permissionCodes]
  return codes.some(code => userPermissions.includes(code))
}

/**
 * 检查用户是否有所有指定权限
 * @param {string|string[]} permissionCodes - 权限代码或权限代码数组
 * @returns {boolean}
 */
export const hasAllPermissions = (permissionCodes) => {
  const userPermissions = getUserPermissions()
  const roleId = getUserRoleId()
  
  // 超级管理员（roleId=6）拥有所有权限
  if (roleId === 6) {
    return true
  }
  
  const codes = Array.isArray(permissionCodes) ? permissionCodes : [permissionCodes]
  return codes.every(code => userPermissions.includes(code))
}

/**
 * 权限控制组件（HOC）
 * 用于控制组件的显示/隐藏
 */
export const PermissionWrapper = ({ permission, children, fallback = null }) => {
  if (hasPermission(permission)) {
    return children
  }
  return fallback
}


