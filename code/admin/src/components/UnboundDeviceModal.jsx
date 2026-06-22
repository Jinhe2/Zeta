import { getDeviceBindId } from '../api/deviceBinding'
import './UnboundDeviceModal.css'

export default function UnboundDeviceModal({ onLogout }) {
  const bindId = getDeviceBindId()

  return (
    <div className="unbound-modal">
      <div className="unbound-modal__panel">
        <div className="unbound-modal__icon">⚠️</div>
        <h2 className="unbound-modal__title">设备未绑定</h2>
        <p className="unbound-modal__desc">
          当前设备尚未绑定屏柜，请联系管理员在后台完成绑定操作。
        </p>
        <div className="unbound-modal__info">
          <span className="unbound-modal__info-label">设备 ID</span>
          <code className="unbound-modal__info-value">{bindId}</code>
        </div>
        <p className="unbound-modal__hint">
          请将此设备 ID 提供给管理员进行绑定
        </p>
        <button type="button" className="unbound-modal__btn" onClick={onLogout}>
          退出登录
        </button>
      </div>
    </div>
  )
}
