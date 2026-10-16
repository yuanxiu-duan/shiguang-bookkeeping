import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/userStore'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { guest: true }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/ForgotPassword.vue'),
    meta: { guest: true }
  },
  {
    path: '/',
    component: () => import('@/views/Dashboard.vue'),
    meta: { requiresAuth: true },
    redirect: '/bills',
    children: [
      {
        path: 'bills',
        name: 'BillFlow',
        component: () => import('@/views/BillFlow.vue')
      },
      {
        path: 'accounting',
        name: 'Accounting',
        component: () => import('@/views/Accounting.vue')
      },
      {
        path: 'reports',
        name: 'Reports',
        component: () => import('@/views/Reports.vue')
      },
      {
        path: 'budget',
        name: 'BudgetManage',
        component: () => import('@/views/BudgetManage.vue')
      },
      {
        path: 'categories',
        name: 'CategoryManage',
        component: () => import('@/views/CategoryManage.vue')
      },
      {
        path: 'account-books',
        name: 'AccountBooks',
        component: () => import('@/views/AccountBookManage.vue')
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/Settings.vue')
      },
      {
        path: 'admin',
        name: 'AdminManage',
        component: () => import('@/views/AdminManage.vue'),
        meta: { requiresAdmin: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  if (to.meta.requiresAuth && !userStore.isLoggedIn()) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else if (to.meta.guest && userStore.isLoggedIn()) {
    next({ path: '/' })
  } else if (to.meta.requiresAdmin && userStore.userInfo?.role !== 'ADMIN') {
    next({ path: '/' })
  } else {
    next()
  }
})

// 防止 Vue Router 重定向/取消导航产生的未捕获 Promise 错误
const originalPush = router.push.bind(router)
router.push = function(to) {
  return originalPush(to).catch(err => {
    // 忽略 NavigationDuplicated 和取消导航错误
    if (err?.name !== 'NavigationDuplicated' && !err?.message?.includes('Redirected')) {
      return Promise.reject(err)
    }
  })
}

const originalReplace = router.replace.bind(router)
router.replace = function(to) {
  return originalReplace(to).catch(err => {
    if (err?.name !== 'NavigationDuplicated' && !err?.message?.includes('Redirected')) {
      return Promise.reject(err)
    }
  })
}

export default router
