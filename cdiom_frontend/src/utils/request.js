import axios from 'axios'
import Cookies from 'js-cookie'
import { message } from 'antd'

// 创建axios实例
const request = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
  },
  responseType: 'json',
  responseEncoding: 'utf8',
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // 优先从sessionStorage获取token（多用户登录模式），其次从Cookie获取
    let token = sessionStorage.getItem('cdiom_token')
    if (!token) {
      token = Cookies.get('cdiom_token')
    }
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // FormData 须让浏览器/axios 自动带 boundary；axios 约定将 Content-Type 设为 false 可跳过默认的 application/json
    if (config.data instanceof FormData) {
      config.headers['Content-Type'] = false
      return config
    }
    if (!config.headers['Content-Type']) {
      config.headers['Content-Type'] = 'application/json;charset=UTF-8'
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 登录过期时清除 token 并跳转首页（不弹错误提示，避免“数据加载失败”等干扰）
function clearAuthAndRedirect() {
  Cookies.remove('cdiom_token')
  sessionStorage.removeItem('cdiom_token')
  sessionStorage.removeItem('cdiom_user')
  sessionStorage.removeItem('cdiom_multi_login')
  window.location.href = '/'
}

// 判断是否为“未登录/Token 过期”类错误（需自动跳转首页）
function isAuthExpiredCode(code, msg) {
  if (code === 401) return true
  if (code === 403 && msg && (msg.includes('未登录') || msg.includes('Token已过期') || msg.includes('登录已过期'))) return true
  return false
}

/** 细粒度权限不足（非登录过期），页面可静默处理、避免 logger.error 刷屏 */
function rejectPermissionDenied(messageText) {
  const err = new Error(messageText || '权限不足')
  err.isPermissionForbidden = true
  return Promise.reject(err)
}

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res
    }
    // 业务 code 为 401 或 403 且为未登录/Token 过期：静默跳转首页，不弹错误
    if (isAuthExpiredCode(res.code, res.msg)) {
      clearAuthAndRedirect()
      return Promise.reject(new Error(res.msg || '登录已过期'))
    }
    // 其他非 200：按原逻辑决定是否提示
    const errorMsg = res.msg || '请求失败'
    if (res.code === 403) {
      message.warning(errorMsg)
      return rejectPermissionDenied(errorMsg)
    }
    if (res.code !== 500) {
      message.error(errorMsg)
    }
    return Promise.reject(new Error(errorMsg))
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      if (status === 401) {
        clearAuthAndRedirect()
        return Promise.reject(error)
      }
      if (status === 403 && isAuthExpiredCode(data?.code, data?.msg)) {
        clearAuthAndRedirect()
        return Promise.reject(error)
      }
      if (status === 403) {
        const msg =
          (typeof data === 'object' && data && data.msg) ||
          '权限不足，无法访问该资源'
        message.warning(msg)
        error.isPermissionForbidden = true
        return Promise.reject(error)
      }
      if (status === 500) {
        return Promise.reject(error)
      }
      if (status === 413) {
        message.error(
          (typeof data === 'object' && data?.msg) ||
            '上传内容过大，请使用更小的文件（例如单张图片不超过 10MB）'
        )
        return Promise.reject(error)
      }
      message.error(data?.msg || `请求失败: ${status}`)
    }
    return Promise.reject(error)
  }
)

/** 业务 code=403 或 HTTP 403（权限不足）时 request 拦截器会打上此标记 */
export function isPermissionForbiddenError(error) {
  return Boolean(error?.isPermissionForbidden)
}

export default request

