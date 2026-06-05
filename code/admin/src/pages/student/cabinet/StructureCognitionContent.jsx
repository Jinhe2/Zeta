const STRUCTURE_PARAGRAPHS = [
  '继电保护屏柜通常采用金属封闭式结构，由柜体框架、前后柜门、安装横梁及底座组成，用于集中安装保护装置、自动装置和相关二次设备。',
  '屏柜正面分为操作观察区与设备检修区：观察窗便于查看装置运行状态与指示灯；门板内侧可安装压板、转换开关等人机操作元件。',
  '屏柜内部按功能分层布置，上层多为保护测量装置，中层为端子排与接线端子，下层常预留电缆进线及接地铜排空间。',
  '学习结构认知时，应熟悉门轴方向、门锁机构、通风散热孔及接地连接位置，为后续设备认知与回路学习打下基础。',
]

export default function StructureCognitionContent() {
  return (
    <div className="cabinet-section cabinet-section--structure">
      <div className="cabinet-section__media">
        <img
          className="cabinet-section__image"
          src="/images/cabinet-structure.svg"
          alt="继电保护屏柜结构示意图"
        />
      </div>
      <div className="cabinet-section__text">
        <h2 className="cabinet-section__title">结构认知</h2>
        {STRUCTURE_PARAGRAPHS.map((paragraph) => (
          <p key={paragraph} className="cabinet-section__paragraph">
            {paragraph}
          </p>
        ))}
      </div>
    </div>
  )
}
