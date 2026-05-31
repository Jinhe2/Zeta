export default function VersionBanner({ engine }) {
  return (
    <div className={`version-banner version-banner--${engine.id}`} role="status">
      <span className="version-banner__badge">{engine.label}</span>
      <span className="version-banner__desc">{engine.description}</span>
    </div>
  )
}
