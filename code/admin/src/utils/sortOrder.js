export function normalizeSortOrder(value) {
  if (value === '' || value === null || value === undefined) {
    return null
  }
  return Number(value)
}
