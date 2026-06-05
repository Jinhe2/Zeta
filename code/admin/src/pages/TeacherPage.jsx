import RoleLayout from '../components/RoleLayout'
import './PlaceholderPage.css'

export default function TeacherPage() {
  return (
    <RoleLayout eyebrow="教师" title="教师工作台">
      <div className="placeholder-page">
        <h2>教师界面</h2>
        <p>该模块尚未实现，后续将提供课程编排、学员进度与逻辑框图批改等功能。</p>
      </div>
    </RoleLayout>
  )
}
