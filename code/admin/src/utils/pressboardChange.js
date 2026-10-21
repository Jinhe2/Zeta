/** 将 TestMonitor 的压板状态转换为界面文案。 */
export function formatPressboardChangeState(state) {
  if (String(state).toUpperCase() === 'ON') return '合'
  if (String(state).toUpperCase() === 'OFF') return '分'
  return '—'
}

/** 使用浏览器本地时区显示毫秒时间戳。 */
export function formatPressboardChangeTime(timestamp) {
  const value = Number(timestamp)
  if (!Number.isFinite(value) || value <= 0) return '—'
  return new Date(value).toLocaleString()
}

export function newestPressboardChangeId(records) {
  return (Array.isArray(records) ? records : []).reduce((latest, record) => {
    const id = Number(record?.id)
    return Number.isSafeInteger(id) && id > latest ? id : latest
  }, 0)
}
