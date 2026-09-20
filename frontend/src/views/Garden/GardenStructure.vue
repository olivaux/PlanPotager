<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import {
  getGarden,
  getGardenAreas,
  updateGarden,
  createArea,
  updateArea,
  deleteArea,
} from '../../services/gardenService.js'
import { useKonvaZoomPan } from '../../composables/useKonvaZoomPan.js'
import { useGardenBackground } from '../../composables/useGardenBackground.js'
import { useAreaShapes } from '../../composables/useAreaShapes.js'
import { CORNER_KEYS } from '../../utils/areaGeometry.js'

const route = useRoute()
const gardenId = Number(route.params.id)

const garden = ref(null)
const areas = ref([]) // AreaDTO[]
const loading = ref(true)
const error = ref(null)

const { stageConfig, stagePos, scale, onWheel, onStageDragMove } = useKonvaZoomPan({ width: 560, height: 560 })
const { backgroundConfig, areaFillConfig } = useGardenBackground({ stagePos, scale, stageConfig })
const areaShapes = useAreaShapes(areas, areaFillConfig)

// --- Infos potager (nom, coordonnées) ---

const name = ref('')
const longitude = ref('')
const latitude = ref('')
const savingInfo = ref(false)

async function loadAll() {
  loading.value = true
  error.value = null
  try {
    const [gardenResult, areasResult] = await Promise.all([
      getGarden(gardenId),
      getGardenAreas(gardenId),
    ])
    garden.value = gardenResult
    areas.value = areasResult
    name.value = gardenResult.name
    longitude.value = gardenResult.longitude ?? ''
    latitude.value = gardenResult.latitude ?? ''
  } catch {
    error.value = 'Impossible de charger ce potager.'
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)

async function saveInfo() {
  savingInfo.value = true
  error.value = null
  try {
    garden.value = await updateGarden(gardenId, {
      name: name.value,
      longitude: longitude.value === '' ? null : Number(longitude.value),
      latitude: latitude.value === '' ? null : Number(latitude.value),
    })
  } catch {
    error.value = 'Impossible de mettre à jour ce potager.'
  } finally {
    savingInfo.value = false
  }
}

// --- Zones (4 coins fixes, pas de sommet dynamique) ---

async function addNewArea() {
  const defaultArea = {
    id: null,
    leftUpX: 40, leftUpY: 40,
    rightUpX: 160, rightUpY: 40,
    rightDownX: 160, rightDownY: 160,
    leftDownX: 40, leftDownY: 160,
  }
  try {
    const created = await createArea(gardenId, defaultArea)
    areas.value = [...areas.value, created]
  } catch {
    error.value = 'Impossible de créer cette zone.'
  }
}

function onCornerDragMove(area, key, konvaEvent) {
  area[`${key}X`] = Math.round(konvaEvent.target.x())
  area[`${key}Y`] = Math.round(konvaEvent.target.y())
}

async function onCornerDragEnd(area) {
  try {
    await updateArea(gardenId, area.id, area)
  } catch {
    error.value = 'Impossible de déplacer ce sommet.'
  }
}

// Configs et handlers des poignees, calcules quand les zones changent et non a chaque rendu (pan/zoom).
const cornerShapes = computed(
  () =>
    new Map(
      areas.value.map((area) => [
        area.id,
        CORNER_KEYS.map((key) => ({
          key,
          config: {
            x: area[`${key}X`],
            y: area[`${key}Y`],
            radius: 6,
            fill: '#aa3bff',
            draggable: true,
            perfectDrawEnabled: false,
          },
          onDragMove: (konvaEvent) => onCornerDragMove(area, key, konvaEvent),
          onDragEnd: () => onCornerDragEnd(area),
        })),
      ]),
    ),
)

async function removeArea(area) {
  if (!window.confirm('Supprimer cette zone ?')) {
    return
  }
  try {
    await deleteArea(gardenId, area.id)
    areas.value = areas.value.filter((a) => a.id !== area.id)
  } catch {
    error.value = 'Impossible de supprimer cette zone.'
  }
}
</script>

<template>
  <div class="page garden-structure">
    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="loading">Chargement…</p>

    <template v-else-if="garden">
      <div class="garden-header">
        <h1>{{ garden.name }}</h1>
        <nav class="garden-nav">
          <RouterLink :to="{ name: 'garden-detail', params: { id: gardenId } }">
            Suivi des plantes
          </RouterLink>
          <RouterLink :to="{ name: 'garden-list' }">Mes potagers</RouterLink>
        </nav>
      </div>

      <div class="garden-layout">
        <aside class="garden-sidebar">
          <section>
            <h2>Informations</h2>
            <form class="info-form" @submit.prevent="saveInfo">
              <label class="field">
                Nom
                <input v-model="name" type="text" required />
              </label>
              <label class="field">
                Longitude
                <input v-model="longitude" type="number" step="any" />
              </label>
              <label class="field">
                Latitude
                <input v-model="latitude" type="number" step="any" />
              </label>
              <button type="submit" class="btn btn-primary" :disabled="savingInfo">Enregistrer</button>
            </form>
          </section>

          <section>
            <h2>Zones</h2>
            <ul v-if="areas.length > 0" class="list-reset areas">
              <li v-for="area in areas" :key="area.id" class="list-card row">
                Zone #{{ area.id }}
                <button type="button" class="btn" @click="removeArea(area)">Supprimer</button>
              </li>
            </ul>
            <button type="button" class="btn" @click="addNewArea">+ Nouvelle zone</button>
          </section>
        </aside>

        <div class="canvas-wrapper">
          <v-stage :config="stageConfig" @wheel="onWheel" @dragmove="onStageDragMove">
            <v-layer>
              <v-rect :config="backgroundConfig" />

              <template v-for="shape in areaShapes" :key="shape.id">
                <v-line :config="shape.fill" />
                <v-text v-for="label in shape.labels" :key="label.key" :config="label.config" />
                <v-circle
                  v-for="corner in cornerShapes.get(shape.id)"
                  :key="corner.key"
                  :config="corner.config"
                  @dragmove="corner.onDragMove"
                  @dragend="corner.onDragEnd"
                />
              </template>
            </v-layer>
          </v-stage>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.info-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-form .field {
  font-size: 13px;
}

.areas {
  margin-bottom: 8px;
  gap: 4px;
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>