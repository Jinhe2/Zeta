export function canStartAfterSettingCheck(result) {
  return result?.status === 'MATCHED' || result?.status === 'SKIPPED'
}

export function buildSettingMismatchDialog(settingCheck) {
  return {
    open: true,
    title: '定值校核未通过',
    message: '当前装置定值与定值清单不一致，请重新整定定值后再开始实验。',
    baselineItems: (settingCheck?.items || []).filter((item) => !item.equal).map((item, index) => ({
      key: `${item.settingRef}-${index}`,
      name: item.settingName || item.settingRef,
      baselineValue: `${item.baselineValue ?? '—'}${item.valueUnit ? ` ${item.valueUnit}` : ''}`,
      actualValue: item.matched
        ? `${item.actualValue ?? '—'}${item.valueUnit ? ` ${item.valueUnit}` : ''}`
        : '未召唤到',
    })),
  }
}

export function canStartAfterExperimentPrecheck(result) {
  return result?.status === 'MATCHED' || result?.status === 'SKIPPED'
}

const WIRING_CATEGORY_LABELS = { VOLTAGE: '电压', CURRENT: '电流' }

export function buildWiringRequirementItems(wiringCheck) {
  return (wiringCheck?.categories || []).flatMap((category) =>
    (category.groups || [])
      .filter((group) => !group.passed)
      .map((group) => ({
        key: `${category.category}-${group.groupNo}`,
        name: `${WIRING_CATEGORY_LABELS[category.category] || category.category} · 第 ${(group.groupNo ?? 0) + 1} 组`,
        message: group.message || '接线错误',
        detail: (group.phases || [])
          .map((phase) => `${phase.position}相（${phase.terminalLabel || '—'}）：${phase.actualOutput || '未接线'}`)
          .join('；'),
      })),
  )
}

export function buildExperimentPrecheckMismatchDialog(result) {
  const settingDialog = buildSettingMismatchDialog(result?.settingCheck)
  return {
    open: true,
    title: '基准校核未通过',
    message: '当前装置存在不满足基准要求的项目，请人工检查并调整后重新开始实验。',
    baselineItems: settingDialog.baselineItems,
    softPressboardItems: (result?.softPressboardCheck?.items || [])
      .filter((item) => !item.equal)
      .map((item, index) => ({
        key: `${item.pressboardRef}-${index}`,
        name: item.pressboardName || item.pressboardRef,
        baselineValue: item.baselineValue ?? '—',
        actualValue: item.matched ? (item.actualValue ?? '—') : '未召唤到',
      })),
    hardPressboardItems: (result?.hardPressboardCheck?.items || [])
      .filter((item) => !item.equal)
      .map((item, index) => ({
        key: `${item.pressboardRef}-${index}`,
        name: item.pressboardName || item.pressboardRef,
        baselineValue: item.baselineValue ?? '—',
        actualValue: item.matched ? (item.actualValue ?? '—') : '未召唤到',
      })),
    wiringItems: buildWiringRequirementItems(result?.wiringCheck),
  }
}

export function buildExperimentStartConfirmDialog() {
  return {
    open: true,
    kind: 'start-confirm',
    title: '实验开始确认',
    message: '请确认测试仪和装置之间的加量回路连接正常，点击确认即可开始实验',
  }
}
