export type UserRole = 'student' | 'teacher' | 'admin'

export interface DemoAccount {
  username: string
  password: string
  role: UserRole
  label: string
  displayName: string
}

/** 演示用测试账号（密码均为 123456） */
export const DEMO_ACCOUNTS: DemoAccount[] = [
  {
    username: 'student',
    password: '123456',
    role: 'student',
    label: '学员',
    displayName: '学员测试账号',
  },
  {
    username: 'teacher',
    password: '123456',
    role: 'teacher',
    label: '教师',
    displayName: '教师测试账号',
  },
  {
    username: 'admin',
    password: '123456',
    role: 'admin',
    label: '管理员',
    displayName: '管理员测试账号',
  },
]

export function getHomePath(role: UserRole): string {
  switch (role) {
    case 'student':
      return '/student'
    case 'teacher':
      return '/teacher'
    case 'admin':
      return '/admin'
  }
}

export function findDemoAccount(username: string, password: string): DemoAccount | null {
  const normalized = username.trim().toLowerCase()
  return (
    DEMO_ACCOUNTS.find(
      (a) => a.username.toLowerCase() === normalized && a.password === password,
    ) ?? null
  )
}
