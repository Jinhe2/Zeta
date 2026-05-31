export default function NodeSectionState({ satisfied }: { satisfied?: boolean | null }) {
  if (satisfied == null) return null
  return (
    <span
      className={`v4-node__state v4-node__state--${satisfied ? 'ok' : 'fail'}`}
      aria-label={satisfied ? '满足' : '不满足'}
    >
      {satisfied ? '满足' : '不满足'}
    </span>
  )
}
