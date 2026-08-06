import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'
import MovementView from '../views/MovementView.vue'
import ContractionView from '../views/ContractionView.vue'
import RecordsView from '../views/RecordsView.vue'
import InsightsView from '../views/InsightsView.vue'
import PregnancyView from '../views/PregnancyView.vue'
import TasksView from '../views/TasksView.vue'
import SettingsView from '../views/SettingsView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: DashboardView },
    { path: '/movement', component: MovementView },
    { path: '/contractions', component: ContractionView },
    { path: '/records', component: RecordsView },
    { path: '/insights', component: InsightsView },
    { path: '/pregnancy', component: PregnancyView },
    { path: '/tasks', component: TasksView },
    { path: '/settings', component: SettingsView },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

export default router
