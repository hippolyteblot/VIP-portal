import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('@/views/DashboardView.vue'),
      meta: { requiresAuth: true, title: 'Dashboard' },
    },
    {
      path: '/applications',
      name: 'applications',
      component: () => import('@/views/ApplicationsView.vue'),
      meta: { requiresAuth: true, title: 'Applications' },
    },
    {
      path: '/applications/:name',
      name: 'application-detail',
      component: () => import('@/views/ApplicationDetailView.vue'),
      meta: { requiresAuth: true, title: 'Application details' },
    },
    {
      path: '/applications/:name/launch/:version?',
      name: 'application-launch',
      component: () => import('@/views/ApplicationLaunchView.vue'),
      meta: { requiresAuth: true, title: 'Launch Application' },
    },
    {
      path: '/applications/create',
      name: 'application-create',
      component: () => import('@/views/CreateApplicationView.vue'),
      meta: { requiresAuth: true, title: 'Create Application' },
    },
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/LandingView.vue'),
      meta: { title: 'Landing' },
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (!auth.initialized) {
    await auth.initialize()
  }

  if (to.name === 'home') {
    return auth.isAuthenticated ? { name: 'dashboard' } : undefined
  }

  if (to.name === 'landing') {
    return auth.isAuthenticated ? { name: 'dashboard' } : undefined
  }

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'login' }
  }

  if (
    !to.meta.requiresAuth &&
    auth.isAuthenticated &&
    ['login', 'register'].includes(to.name as string)
  ) {
    return { name: 'dashboard' }
  }
})

export default router
