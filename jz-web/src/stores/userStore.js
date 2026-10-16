import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login, register, getCurrentUser } from '@/api/login'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  const currentAccountBookId = ref(JSON.parse(localStorage.getItem('currentAccountBookId') || 'null'))

  function setCurrentAccountBookId(id) {
    currentAccountBookId.value = id
    localStorage.setItem('currentAccountBookId', JSON.stringify(id))
  }

  async function doLogin(username, password) {
    const result = await login({ username, password, loginType: 'PASSWORD' })
    token.value = result.token
    userInfo.value = result.user
    localStorage.setItem('token', result.token)
    localStorage.setItem('userInfo', JSON.stringify(result.user))
    return result
  }

  async function doLoginByEmail(email, password) {
    const result = await login({ email, password, loginType: 'PASSWORD' })
    token.value = result.token
    userInfo.value = result.user
    localStorage.setItem('token', result.token)
    localStorage.setItem('userInfo', JSON.stringify(result.user))
    return result
  }

  async function doLoginByCode(email, emailCode) {
    const result = await login({ email, emailCode, loginType: 'CODE' })
    token.value = result.token
    userInfo.value = result.user
    localStorage.setItem('token', result.token)
    localStorage.setItem('userInfo', JSON.stringify(result.user))
    return result
  }

  async function doRegister(username, password, email, emailCode) {
    const result = await register({ username, password, email, emailCode })
    return result
  }

  async function fetchCurrentUser() {
    try {
      const user = await getCurrentUser()
      userInfo.value = user
      localStorage.setItem('userInfo', JSON.stringify(user))
      return user
    } catch (error) {
      logout()
      throw error
    }
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    currentAccountBookId.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('currentAccountBookId')
  }

  function isLoggedIn() {
    return !!token.value
  }

  return {
    token,
    userInfo,
    currentAccountBookId,
    setCurrentAccountBookId,
    doLogin,
    doLoginByEmail,
    doLoginByCode,
    doRegister,
    fetchCurrentUser,
    logout,
    isLoggedIn
  }
})