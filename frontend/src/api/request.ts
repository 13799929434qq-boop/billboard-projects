import axios from 'axios'
import type { AxiosInstance, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

interface ApiBody<T = unknown> {
  code: number
  msg: string
  data: T
}

const request: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('gb_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (resp: AxiosResponse<ApiBody>) => {
    if (resp.config.responseType === 'blob') {
      return resp
    }
    const body = resp.data
    if (body && body.code === 0) {
      return body.data as never
    }
    ElMessage.error(body?.msg || '请求失败')
    return Promise.reject(new Error(body?.msg || '请求失败'))
  },
  (err) => {
    const status = err.response?.status
    const msg = err.response?.data?.msg || err.message || '网络错误'
    if (status === 401) {
      localStorage.removeItem('gb_token')
      localStorage.removeItem('gb_user')
      if (router.currentRoute.value.path !== '/login') {
        ElMessage.warning('登录已过期，请重新登录')
        router.push('/login')
      }
    } else {
      ElMessage.error(msg)
    }
    return Promise.reject(err)
  }
)

export default request
