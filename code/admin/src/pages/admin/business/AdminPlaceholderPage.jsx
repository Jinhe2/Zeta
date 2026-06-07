import './AdminPlaceholderPage.css'

export default function AdminPlaceholderPage({ title, description }) {
  return (
    <div className="admin-placeholder">
      <h2 className="admin-placeholder__title">{title}</h2>
      <p className="admin-placeholder__desc">{description}</p>
    </div>
  )
}
