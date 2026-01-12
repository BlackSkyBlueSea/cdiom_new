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
    // 从Cookie获取token
    const token = Cookies.get('cdiom_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // 确保请求头使用UTF-8编码
    if (!config.headers['Content-Type']) {
      config.headers['Content-Type'] = 'application/json;charset=UTF-8'
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res
    } else {
      // 非200状态码，根据错误类型决定是否显示错误提示
      const errorMsg = res.msg || '请求失败'
      // 对于某些特定的错误，不显示提示（由调用方处理）
      if (res.code !== 403 && res.code !== 500) {
        message.error(errorMsg)
      }
      return Promise.reject(new Error(errorMsg))
    }
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      if (status === 401) {
        // 未授权，清除token并跳转到登录页
        Cookies.remove('cdiom_token')
        window.location.href = '/login'
        return Promise.reject(error)
      } else if (status === 403) {
        // 权限不足，不显示错误提示，让调用方处理
        return Promise.reject(error)
      } else if (status === 500) {
        // 系统错误，不显示错误提示，让调用方处理（避免刷新时频繁提示）
        return Promise.reject(error)
      } else {
        // 其他错误，显示错误信息
        message.error(data?.msg || `请求失败: ${status}`)
      }
    } else {
      // 网络错误，不显示提示，让调用方处理
    }
    return Promise.reject(error)
  }
)

export default request

