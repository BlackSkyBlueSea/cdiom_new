import Cookies from 'js-cookie'

export const getToken = () => {
  return Cookies.get('cdiom_token')
}

export const setToken = (token) => {
  Cookies.set('cdiom_token', token, { expires: 8 / 24 }) // 8小时
}

export const removeToken = () => {
  Cookies.remove('cdiom_token')
  Cookies.remove('cdiom_user')
}

export const isAuthenticated = () => {
  return !!getToken()
}

export const setUser = (user) => {
  Cookies.set('cdiom_user', JSON.stringify(user), { expires: 8 / 24 })
}

export const getUser = () => {
  const userStr = Cookies.get('cdiom_user')
  return userStr ? JSON.parse(userStr) : null
}

export const getUserRoleId = () => {
  const user = getUser()
  return user ? user.roleId : null
}


