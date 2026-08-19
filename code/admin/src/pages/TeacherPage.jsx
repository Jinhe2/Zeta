import RoleLayout from '../components/RoleLayout'
import UsersPage from './admin/business/UsersPage'
import './admin/business/UsersPage.css'

export default function TeacherPage() {
  return (
    <RoleLayout eyebrow="教师" title="教师工作台">
      <UsersPage fixedRole="STUDENT" invalidRedirect="/teacher" />
    </RoleLayout>
  )
}
