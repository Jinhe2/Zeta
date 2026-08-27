import { useState } from 'react'
import { imageUrl } from '../api/client'

function displayBaselineValue(value, valueType) {
  if (valueType !== 'FLOAT' || value === null || value === undefined || value === '') return value ?? '—'
  const number = Number(value)
  return Number.isFinite(number) ? number.toFixed(2) : value
}

/** 实验引导弹窗：按顺序依次展示管理员配置的引导条目。 */
export default function ExperimentGuideDialog({ items, title, onClose }) {
  const [index, setIndex] = useState(0)
  const list = items ?? []
  const current = list[index] ?? null

  return (
    <div className="experiment-guide-dialog" role="dialog" aria-modal="false" aria-labelledby="experiment-guide-title">
      <div
        className={`experiment-guide-dialog__panel${
          current && current.type === 'SETTING_LIST' ? ' experiment-guide-dialog__panel--setting-list' : ''
        }`}
      >
        <div className="experiment-guide-dialog__header">
          <div>
            <span className="experiment-guide-dialog__eyebrow">实验引导</span>
            <h2 id="experiment-guide-title">{title || '实验引导'}</h2>
          </div>
          {list.length > 0 && (
            <span className="experiment-guide-dialog__count">
              {index + 1} / {list.length}
            </span>
          )}
        </div>

        {list.length === 0 ? (
          <div className="experiment-guide-dialog__text">
            <p>暂未配置实验引导。</p>
          </div>
        ) : (
          <>
            {current.type === 'IMAGE_TEXT' && current.hasImage && (
              <div className="experiment-guide-dialog__image">
                <img src={imageUrl('experiment-guide', current.id)} alt={current.title} />
              </div>
            )}

            {current.type === 'SETTING_LIST' ? (
              <div className="experiment-guide-dialog__setting">
                <div className="experiment-guide-dialog__text">
                  <h3>{current.title}</h3>
                  {current.content && <p>{current.content}</p>}
                </div>
                <div className="experiment-guide-dialog__table-wrap">
                  <table className="experiment-guide-dialog__table">
                    <thead>
                      <tr>
                        <th>序号</th>
                        <th>定值名称</th>
                        <th>定值</th>
                      </tr>
                    </thead>
                    <tbody>
                      {(current.settingItems ?? []).length === 0 ? (
                        <tr>
                          <td colSpan={3} className="experiment-guide-dialog__empty">该层级暂无定值清单</td>
                        </tr>
                      ) : (
                        (current.settingItems ?? []).map((item, i) => (
                          <tr key={item.settingRef || i}>
                            <td>{i + 1}</td>
                            <td>{item.settingName}</td>
                            <td>{displayBaselineValue(item.baselineValue, item.valueType)}</td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            ) : (
              <div className="experiment-guide-dialog__text">
                <h3>{current.title}</h3>
                {current.content && <p>{current.content}</p>}
              </div>
            )}
          </>
        )}

        <div className="experiment-guide-dialog__actions">
          <button
            type="button"
            onClick={() => setIndex((cur) => Math.max(0, cur - 1))}
            disabled={index <= 0}
          >
            上一条
          </button>
          <button
            type="button"
            onClick={() => setIndex((cur) => Math.min(list.length - 1, cur + 1))}
            disabled={index >= list.length - 1}
          >
            下一条
          </button>
          <button type="button" onClick={onClose}>
            关闭
          </button>
        </div>
      </div>
    </div>
  )
}
