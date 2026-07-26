// 路由配置 + 登录守卫
import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    name: 'projects',
    component: () => import('@/views/ProjectListView.vue'),
    meta: { title: '我的项目' }
  },
  {
    path: '/studio/:projectId',
    name: 'studio',
    component: () => import('@/views/StudioView.vue'),
    meta: { title: '创作工作台' }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：未登录跳 /login
router.beforeEach((to) => {
  const userStore = useUserStore()
  if (to.meta.public) {
    // 已登录访问 /login 自动回首页
    if (to.name === 'login' && userStore.isLogin()) {
      return { path: '/' }
    }
    return true
  }
  if (!userStore.isLogin()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

router.afterEach((to) => {
  const title = (to.meta.title as string) || ''
  document.title = title ? `${title} - AI 漫剧制作平台` : 'AI 漫剧制作平台'
})

export default router
