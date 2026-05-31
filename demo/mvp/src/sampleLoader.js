export const SAMPLE_FILES = ['1.json', 'example.json', 'reclose.json']

export async function loadSampleList() {
  const list = await Promise.all(
    SAMPLE_FILES.map(async (filename) => {
      const res = await fetch(`/samples/${filename}`)
      if (!res.ok) throw new Error(`加载样本失败: ${filename}`)
      const data = await res.json()
      return {
        filename,
        name: data.name || filename.replace('.json', ''),
        data,
      }
    }),
  )
  return list.sort((a, b) => a.filename.localeCompare(b.filename))
}

export async function loadSample(filename) {
  const res = await fetch(`/samples/${filename}`)
  if (!res.ok) throw new Error(`找不到样本：${filename}`)
  return res.json()
}

export async function parseUploadedJson(file) {
  const text = await file.text()
  const data = JSON.parse(text)
  if (!data.inputs && !data.gates && !data.outputs) {
    throw new Error('无效的保护逻辑配置：缺少 inputs/gates/outputs 字段')
  }
  return data
}
