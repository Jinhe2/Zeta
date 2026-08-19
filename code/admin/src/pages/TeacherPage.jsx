import UsersPage from './admin/business/UsersPage'

export default function TeacherPage() {
  return <UsersPage fixedRole="STUDENT" invalidRedirect="/teacher" />
}
