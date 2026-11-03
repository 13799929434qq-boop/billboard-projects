import { defineStore } from 'pinia'
import type { UserInfo } from '../types'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('gb_token') || '',
    user: JSON.parse(localStorage.getItem('gb_user') || 'null') as UserInfo | null
  }),
  getters: {
    role: (s): string => s.user?.role || '',
    name: (s): string => s.user?.name || '',
    isLoggedIn: (s): boolean => !!s.token
  },
  actions: {
    setAuth(token: string, user: UserInfo) {
      this.token = token
      this.user = user
      localStorage.setItem('gb_token', token)
      localStorage.setItem('gb_user', JSON.stringify(user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('gb_token')
      localStorage.removeItem('gb_user')
    }
  }
})
