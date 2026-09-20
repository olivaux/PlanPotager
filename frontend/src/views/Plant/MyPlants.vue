<script setup>
import { ref, onMounted } from 'vue'
import { getAvailablePlants, removePlant } from '../../services/plantService.js'
import { resolveSpeciesImageUrl } from '../../composables/usePlantImage.js'

const plants = ref([])
const error = ref(null)

async function loadPlants() {
  try {
    plants.value = await getAvailablePlants()
  } catch {
    error.value = 'Impossible de charger vos plantes.'
  }
}

onMounted(loadPlants)

async function confirmRemove(plantId) {
  if (!window.confirm('Supprimer cette plante de votre compte ?')) {
    return
  }
  try {
    await removePlant(plantId)
    await loadPlants()
  } catch {
    error.value = 'Impossible de supprimer cette plante.'
  }
}
</script>

<template>
  <div class="page page-medium">
    <h1>Mes plantes</h1>

    <nav class="garden-nav">
          <RouterLink :to="{ name: 'plant-add' }">
            Ajouter des plantes
          </RouterLink>
    </nav>
    
    <p v-if="error" class="error">{{ error }}</p>
    <p v-else-if="plants.length === 0">Aucune plante enregistrée pour l'instant.</p>
    <ul v-else class="list-reset">
      <li v-for="plant in plants" :key="plant.id" class="list-card row">
        <span class="plant-info">
          <img :src="resolveSpeciesImageUrl(plant.species)" :alt="plant.species" class="species-thumb" />
          <span>
            <strong>{{ plant.variety }}</strong>
            <span v-if="plant.comment" class="comment"> · {{ plant.comment }}</span>
          </span>
        </span>
        <button type="button" class="btn" @click="confirmRemove(plant.id)">Supprimer</button>
      </li>
    </ul>
  </div>
</template>

<style scoped>
ul {
  gap: 8px;
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.plant-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.species-thumb {
  width: 48px;
  height: 48px;
  object-fit: contain;
}

.comment {
  color: var(--text);
}
</style>
