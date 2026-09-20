import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../composables/useAuthStore.js'
import AuthAction from '../views/Auth/AuthAction.vue'
import Profile from '../views/Profile/Profile.vue'
import EditProfile from '../views/Profile/EditProfile.vue'
import CatalogView from '../views/Registry/CatalogView.vue'
import AddPlant from '../views/Plant/AddPlant.vue'
import MyPlants from '../views/Plant/MyPlants.vue'
import GardenList from '../views/Garden/GardenList.vue'
import NewGarden from '../views/Garden/NewGarden.vue'
import GardenStructure from '../views/Garden/GardenStructure.vue'
import GardenDetail from '../views/Garden/GardenDetail.vue'

// Les routes des vues
const routes = [
  // meta.title : titre affiché dans l'entête (les vues de potager le complètent avec le nom du potager)
  { path: '/signup', name: 'signup', component: AuthAction, meta: { mode: 'signup', title: 'Créer un compte' } },
  { path: '/login', name: 'login', component: AuthAction, meta: { mode: 'login', title: 'Se connecter' } },
  { path: '/profile', name: 'profile', component: Profile, meta: { requiresAuth: true, title: 'Mon profil' } },
  { path: '/profile/edit', name: 'profile-edit', component: EditProfile, meta: { requiresAuth: true, title: 'Modifier le profil' } },
  { path: '/catalog', name: 'catalog', component: CatalogView, meta: { requiresAuth: true, title: 'Catalogue' } },
  { path: '/plant/add', name: 'plant-add', component: AddPlant, meta: { requiresAuth: true, title: 'Ajouter une plante' } },
  { path: '/plant/list', name: 'plant-list', component: MyPlants, meta: { requiresAuth: true, title: 'Mes plantes' } },
  { path: '/garden', name: 'garden-list', component: GardenList, meta: { requiresAuth: true, title: 'Liste des potagers' } },
  { path: '/garden/new', name: 'garden-new', component: NewGarden, meta: { requiresAuth: true, title: 'Nouveau potager' } },
  { path: '/garden/:id/structure', name: 'garden-structure', component: GardenStructure, meta: { requiresAuth: true, title: 'Structure du potager' } },
  { path: '/garden/:id/plants', name: 'garden-detail', component: GardenDetail, meta: { requiresAuth: true, title: 'Potager' } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  const { isAuthenticated, fetchCurrentUser } = useAuthStore()

  if (!isAuthenticated.value) {
    await fetchCurrentUser()
  }

  if (to.path === '/') {
    return { path: isAuthenticated.value ? '/profile' : '/login' }
  }

  const isAuthRoute = to.path === '/login' || to.path === '/signup'
  if (isAuthRoute && isAuthenticated.value) {
    return { path: '/profile' }
  }

  if (to.meta.requiresAuth && !isAuthenticated.value) {
    return { path: '/login' }
  }

  return true
})

export default router
