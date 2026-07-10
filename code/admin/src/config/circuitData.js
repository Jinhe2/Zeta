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
    ],
  },
]
