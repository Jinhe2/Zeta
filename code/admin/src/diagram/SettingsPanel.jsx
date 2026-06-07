import {
  addSettingToConfig,
  deleteSettingFromConfig,
  updateSettingInConfig,
} from './configMutations'

const DATA_TYPES = ['FLOAT', 'INT', 'BOOL', 'STRING']

export default function SettingsPanel({ config, onConfigChange }) {
  const settings = config.settings ?? []

  const apply = (next) => onConfigChange(next)

  return (
    <div className="logic-visual-editor__panel logic-visual-editor__panel--settings">
      <div className="logic-visual-editor__panel-head">
        <h3>定值 settings</h3>
        <button
          type="button"
          className="users-page__link"
          onClick={() => apply(addSettingToConfig(config))}
        >
          + 新增
        </button>
      </div>

      {settings.length === 0 ? (
        <p className="logic-visual-editor__hint">暂无定值，输入元件可引用定值 ID。</p>
      ) : (
        <div className="logic-visual-editor__settings-list">
          {settings.map((setting) => (
            <div key={setting.id} className="logic-visual-editor__setting-card">
              <div className="logic-visual-editor__setting-head">
                <span className="logic-visual-editor__node-id">{setting.id}</span>
                <button
                  type="button"
                  className="users-page__link users-page__link--danger"
                  onClick={() => {
                    if (window.confirm(`确定删除定值「${setting.id}」？`)) {
                      apply(deleteSettingFromConfig(config, setting.id))
                    }
                  }}
                >
                  删除
                </button>
              </div>
              <label>
                名称
                <input
                  value={setting.name ?? ''}
                  onChange={(e) =>
                    apply(updateSettingInConfig(config, setting.id, { name: e.target.value }))
                  }
                />
              </label>
              <label>
                数据类型
                <select
                  value={setting.dataType ?? 'FLOAT'}
                  onChange={(e) =>
                    apply(updateSettingInConfig(config, setting.id, { dataType: e.target.value }))
                  }
                >
                  {DATA_TYPES.map((t) => (
                    <option key={t} value={t}>
                      {t}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                默认值
                <input
                  value={setting.defaultValue ?? ''}
                  onChange={(e) => {
                    const raw = e.target.value
                    let defaultValue = raw
                    if (setting.dataType === 'FLOAT') {
                      defaultValue = raw === '' ? 0 : Number(raw)
                    } else if (setting.dataType === 'INT') {
                      defaultValue = raw === '' ? 0 : parseInt(raw, 10)
                    } else if (setting.dataType === 'BOOL') {
                      defaultValue = raw === 'true' || raw === '1'
                    }
                    apply(updateSettingInConfig(config, setting.id, { defaultValue }))
                  }}
                />
              </label>
              <label>
                单位
                <input
                  value={setting.unit ?? ''}
                  onChange={(e) =>
                    apply(updateSettingInConfig(config, setting.id, { unit: e.target.value }))
                  }
                />
              </label>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
