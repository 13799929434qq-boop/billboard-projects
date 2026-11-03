/** 金额千分位格式化 */
export function fmtMoney(n: number | string | null | undefined): string {
  if (n === null || n === undefined || n === '') {
    return '0.00'
  }
  const num = typeof n === 'string' ? parseFloat(n) : n
  if (isNaN(num)) {
    return '0.00'
  }
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 状态 → 标签颜色类型 */
export function statusTagType(status: string): 'success' | 'danger' | 'warning' | 'info' | 'primary' {
  switch (status) {
    case '已收':
    case '生效':
    case '在租':
      return 'success'
    case '未收':
    case '解除':
    case '停用':
      return 'danger'
    case '部分缴纳':
    case '维护中':
    case '到期':
      return 'warning'
    case '空闲':
    case '草稿':
      return 'info'
    default:
      return 'primary'
  }
}

/** 日期是否临期（距今天不足 days 天） */
export function isExpiring(dateStr: string | null | undefined, days = 90): boolean {
  if (!dateStr) {
    return false
  }
  const target = new Date(dateStr).getTime()
  const now = Date.now()
  const diffDays = (target - now) / (1000 * 60 * 60 * 24)
  return diffDays >= 0 && diffDays <= days
}

/** 日期是否已过期 */
export function isExpired(dateStr: string | null | undefined): boolean {
  if (!dateStr) {
    return false
  }
  return new Date(dateStr).getTime() < Date.now()
}

/** 将当前页面筛选结果导出为 Excel 可直接打开的 UTF-8 CSV。 */
export function exportCsv(filename: string, header: string[], rows: unknown[][]): void {
  const escapeCell = (cell: unknown) => `"${String(cell ?? '').replace(/"/g, '""')}"`
  const csv = '\ufeff' + [header, ...rows].map((line) => line.map(escapeCell).join(',')).join('\r\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = filename.endsWith('.csv') ? filename : `${filename}.csv`
  link.click()
  URL.revokeObjectURL(url)
}
