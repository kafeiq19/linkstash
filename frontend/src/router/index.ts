import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { guest: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { guest: true },
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/BookmarksView.vue'),
          meta: { mode: 'home', requiresAuth: true },
        },
        {
          path: 'favorites',
          name: 'favorites',
          component: () => import('@/views/BookmarksView.vue'),
          meta: { mode: 'favorites', requiresAuth: true },
        },
        {
          path: 'archived',
          name: 'archived',
          component: () => import('@/views/BookmarksView.vue'),
          meta: { mode: 'archived', requiresAuth: true },
        },
        {
          path: 'b/:id',
          name: 'bookmark-detail',
          component: () => import('@/views/DetailView.vue'),
          meta: { requiresAuth: true },
        },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!auth.ready) await auth.restore()

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guest && auth.isAuthenticated) {
    return { name: 'home' }
  }
  return true
})

export default router
