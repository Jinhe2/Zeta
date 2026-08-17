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
