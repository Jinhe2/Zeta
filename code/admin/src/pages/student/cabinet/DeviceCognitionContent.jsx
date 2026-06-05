const DEVICE_PARAGRAPHS = [
  '屏柜内核心设备为继电保护测控装置，通常面板安装，正面包含液晶显示区、运行/动作/告警指示灯及本地操作按键。',
  '通过液晶界面可查看模拟量、开关量状态、定值区信息及装置自检结果；指示灯用于快速判断装置运行与故障情况。',
  '装置背面或侧方设有交流电压、电流、开入开出等接线端子，通过屏柜内电缆与端子排、断路器辅助接点相连。',
  '学习设备认知时，应能识别装置型号、额定参数、通信接口位置，并理解装置在屏柜中的安装层级与检修空间要求。',
]

export default function DeviceCognitionContent() {
  return (
    <div className="cabinet-section cabinet-section--device">
      <div className="cabinet-section__media cabinet-section__media--cabinet">
        <img
          className="cabinet-section__image"
          src="/images/cabinet-structure.svg"
          alt="继电保护屏柜示意图"
        />
      </div>

      <div className="cabinet-section__media cabinet-section__media--device">
        <img
          className="cabinet-section__image cabinet-section__image--device"
          src="/images/protection-device.svg"
          alt="保护测控装置面板示意图"
        />
      </div>

      <div className="cabinet-section__text cabinet-section__text--device">
        <h2 className="cabinet-section__title">设备认知</h2>
        {DEVICE_PARAGRAPHS.map((paragraph) => (
          <p key={paragraph} className="cabinet-section__paragraph">
            {paragraph}
          </p>
        ))}
      </div>
    </div>
  )
}
