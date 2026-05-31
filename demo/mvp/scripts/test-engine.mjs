import { readFileSync, readdirSync } from 'fs'
import { fileURLToPath } from 'url'
import { dirname, join } from 'path'

const root = join(dirname(fileURLToPath(import.meta.url)), '../..')
const { buildDiagram } = await import('../src/engine/index.js')

for (const f of readdirSync(join(root, '../../samples/fixed')).filter((x) => x.endsWith('.json'))) {
  const config = JSON.parse(readFileSync(join(root, '../../samples/fixed', f), 'utf8'))
  try {
    const d = buildDiagram(config)
    console.log(f, 'OK', d.bounds, Object.keys(d.nodes).length, 'nodes', d.wires.length, 'wires')
  } catch (e) {
    console.error(f, 'FAIL', e)
  }
}
