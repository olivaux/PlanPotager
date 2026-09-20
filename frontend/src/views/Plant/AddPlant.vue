<script setup>
import { ref, computed, onMounted } from 'vue'
import { getAllSpecies, getVarietiesBySpecies } from '../../services/registryService.js'
import { addPlant } from '../../services/plantService.js'
import { resolveSpeciesImageUrl } from '../../composables/usePlantImage.js'


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

onMounted(async () => {
  try {
    species.value = await getAllSpecies()
  } catch {
    speciesError.value = 'Impossible de charger la liste des espèces.'
  }
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
  } catch {
    submitError.value = "Impossible d'ajouter cette plante."
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page page-narrow">
    <h1>Ajouter une plante</h1>

    <nav class="garden-nav">
          <RouterLink :to="{ name: 'plant-list' }">
            Mes plantes
          </RouterLink>
    </nav>

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
  </div>
</template>

<style scoped>
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

button {
  align-self: flex-start;
}
</style>
