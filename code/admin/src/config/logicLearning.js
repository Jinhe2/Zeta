// 仅控制组合学习入口展示，保留原页面、路由、接口和实验处理逻辑。
export const SHOW_LOGIC_GROUP_ENTRY = false

export function resolveLogicLearningMode(mode) {
  if (!SHOW_LOGIC_GROUP_ENTRY && mode === 'group') return 'basic'
  return mode ?? 'basic'
}
