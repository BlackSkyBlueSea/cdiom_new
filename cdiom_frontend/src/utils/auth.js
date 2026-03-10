import Cookies from 'js-cookie'

// 检查是否是多用户登录模式（通过URL参数或sessionStorage标记）
const isMultiLoginMode = () => {
  // 检查URL参数（登录页面使用）
  const urlParams = new URLSearchParams(window.location.search)
  if (urlParams.get('multiLogin') === 'true') {
    return true
  }
  // 检查sessionStorage中是否有multi_login标记（登录时设置）
  if (sessionStorage.getItem('cdiom_multi_login') === 'true') {
    return true
  }
  // 如果sessionStorage中有token但Cookie中没有，说明是多用户登录模式
  if (sessionStorage.getItem('cdiom_token') && !Cookies.get('cdiom_token')) {
    return true
  }
  return false
}

// 获取token（优先从 sessionStorage，其次从 Cookie，与 request 拦截器一致，避免多标签时用错身份）
export const getToken = () => {
  return sessionStorage.getItem('cdiom_token') || Cookies.get('cdiom_token')
}

// 设置token（根据模式选择存储方式）
// 始终写入 sessionStorage，保证「当前标签页」身份不被其他标签的 Cookie 覆盖（多用户同设备时审批人等信息正确）
export const setToken = (token, useSessionStorage = false) => {
  sessionStorage.setItem('cdiom_token', token)
  if (useSessionStorage || isMultiLoginMode()) {
    sessionStorage.setItem('cdiom_multi_login', 'true')
    // 多用户模式：仅当前标签，不写 Cookie，避免覆盖其他标签的 Cookie
  } else {
    Cookies.set('cdiom_token', token, { expires: 8 / 24 }) // 8小时，兼容单用户/刷新
  }
}

// 移除token
export const removeToken = () => {
  Cookies.remove('cdiom_token')
  Cookies.remove('cdiom_user')
  sessionStorage.removeItem('cdiom_token')
  sessionStorage.removeItem('cdiom_user')
  sessionStorage.removeItem('cdiom_multi_login')
}

// 检查是否已认证
export const isAuthenticated = () => {
  return !!getToken()
}

// 设置用户信息（始终写入 sessionStorage，与 setToken 一致，避免多标签时 Cookie 被覆盖导致当前用户/审批人错乱）
export const setUser = (user, useSessionStorage = false) => {
  const userStr = JSON.stringify(user)
  sessionStorage.setItem('cdiom_user', userStr)
  if (!useSessionStorage && !isMultiLoginMode()) {
    Cookies.set('cdiom_user', userStr, { expires: 8 / 24 })
  }
}

// 获取用户信息（优先 sessionStorage，其次 Cookie，与 token 一致，保证当前标签页身份一致）
export const getUser = () => {
  const userStr = sessionStorage.getItem('cdiom_user') || Cookies.get('cdiom_user')
  return userStr ? JSON.parse(userStr) : null
}

export const getUserRoleId = () => {
  const user = getUser()
  return user ? user.roleId : null
}

// 获取用户信息（别名，用于兼容）
export const getUserInfo = () => {
  return getUser()
}

// 清除认证信息（别名，用于兼容）
export const clearAuth = () => {
  removeToken()
}

