export default function NodeSectionState({ satisfied }: { satisfied?: boolean | number | null }) {
  if (satisfied == null) return null
  const state = satisfied === -1 ? 'invalid' : satisfied === true || satisfied === 1 ? 'ok' : 'fail'
  const label = state === 'invalid' ? '无效' : state === 'ok' ? '满足' : '不满足'
  return (
    <span
      className={`v4-node__state v4-node__state--${state}`}
      aria-label={label}
    >
      {label}
    </span>
  )
}
