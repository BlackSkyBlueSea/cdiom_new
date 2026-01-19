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

// 获取token（优先从sessionStorage，其次从Cookie）
export const getToken = () => {
  if (isMultiLoginMode()) {
    return sessionStorage.getItem('cdiom_token')
  }
  return Cookies.get('cdiom_token')
}

// 设置token（根据模式选择存储方式）
export const setToken = (token, useSessionStorage = false) => {
  if (useSessionStorage || isMultiLoginMode()) {
    sessionStorage.setItem('cdiom_token', token)
    // 设置标记，表示这是多用户登录模式
    sessionStorage.setItem('cdiom_multi_login', 'true')
  } else {
    Cookies.set('cdiom_token', token, { expires: 8 / 24 }) // 8小时
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

// 设置用户信息
export const setUser = (user, useSessionStorage = false) => {
  const userStr = JSON.stringify(user)
  if (useSessionStorage || isMultiLoginMode()) {
    sessionStorage.setItem('cdiom_user', userStr)
  } else {
    Cookies.set('cdiom_user', userStr, { expires: 8 / 24 })
  }
}

// 获取用户信息
export const getUser = () => {
  let userStr = null
  if (isMultiLoginMode()) {
    userStr = sessionStorage.getItem('cdiom_user')
  } else {
    userStr = Cookies.get('cdiom_user')
  }
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

