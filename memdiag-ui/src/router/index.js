import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../views/Dashboard.vue'
import Histogram from '../views/Histogram.vue'
import Snapshots from '../views/Snapshots.vue'
import Diff from '../views/Diff.vue'
import Diagnosis from '../views/Diagnosis.vue'
import Threads from '../views/Threads.vue'
import Nmt from '../views/Nmt.vue'

const routes = [
  { path: '/', name: 'Dashboard', component: Dashboard },
  { path: '/histogram', name: 'Histogram', component: Histogram },
  { path: '/snapshots', name: 'Snapshots', component: Snapshots },
  { path: '/diff', name: 'Diff', component: Diff },
  { path: '/diagnose', name: 'Diagnosis', component: Diagnosis },
  { path: '/threads', name: 'Threads', component: Threads },
  { path: '/nmt', name: 'Nmt', component: Nmt }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
