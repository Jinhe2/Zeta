import { useCallback, useEffect, useState } from 'react'
import { Navigate, useParams } from 'react-router-dom'
import { api } from '../../../api/client'
import { useAuth } from '../../../auth/AuthContext'
import './UsersPage.css'

const ROLE_KEY_MAP = {
  students: 'STUDENT',
  teachers: 'TEACHER',
  admins: 'ADMIN',
}

const ROLE_CONFIG = {
  STUDENT: { title: '学员管理', createLabel: '新建学员' },
  TEACHER: { title: '教师管理', createLabel: '新建教师' },
  ADMIN: { title: '管理员管理', createLabel: '新建管理员' },
}

function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function parseCsvLine(line) {
  const values = []
  let value = ''
  let quoted = false
  for (let index = 0; index < line.length; index += 1) {
    const char = line[index]
    if (char === '"') {
      if (quoted && line[index + 1] === '"') {
        value += '"'
        index += 1
      } else {
        quoted = !quoted
      }
    } else if (char === ',' && !quoted) {
      values.push(value.trim())
      value = ''
    } else {
      value += char
    }
  }
  if (quoted) throw new Error('CSV 存在未闭合的引号')
  values.push(value.trim())
  return values
}

function parseStudentCsv(text) {
  const lines = text.replace(/^\uFEFF/, '').split(/\r?\n/).filter((line) => line.trim())
  if (lines.length < 2) throw new Error('CSV 中没有可导入的学员数据')

  const aliases = {
    username: ['username', '用户名'],
    displayName: ['displayname', 'display_name', '显示名称', '姓名'],
    password: ['password', '密码'],
  }
  const headers = parseCsvLine(lines[0]).map((header) => header.toLowerCase())
  const indexes = Object.fromEntries(Object.entries(aliases).map(([field, names]) => [
    field,
    headers.findIndex((header) => names.includes(header)),
  ]))
  if (Object.values(indexes).some((index) => index < 0)) {
    throw new Error('CSV 表头必须包含 username、displayName、password')
  }

  const students = lines.slice(1).map((line) => {
    const values = parseCsvLine(line)
    return {
      username: values[indexes.username] || '',
      displayName: values[indexes.displayName] || '',
      password: values[indexes.password] || '',
    }
  })
  if (students.length > 500) throw new Error('单次最多导入 500 名学员')
  return students
}

export default function UsersPage() {
  const { roleKey } = useParams()
  const role = ROLE_KEY_MAP[roleKey]
  const { session } = useAuth()
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const [showCreate, setShowCreate] = useState(false)
  const [createForm, setCreateForm] = useState({ username: '', password: '', displayName: '' })
  const [creating, setCreating] = useState(false)
  const [showImport, setShowImport] = useState(false)
  const [importFileName, setImportFileName] = useState('')
  const [importRows, setImportRows] = useState([])
  const [importResult, setImportResult] = useState(null)
  const [importing, setImporting] = useState(false)

  const [editingUser, setEditingUser] = useState(null)
  const [editDisplayName, setEditDisplayName] = useState('')
  const [saving, setSaving] = useState(false)

  const [resetUser, setResetUser] = useState(null)
  const [resetPassword, setResetPassword] = useState('')
  const [resetting, setResetting] = useState(false)

  const loadUsers = useCallback(async () => {
    if (!role) return
    setLoading(true)
    setError('')
    setUsers([])
    try {
      const data = await api.listUsers(role)
      setUsers(data.filter((user) => user.role === role))
    } catch (err) {
      setError(err.message || '加载用户列表失败')
      setUsers([])
    } finally {
      setLoading(false)
    }
  }, [role])

  useEffect(() => {
    if (!role) return undefined
    const timer = window.setTimeout(() => {
      setShowCreate(false)
      setShowImport(false)
      setEditingUser(null)
      setResetUser(null)
      setMessage('')
      loadUsers()
    }, 0)
    return () => window.clearTimeout(timer)
  }, [role, loadUsers])

  const handleImportFile = async (event) => {
    const file = event.target.files?.[0]
    setImportRows([])
    setImportResult(null)
    setImportFileName(file?.name || '')
    setError('')
    if (!file) return
    try {
      setImportRows(parseStudentCsv(await file.text()))
    } catch (err) {
      setError(err.message || 'CSV 解析失败')
    }
  }

  const handleBatchImport = async () => {
    if (!importRows.length) return
    setImporting(true)
    setError('')
    try {
      const result = await api.batchImportStudents(importRows)
      setImportResult(result)
      if (result.successCount > 0) await loadUsers()
    } catch (err) {
      setError(err.message || '批量导入失败')
    } finally {
      setImporting(false)
    }
  }

  const flash = (text) => {
    setMessage(text)
    setTimeout(() => setMessage(''), 3000)
  }

  const handleCreate = async (e) => {
    e.preventDefault()
    setCreating(true)
    setError('')
    try {
      await api.createUser({ ...createForm, role })
      setShowCreate(false)
      setCreateForm({ username: '', password: '', displayName: '' })
      flash('创建成功')
      await loadUsers()
    } catch (err) {
      setError(err.message || '创建失败')
    } finally {
      setCreating(false)
    }
  }

  const openEdit = (user) => {
    setEditingUser(user)
    setEditDisplayName(user.displayName)
  }

  const handleUpdate = async (e) => {
    e.preventDefault()
    if (!editingUser) return
    setSaving(true)
    setError('')
    try {
      await api.updateUser(editingUser.id, { displayName: editDisplayName, role })
      setEditingUser(null)
      flash('已保存')
      await loadUsers()
    } catch (err) {
      setError(err.message || '更新失败')
    } finally {
      setSaving(false)
    }
  }

  const handleResetPassword = async (e) => {
    e.preventDefault()
    if (!resetUser) return
    setResetting(true)
    setError('')
    try {
      await api.resetUserPassword(resetUser.id, resetPassword)
      setResetUser(null)
      setResetPassword('')
      flash(`已重置 ${resetUser.username} 的密码`)
    } catch (err) {
      setError(err.message || '重置密码失败')
    } finally {
      setResetting(false)
    }
  }

  const handleDelete = async (user) => {
    if (!window.confirm(`确定删除「${user.displayName}」（${user.username}）？`)) {
      return
    }
    setError('')
    try {
      await api.deleteUser(user.id)
      flash('已删除')
      await loadUsers()
    } catch (err) {
      setError(err.message || '删除失败')
    }
  }

  const isSelf = (user) => user.username === session?.username

  if (!role) {
    return <Navigate to="/admin/users/students" replace />
  }

  const config = ROLE_CONFIG[role]

  return (
    <div className="users-page">
      <div className="users-page__header">
        <div>
          <h2 className="users-page__title">{config.title}</h2>
          <p className="users-page__desc">仅管理{config.title.replace('管理', '')}账号，角色固定不可变更。</p>
        </div>
        <div className="users-page__header-actions">
          {role === 'STUDENT' && (
            <button
              type="button"
              className="users-page__btn"
              onClick={() => {
                setShowImport(true)
                setImportFileName('')
                setImportRows([])
                setImportResult(null)
                setError('')
              }}
            >
              批量导入
            </button>
          )}
          <button
            type="button"
            className="users-page__btn users-page__btn--primary"
            onClick={() => setShowCreate(true)}
          >
            {config.createLabel}
          </button>
        </div>
      </div>

      {message && <div className="users-page__message">{message}</div>}
      {error && <div className="users-page__error">{error}</div>}

      {loading ? (
        <p className="users-page__loading">加载中…</p>
      ) : users.length === 0 ? (
        <p className="users-page__empty">暂无用户，点击右上角新建。</p>
      ) : (
        <div className="users-page__table-wrap">
          <table className="users-page__table">
            <thead>
              <tr>
                <th>用户名</th>
                <th>显示名称</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id}>
                  <td>{user.username}</td>
                  <td>{user.displayName}</td>
                  <td>{formatDate(user.createdAt)}</td>
                  <td>
                    <div className="users-page__actions">
                      <button type="button" className="users-page__link" onClick={() => openEdit(user)}>
                        编辑
                      </button>
                      <button
                        type="button"
                        className="users-page__link"
                        onClick={() => {
                          setResetUser(user)
                          setResetPassword('')
                        }}
                      >
                        重置密码
                      </button>
                      <button
                        type="button"
                        className="users-page__link users-page__link--danger"
                        disabled={isSelf(user)}
                        title={isSelf(user) ? '不能删除当前登录账号' : undefined}
                        onClick={() => handleDelete(user)}
                      >
                        删除
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showCreate && (
        <div className="users-page__overlay">
          <form className="users-page__dialog" onSubmit={handleCreate}>
            <h3>{config.createLabel}</h3>
            <label>
              用户名
              <input
                value={createForm.username}
                onChange={(e) => setCreateForm({ ...createForm, username: e.target.value })}
                required
                autoFocus
              />
            </label>
            <label>
              显示名称
              <input
                value={createForm.displayName}
                onChange={(e) => setCreateForm({ ...createForm, displayName: e.target.value })}
                required
              />
            </label>
            <label>
              密码
              <input
                type="password"
                value={createForm.password}
                onChange={(e) => setCreateForm({ ...createForm, password: e.target.value })}
                required
                minLength={6}
              />
            </label>
            <div className="users-page__dialog-actions">
              <button type="button" className="users-page__btn" onClick={() => setShowCreate(false)}>
                取消
              </button>
              <button type="submit" className="users-page__btn users-page__btn--primary" disabled={creating}>
                {creating ? '创建中…' : '创建'}
              </button>
            </div>
          </form>
        </div>
      )}

      {showImport && role === 'STUDENT' && (
        <div className="users-page__overlay">
          <div className="users-page__dialog users-page__dialog--import" role="dialog" aria-modal="true">
            <h3>批量导入学员</h3>
            <p className="users-page__import-help">
              上传 CSV 文件，表头为 username、displayName、password。单次最多 500 名学员，角色固定为学员。
            </p>
            <a
              className="users-page__template-link"
              href={'data:text/csv;charset=utf-8,%EF%BB%BFusername%2CdisplayName%2Cpassword%0Astudent001%2C%E5%BC%A0%E4%B8%89%2C123456'}
              download="学员批量导入模板.csv"
            >
              下载 CSV 模板
            </a>
            <label className="users-page__file-label">
              选择 CSV 文件
              <input type="file" accept=".csv,text/csv" onChange={handleImportFile} />
            </label>
            {importFileName && (
              <p className="users-page__import-summary">
                {importFileName}：已读取 {importRows.length} 行
              </p>
            )}
            {importResult && (
              <div className="users-page__import-result">
                <strong>成功 {importResult.successCount} 行，失败 {importResult.failureCount} 行</strong>
                {importResult.failureCount > 0 && (
                  <ul>
                    {importResult.results.filter((item) => !item.success).map((item) => (
                      <li key={`${item.rowNumber}-${item.username}`}>
                        第 {item.rowNumber} 行（{item.username || '空用户名'}）：{item.message}
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            )}
            <div className="users-page__dialog-actions">
              <button type="button" className="users-page__btn" onClick={() => setShowImport(false)}>
                关闭
              </button>
              <button
                type="button"
                className="users-page__btn users-page__btn--primary"
                disabled={!importRows.length || importing || Boolean(importResult)}
                onClick={handleBatchImport}
              >
                {importing ? '导入中…' : `导入 ${importRows.length || ''} 名学员`}
              </button>
            </div>
          </div>
        </div>
      )}

      {editingUser && (
        <div className="users-page__overlay">
          <form className="users-page__dialog" onSubmit={handleUpdate}>
            <h3>编辑 — {editingUser.username}</h3>
            <label>
              显示名称
              <input
                value={editDisplayName}
                onChange={(e) => setEditDisplayName(e.target.value)}
                required
                autoFocus
              />
            </label>
            <div className="users-page__dialog-actions">
              <button type="button" className="users-page__btn" onClick={() => setEditingUser(null)}>
                取消
              </button>
              <button type="submit" className="users-page__btn users-page__btn--primary" disabled={saving}>
                {saving ? '保存中…' : '保存'}
              </button>
            </div>
          </form>
        </div>
      )}

      {resetUser && (
        <div className="users-page__overlay">
          <form className="users-page__dialog" onSubmit={handleResetPassword}>
            <h3>重置密码 — {resetUser.username}</h3>
            <label>
              新密码
              <input
                type="password"
                value={resetPassword}
                onChange={(e) => setResetPassword(e.target.value)}
                required
                minLength={6}
                autoFocus
              />
            </label>
            <div className="users-page__dialog-actions">
              <button type="button" className="users-page__btn" onClick={() => setResetUser(null)}>
                取消
              </button>
              <button type="submit" className="users-page__btn users-page__btn--primary" disabled={resetting}>
                {resetting ? '提交中…' : '确认重置'}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  )
}
