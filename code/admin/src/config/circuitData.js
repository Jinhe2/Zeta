import { publicUrl } from '../api/client'

/** 回路数据配置（未来可从 API 获取） */
export const CIRCUIT_DATA = [
  {
    category: '110kV线路保护测控屏',
    circuits: [
      {
        name: '110kV线路保护测控屏回路动态展示',
        file: publicUrl('kegong/circuit-dynamic-20260719/index.html'),
        description: '110kV线路保护测控屏多图纸回路动态教学',
      },
    ],
  },
]
