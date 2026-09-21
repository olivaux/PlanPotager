<script setup>
import { ref, computed, onMounted } from 'vue'
import { getAvailablePlants, addPlant, removePlant } from '../../services/plantService.js'
import { getAllSpecies, getVarietiesBySpecies } from '../../services/registryService.js'
import { resolveSpeciesImageUrl } from '../../composables/usePlantImage.js'

const plants = ref([])
const error = ref(null)

const species = ref([])
const speciesError = ref(null)

const selectedSpecies = ref('')
const speciesImageUrl = computed(() => resolveSpeciesImageUrl(selectedSpecies.value))
const varieties = ref([])
const varietiesError = ref(null)
const loadingVarieties = ref(false)

const selectedVariety = ref('')
const comment = ref('')

const submitting = ref(false)
const submitError = ref(null)
const submitted = ref(false)

const canSubmit = computed(() => selectedVariety.value !== '' && !submitting.value)

async function loadPlants() {
  try {
    plants.value = await getAvailablePlants()
  } catch {
    error.value = 'Impossible de charger vos plantes.'
  }
}

async function loadSpecies() {
  try {
    species.value = await getAllSpecies()
  } catch {
    speciesError.value = 'Impossible de charger la liste des espèces.'
  }
}

onMounted(() => {
  loadPlants()
  loadSpecies()
})

async function onSpeciesChange() {
  selectedVariety.value = ''
  varieties.value = []
  varietiesError.value = null

  if (!selectedSpecies.value) {
    return
  }

  loadingVarieties.value = true
  try {
    varieties.value = await getVarietiesBySpecies(selectedSpecies.value)
  } catch {
    varietiesError.value = 'Impossible de charger les variétés de cette espèce.'
  } finally {
    loadingVarieties.value = false
  }
}

async function submit() {
  if (!canSubmit.value) {
    return
  }

  submitting.value = true
  submitError.value = null
  submitted.value = false

  try {
    await addPlant({ variety: selectedVariety.value, comment: comment.value })
    submitted.value = true
    selectedVariety.value = ''
    comment.value = ''
    await loadPlants()
  } catch {
    submitError.value = "Impossible d'ajouter cette plante."
  } finally {
    submitting.value = false
  }
}

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

    <section class="add-plant">
      <h2>Ajouter une plante</h2>

      <img :src="speciesImageUrl" :alt="selectedSpecies" class="species-preview" />

      <p v-if="submitted" class="success">Plante ajoutée à votre compte.</p>
      <p v-if="submitError" class="error">{{ submitError }}</p>
      <p v-if="speciesError" class="error">{{ speciesError }}</p>

      <form @submit.prevent="submit">
        <label class="field">
          Espèce
          <select v-model="selectedSpecies" @change="onSpeciesChange">
            <option value="" disabled>— Choisir une espèce —</option>
            <option v-for="s in species" :key="s.name" :value="s.name">{{ s.name }}</option>
          </select>
        </label>

        <label class="field">
          Variété
          <select v-model="selectedVariety" :disabled="!selectedSpecies || loadingVarieties">
            <option value="" disabled>
              {{ loadingVarieties ? 'Chargement…' : '— Choisir une variété —' }}
            </option>
            <option v-for="v in varieties" :key="v.name" :value="v.name">{{ v.name }}</option>
          </select>
        </label>
        <p v-if="varietiesError" class="error">{{ varietiesError }}</p>

        <label class="field">
          Remarque
          <input v-model="comment" type="text" />
        </label>

        <button type="submit" class="btn btn-primary" :disabled="!canSubmit">Ajouter</button>
      </form>
    </section>

    <h2>Plantes disponibles</h2>

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

.add-plant {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 32px;
}

.species-preview {
  width: 120px;
  height: 120px;
  object-fit: contain;
}

form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

form button {
  align-self: flex-start;
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
