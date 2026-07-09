export function sectionStateClass(satisfied?: boolean | null): string {
  if (satisfied == null) return ''
  return satisfied ? ' v4-node--section-ok' : ' v4-node--section-fail'
}
