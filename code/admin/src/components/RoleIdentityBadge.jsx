import './AdminLayout.css'

export default function RoleIdentityBadge({ displayName, roleLabel }) {
  return (
    <div className="admin-layout__header-meta role-identity">
      <span className="role-identity__role" aria-hidden="true">
        {roleLabel}
      </span>
      <span className="role-identity__name">{displayName || roleLabel || '用户'}</span>
    </div>
  )
}
