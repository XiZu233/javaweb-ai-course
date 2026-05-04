import Vue from 'vue'
import VueRouter from 'vue-router'
import Login from '../views/login/index.vue'
import Layout from '../views/layout/index.vue'

Vue.use(VueRouter)

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dept',
    children: [
      {
        path: 'dept',
        name: 'Dept',
        component: () => import('../views/dept/index.vue'),
        meta: { title: '部门管理' }
      },
      {
        path: 'emp',
        name: 'Emp',
        component: () => import('../views/emp/index.vue'),
        meta: { title: '员工管理' }
      }
    ]
  }
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
