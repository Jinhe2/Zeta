export function sectionStateClass(satisfied?: boolean | number | null): string {
  if (satisfied == null) return ''
  if (satisfied === -1) return ' v4-node--section-invalid'
  return satisfied === true || satisfied === 1
    ? ' v4-node--section-ok'
    : ' v4-node--section-fail'
}
