/**
 * 与后端 @RequiresPermission 对齐：内容区按权限码（OR）决定是否 403。
 * 侧栏按「角色」展示该角色下全部导航项；无细粒度权限时点入仍走 canAccessFunctionRoute → 403 + 提示。
 * 仪表盘（dashboard）内容区不校验权限，由各卡片接口鉴权。
 */
import { PERMISSIONS } from '../utils/permission'

/** 各功能路径（与 /app/{key} 一致）所需权限：任一则可进入 */
export const FUNCTION_ROUTE_PERMISSIONS = {
  drugs: [PERMISSIONS.DRUG_VIEW, PERMISSIONS.DRUG_MANAGE, PERMISSIONS.PURCHASE_VIEW],
  /** 仅药品查看(drug:view)不应进入库存/入库页；与出库等模块一致，需本模块 view 或药品维护权限 */
  inventory: [PERMISSIONS.INVENTORY_VIEW, PERMISSIONS.DRUG_MANAGE],
  inbound: [PERMISSIONS.INBOUND_VIEW, PERMISSIONS.DRUG_MANAGE],
  outbound: [
    PERMISSIONS.OUTBOUND_VIEW,
    PERMISSIONS.OUTBOUND_APPLY,
    PERMISSIONS.OUTBOUND_APPLY_ON_BEHALF,
    PERMISSIONS.OUTBOUND_APPROVE,
    PERMISSIONS.OUTBOUND_APPROVE_SPECIAL,
    PERMISSIONS.OUTBOUND_EXECUTE,
  ],
  'purchase-orders': [
    PERMISSIONS.DRUG_VIEW,
    PERMISSIONS.DRUG_MANAGE,
    PERMISSIONS.PURCHASE_VIEW,
  ],
  suppliers: [PERMISSIONS.DRUG_VIEW, PERMISSIONS.DRUG_MANAGE],
  users: [PERMISSIONS.USER_MANAGE],
  roles: [PERMISSIONS.ROLE_MANAGE],
  configs: [PERMISSIONS.CONFIG_MANAGE],
  notices: [PERMISSIONS.NOTICE_VIEW, PERMISSIONS.NOTICE_MANAGE],
  'operation-logs': [PERMISSIONS.LOG_OPERATION_VIEW],
  'login-logs': [PERMISSIONS.LOG_LOGIN_VIEW],
  /** 供应商端：与 dashboard/supplier、采购订单、药品接口 OR 一致（不含 notice，避免非供应商误入） */
  'supplier-dashboard': [
    PERMISSIONS.PURCHASE_VIEW,
    PERMISSIONS.DRUG_VIEW,
    PERMISSIONS.DRUG_MANAGE,
  ],
  'supplier-drugs': [
    PERMISSIONS.PURCHASE_VIEW,
    PERMISSIONS.DRUG_VIEW,
    PERMISSIONS.DRUG_MANAGE,
  ],
  'supplier-orders': [
    PERMISSIONS.PURCHASE_VIEW,
    PERMISSIONS.DRUG_VIEW,
    PERMISSIONS.DRUG_MANAGE,
  ],
}

/** 侧栏展示顺序（含仪表盘；仪表盘 requirePerms 为 null 表示不校验） */
export const APP_MENU_ORDER = [
  'dashboard',
  'drugs',
  'inventory',
  'inbound',
  'outbound',
  'purchase-orders',
  'suppliers',
  'users',
  'roles',
  'configs',
  'supplier-dashboard',
  'supplier-drugs',
  'supplier-orders',
  'notices',
  'operation-logs',
  'login-logs',
]

export function getPrimaryRouteKey(pathname) {
  const raw = pathname.replace(/^\/app\/?/, '').split('/').filter(Boolean)
  return raw[0] || 'dashboard'
}

/**
 * @param {string} pathname
 * @param {(codes: string[]) => boolean} hasPermissionFn
 * @param {() => number|undefined|null} getUserRoleIdFn
 */
export function canAccessFunctionRoute(pathname, hasPermissionFn, getUserRoleIdFn) {
  const roleId = getUserRoleIdFn()
  if (roleId === 6) return true
  const key = getPrimaryRouteKey(pathname)
  if (key === 'dashboard') return true
  const codes = FUNCTION_ROUTE_PERMISSIONS[key]
  if (!codes || codes.length === 0) return false
  return hasPermissionFn(codes)
}

/** 侧栏：各菜单项对哪些 roleId 可见（与改版前角色菜单一致） */
export const MENU_ITEM_ROLES = {
  dashboard: [1, 2, 3, 4, 6],
  drugs: [2, 6],
  inventory: [2, 6],
  inbound: [2, 6],
  outbound: [2, 4, 6],
  'purchase-orders': [3, 6],
  suppliers: [3, 6],
  users: [1, 6],
  roles: [1, 6],
  configs: [1, 6],
  notices: [1, 2, 3, 4, 5],
  'operation-logs': [1, 6],
  'login-logs': [1, 6],
  'supplier-dashboard': [5],
  'supplier-drugs': [5],
  'supplier-orders': [5],
}

export const APP_MENU_LABELS = {
  dashboard: '仪表盘',
  drugs: '药品信息管理',
  inventory: '库存管理',
  inbound: '入库管理',
  outbound: '出库管理',
  'purchase-orders': '采购订单',
  suppliers: '供应商管理',
  users: '用户管理',
  roles: '角色管理',
  configs: '参数配置',
  notices: '通知公告',
  'operation-logs': '操作日志',
  'login-logs': '登录日志',
  'supplier-dashboard': '工作台',
  'supplier-drugs': '可供应药品',
  'supplier-orders': '订单管理',
}
