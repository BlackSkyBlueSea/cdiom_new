import Cookies from 'js-cookie'

export const getToken = () => {
  return Cookies.get('cdiom_token')
}

export const setToken = (token) => {
  Cookies.set('cdiom_token', token, { expires: 8 / 24 }) // 8小时
}

export const removeToken = () => {
  Cookies.remove('cdiom_token')
}

export const isAuthenticated = () => {
  return !!getToken()
}


