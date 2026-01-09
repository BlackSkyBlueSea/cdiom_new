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
      message.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      if (status === 401) {
        // 未授权，清除token并跳转到登录页
        Cookies.remove('cdiom_token')
        window.location.href = '/login'
      } else {
        message.error(data?.msg || `请求失败: ${status}`)
      }
    } else {
      message.error('网络错误，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default request

