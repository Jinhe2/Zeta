import { useState } from 'react'

const PLATE_PARTS = [
  {
    id: 'label',
    name: '标签牌',
    description:
      '标注压板名称、编号及所属回路功能，是运行与检修人员识别压板用途的首要依据，可防止误投、误退操作。',
  },
  {
    id: 'bracket',
    name: '绝缘支架',
    description:
      '支撑并固定压板组件，采用绝缘材料与金属框架隔离，保证各导电部件对地及相间绝缘距离满足规程要求。',
  },
  {
    id: 'base',
    name: '压板座',
    description:
      '压板安装基座，内部设有静触头或插孔，与连接片配合形成可投退的电气接点，是压板结构的固定核心。',
  },
  {
    id: 'link',
    name: '连接片',
    description:
      '压板投退的执行部件。投入时连接片插入压板座使回路接通；退出时拔下连接片使回路断开，实现二次回路的隔离控制。',
  },
  {
    id: 'terminal',
    name: '接线端子',
    description:
      '压板两侧接线端子用于引入和引出电缆芯线，通过紧固螺钉压接导线，与屏柜内回路、端子排形成完整电气通路。',
  },
]

export default function PlateCognitionContent() {
  const [activePartId, setActivePartId] = useState(PLATE_PARTS[0].id)
  const activePart = PLATE_PARTS.find((part) => part.id === activePartId) ?? PLATE_PARTS[0]

  return (
    <div className="cabinet-section cabinet-section--plate">
      <div className="cabinet-section__media cabinet-section__media--cabinet">
        <img
          className="cabinet-section__image"
          src="/images/cabinet-structure.svg"
          alt="继电保护屏柜示意图"
        />
      </div>

      <div className="cabinet-section__media cabinet-section__media--plate">
        <p className="plate-assembly__hint">点击元件查看说明</p>
        <div className="plate-assembly" role="group" aria-label="压板结构">
          {PLATE_PARTS.map((part) => (
            <button
              key={part.id}
              type="button"
              className={`plate-part plate-part--${part.id}${activePartId === part.id ? ' plate-part--active' : ''}`}
              onClick={() => setActivePartId(part.id)}
              aria-pressed={activePartId === part.id}
              aria-label={part.name}
            >
              <span className="plate-part__label">{part.name}</span>
            </button>
          ))}
        </div>
      </div>

      <div className="cabinet-section__text cabinet-section__text--plate">
        <h2 className="cabinet-section__title">压板认知</h2>
        <h3 className="cabinet-section__subtitle">{activePart.name}</h3>
        <p className="cabinet-section__paragraph">{activePart.description}</p>
      </div>
    </div>
  )
}
