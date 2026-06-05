import RoleLayout from '../components/RoleLayout'
import './PlaceholderPage.css'

export default function AdminDashboardPage() {
  return (
    <RoleLayout eyebrow="管理员" title="系统管理">
      <div className="placeholder-page">
        <h2>管理员界面</h2>
        <p>该模块尚未实现，后续将提供用户管理、保护逻辑配置与系统设置等功能。</p>
      </div>
    </RoleLayout>
  )
}
