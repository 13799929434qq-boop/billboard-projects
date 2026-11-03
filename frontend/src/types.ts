export type Role = 'admin' | 'archivist' | 'finance' | 'leader'

export const ROLE_NAMES: Record<string, string> = {
  admin: '系统管理员',
  archivist: '运营/档案员',
  finance: '合同/财务',
  leader: '领导'
}

export interface UserInfo {
  username: string
  name: string
  role: Role | string
}

export interface SysUser {
  id: number
  username: string
  name: string
  role: string
  status: string
  createdAt: string
}

export interface Section {
  id: number
  code: string
  name: string
  region: string
  operator: string
  mileage: string
  createdAt: string
}

export interface Billboard {
  id: number
  code: string
  sectionId: number
  side: string
  stakeContract: string
  stakeActual: string
  stakeApproval: string
  spec: string
  width: number
  height: number
  ownership: string
  landNature: string
  builder: string
  buildYear: number
  status: string
  permitNo: string
  permitExpire: string
  insuranceNo: string
  insuranceExpire: string
  annualCheckNo: string
  annualCheckDate: string
  annualCheckExpire: string
  photos: string
  remark: string
  sectionName?: string
  renterName?: string
  contractCode?: string
}

export interface BillboardChange {
  id: number
  billboardId: number
  changeType: string
  changeDate: string
  beforeValue: string
  afterValue: string
  reason: string
  operator: string
  createdAt: string
  billboardCode?: string
  sectionName?: string
}

export interface Merchant {
  id: number
  name: string
  contact: string
  phone: string
  address: string
  taxNo: string
  bankName: string
  bankAccount: string
  blacklisted: boolean
  remark: string
  createdAt: string
}

export interface ContractItem {
  id?: number
  contractId?: number
  billboardId: number | null
  yearlyPrice: number | null
  billboardCode?: string
  sectionName?: string
}

export interface Contract {
  id: number
  code: string
  merchantId: number | null
  signDate: string
  startDate: string
  endDate: string
  years: number
  totalAmount: number
  status: string
  remark: string
  createdAt?: string
  merchantName?: string
  items?: ContractItem[]
}

export interface Receivable {
  id: number
  contractId: number
  merchantId: number
  billboardId: number
  year: number
  amount: number
  received: number
  status: string
  merchantName?: string
  contractCode?: string
  billboardCode?: string
  sectionName?: string
}

export interface FundTransaction {
  id: number
  txDate: string
  direction: string
  category: string
  amount: number
  merchantId: number | null
  contractId: number | null
  summary: string
  operator: string
  createdAt?: string
  merchantName?: string
  contractCode?: string
}

export interface Dict {
  type: string
  name: string
  items: DictItem[]
}

export interface DictItem {
  id: number
  dictType: string
  label: string
  sort: number
}

export interface OperationLog {
  id: number
  time: string
  username: string
  name: string
  action: string
  detail: string
}

export interface LookupData {
  sections: Section[]
  billboards: Billboard[]
  merchants: Merchant[]
  contracts: Contract[]
}

export interface Stats {
  billboardCount: number
  rentedCount: number
  idleCount: number
  merchantCount: number
  blacklistCount: number
  activeContractCount: number
  draftContractCount: number
  totalReceivable: number
  totalReceived: number
  totalArrears: number
  arrearsMerchantCount: number
  sectionCount: number
  userCount: number
  expiringContracts: Array<Record<string, unknown>>
  expiringCompliance: Array<Record<string, unknown>>
}

export type ReportRow = Record<string, string | number | null>
