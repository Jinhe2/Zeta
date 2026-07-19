import { publicUrl } from '../api/client'

/** 回路数据配置（未来可从 API 获取） */
export const CIRCUIT_DATA = [
  {
    category: '操作回路',
    circuits: [
      {
        name: '操作回路',
        file: publicUrl('kegong/操作回路/操作回路.html'),
        description: '操作回路全节点流向可视化',
      },
      {
        name: '遥控回路',
        file: publicUrl('kegong/操作回路/遥控回路.html'),
        description: '遥控回路全节点流向可视化',
      },
      {
        name: '110kV线路保护测控屏回路动态展示',
        file: publicUrl('kegong/circuit-dynamic-20260719/index.html'),
        description: '110kV线路保护测控屏多图纸回路动态教学',
      },
    ],
  },
]
