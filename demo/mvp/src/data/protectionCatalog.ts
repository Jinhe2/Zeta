/** 保护逻辑样本展示信息（覆盖 JSON 内较泛化的 name） */
export const PROTECTION_DISPLAY: Record<
  string,
  { title: string; description: string; category: string }
> = {
  'example.json': {
    title: '过流 I 段保护逻辑',
    description: '过流 I 段保护控制逻辑框图，含方向、电压、时间元件等典型结构。',
    category: '过流保护',
  },
  'reclose.json': {
    title: '重合闸保护逻辑',
    description: '重合闸投入条件、检同期/检无压及 TWJ 启动等逻辑。',
    category: '重合闸',
  },
  '1.json': {
    title: '差动保护逻辑',
    description: '本侧差动允许条件，含光纤同步、电流启动与电压辅助等输入。',
    category: '差动保护',
  },
}

export function getProtectionDisplay(filename: string, fallbackName?: string) {
  const meta = PROTECTION_DISPLAY[filename]
  return {
    title: meta?.title ?? fallbackName ?? filename.replace('.json', ''),
    description: meta?.description ?? '',
    category: meta?.category ?? '保护逻辑',
  }
}
