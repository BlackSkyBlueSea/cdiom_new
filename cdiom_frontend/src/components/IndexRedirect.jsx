import { Navigate } from 'react-router-dom'
import { getUserRoleId } from '../utils/auth'

const IndexRedirect = () => {
  const roleId = getUserRoleId()
  
  // 供应商角色跳转到供应商工作台
  if (roleId === 5) {
    return <Navigate to="/app/supplier-dashboard" replace />
  }
  
  // 其他角色跳转到普通仪表盘
  return <Navigate to="/app/dashboard" replace />
}

export default IndexRedirect




