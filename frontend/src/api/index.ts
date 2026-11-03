import request from './request'
import type {
  Billboard,
  BillboardChange,
  Contract,
  Dict,
  FundTransaction,
  LookupData,
  Merchant,
  OperationLog,
  Receivable,
  ReportRow,
  Section,
  Stats,
  SysUser,
  UserInfo
} from '../types'

// ---------- 认证 ----------
export const authApi = {
  login: (username: string, password: string) =>
    request.post('/auth/login', { username, password }) as Promise<{ token: string; user: UserInfo }>,
  me: () => request.get('/auth/me') as Promise<UserInfo>,
  logout: () => request.post('/auth/logout') as Promise<null>
}

// ---------- 路段 ----------
export const sectionApi = {
  list: (keyword?: string) => request.get('/sections', { params: { keyword } }) as Promise<Section[]>,
  create: (data: Partial<Section>) => request.post('/sections', data) as Promise<Section>,
  update: (id: number, data: Partial<Section>) => request.put(`/sections/${id}`, data) as Promise<Section>,
  remove: (id: number) => request.delete(`/sections/${id}`) as Promise<null>
}

// ---------- 广告牌 ----------
export interface BillboardQuery {
  keyword?: string
  sectionId?: number
  status?: string
  ownership?: string
  landNature?: string
}
export const billboardApi = {
  list: (params: BillboardQuery) => request.get('/billboards', { params }) as Promise<Billboard[]>,
  create: (data: Partial<Billboard>) => request.post('/billboards', data) as Promise<Billboard>,
  update: (id: number, data: Partial<Billboard>) => request.put(`/billboards/${id}`, data) as Promise<Billboard>,
  remove: (id: number) => request.delete(`/billboards/${id}`) as Promise<null>
}

export const billboardChangeApi = {
  list: (params: { billboardId?: number; changeType?: string } = {}) =>
    request.get('/billboard-changes', { params }) as Promise<BillboardChange[]>,
  create: (data: Partial<BillboardChange>) => request.post('/billboard-changes', data) as Promise<BillboardChange>
}

// ---------- 商家 ----------
export const merchantApi = {
  list: (keyword?: string, blacklisted?: boolean) =>
    request.get('/merchants', { params: { keyword, blacklisted } }) as Promise<Merchant[]>,
  create: (data: Partial<Merchant>) => request.post('/merchants', data) as Promise<Merchant>,
  update: (id: number, data: Partial<Merchant>) => request.put(`/merchants/${id}`, data) as Promise<Merchant>,
  remove: (id: number) => request.delete(`/merchants/${id}`) as Promise<null>
}

// ---------- 合同 ----------
export const contractApi = {
  list: (params: { keyword?: string; status?: string; merchantId?: number }) =>
    request.get('/contracts', { params }) as Promise<Contract[]>,
  create: (data: Partial<Contract>) => request.post('/contracts', data) as Promise<Contract>,
  update: (id: number, data: Partial<Contract>) => request.put(`/contracts/${id}`, data) as Promise<Contract>,
  remove: (id: number) => request.delete(`/contracts/${id}`) as Promise<null>
}

// ---------- 应收 ----------
export const receivableApi = {
  list: (params: { merchantId?: number; status?: string; year?: number }) =>
    request.get('/receivables', { params }) as Promise<Receivable[]>,
  pay: (id: number, amount: number) => request.post(`/receivables/${id}/pay`, { amount }) as Promise<Receivable>
}

// ---------- 资金流水 ----------
export const transactionApi = {
  list: (params: { direction?: string; category?: string; from?: string; to?: string; keyword?: string }) =>
    request.get('/transactions', { params }) as Promise<FundTransaction[]>,
  create: (data: Partial<FundTransaction>) => request.post('/transactions', data) as Promise<FundTransaction>,
  update: (id: number, data: Partial<FundTransaction>) => request.put(`/transactions/${id}`, data) as Promise<FundTransaction>,
  remove: (id: number) => request.delete(`/transactions/${id}`) as Promise<null>
}

// ---------- 数据字典 ----------
export const dictApi = {
  list: () => request.get('/dicts') as Promise<Dict[]>,
  addItem: (type: string, label: string) => request.post(`/dicts/${type}/items`, { label }) as Promise<Dict>,
  updateItem: (id: number, data: { label: string; sort: number }) => request.put(`/dicts/items/${id}`, data) as Promise<Dict>,
  removeItem: (id: number) => request.delete(`/dicts/items/${id}`) as Promise<null>
}

// ---------- 用户 ----------
export const userApi = {
  list: () => request.get('/users') as Promise<SysUser[]>,
  create: (data: Record<string, unknown>) => request.post('/users', data) as Promise<SysUser>,
  update: (id: number, data: Record<string, unknown>) => request.put(`/users/${id}`, data) as Promise<SysUser>
}

// ---------- 日志 ----------
export const logApi = {
  list: (keyword?: string) => request.get('/logs', { params: { keyword } }) as Promise<OperationLog[]>
}

// ---------- 下拉联动 ----------
export const lookupApi = {
  all: () => request.get('/lookup') as Promise<LookupData>
}

// ---------- 工作台指标 ----------
export const statsApi = {
  get: () => request.get('/stats') as Promise<Stats>
}

// ---------- 报表 ----------
export const reportApi = {
  billboardStats: (keyword?: string) =>
    request.get('/reports/billboards', { params: { keyword } }) as Promise<Record<string, unknown>>,
  contractExecution: () => request.get('/reports/contracts') as Promise<ReportRow[]>,
  occupancy: () => request.get('/reports/occupancy') as Promise<Record<string, unknown>[]>,
  collection: (from?: string, to?: string) =>
    request.get('/reports/collection', { params: { from, to } }) as Promise<Record<string, unknown>>,
  receivableStats: () => request.get('/reports/receivables') as Promise<Record<string, unknown>>
}

/** 报表/文件下载（携带 JWT，fetch + blob 方式） */
export async function downloadFile(path: string, filename: string): Promise<void> {
  const resp = await fetch(`/api${path}`, {
    headers: { Authorization: `Bearer ${localStorage.getItem('gb_token') || ''}` }
  })
  if (!resp.ok) {
    throw new Error('导出失败')
  }
  const blob = await resp.blob()
  const blobUrl = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = blobUrl
  link.download = filename
  link.click()
  window.URL.revokeObjectURL(blobUrl)
}
