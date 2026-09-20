<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import {
  getGarden,
  getGardenPlants,
  getGardenAreas,
  addPlantToGarden,
  updatePlantPosition,
  setPlantState,
  removePlantFromGarden,
} from '../../services/gardenService.js'
import { getAvailablePlants } from '../../services/plantService.js'
import { useKonvaZoomPan } from '../../composables/useKonvaZoomPan.js'
import { usePlantImage } from '../../composables/usePlantImage.js'
import { useGardenBackground } from '../../composables/useGardenBackground.js'
import { useAreaShapes } from '../../composables/useAreaShapes.js'

const DEFAULT_PLANT_RADIUS = 30

const route = useRoute()
const gardenId = Number(route.params.id)

const garden = ref(null)
const plants = ref([]) // GardenPlantDTO[]
const areas = ref([]) // AreaDTO[]
const ownedPlants = ref([]) // PlantDTO[], toutes les plantes du compte (placées ou non)
const loading = ref(true)
const error = ref(null)

const { stageConfig, stagePos, scale, onWheel, onStageDragMove } = useKonvaZoomPan({ width: 560, height: 560 })
const { backgroundConfig, areaFillConfig } = useGardenBackground({ stagePos, scale, stageConfig })

const plantsById = computed(() => new Map(ownedPlants.value.map((p) => [p.id, p])))
const plantsByGardenPlantId = computed(() => new Map(plants.value.map((p) => [p.id, p])))

const selectedGardenPlantId = ref(null)
const selectedPlant = computed(
  () => plants.value.find((p) => p.id === selectedGardenPlantId.value) ?? null,
)

const areaShapes = useAreaShapes(areas, areaFillConfig)

const associationLines = computed(() =>
  (garden.value?.associationLinks ?? [])
    .map((link) => {
      const from = plantsByGardenPlantId.value.get(link.plantId1)
      const to = plantsByGardenPlantId.value.get(link.plantId2)
      if (!from || !to) {
        return null
      }
      return {
        key: `${link.plantId1}-${link.plantId2}`,
        config: {
          points: [from.x, from.y, to.x, to.y],
          stroke: link.positive ? '#3de05b' : '#c0392b',
          strokeWidth: 3,
          dash: [6, 4],
          listening: false,
          perfectDrawEnabled: false,
        },
      }
    })
    .filter((line) => line !== null),
)

function plantRadius(plantId) {
  return plantsById.value.get(plantId)?.radius ?? DEFAULT_PLANT_RADIUS
}

function plantImage(plantId) {
  const species = plantsById.value.get(plantId)?.species
  return usePlantImage(species).value
}

const STATE_STROKE_COLORS = {
  A_PLANTER: '#3db2e0',
  A_RECOLTER: '#d8e03d',
}

function plantStrokeColor(state) {
  return STATE_STROKE_COLORS[state]
}

function circleClip(ctx, radius) {
  ctx.arc(0, 0, radius, 0, Math.PI * 2, false)
}

// Un clipFunc par rayon (et non par plante et par rendu) : sa reference reste stable d'un rendu a l'autre.
const clipFuncsByRadius = new Map()

function clipFuncForRadius(radius) {
  if (!clipFuncsByRadius.has(radius)) {
    clipFuncsByRadius.set(radius, (ctx) => circleClip(ctx, radius))
  }
  return clipFuncsByRadius.get(radius)
}

// Configs Konva de chaque plante, recalculees uniquement quand les plantes, la selection ou les images changent,
// pas a chaque pan/zoom. Le cercle est le seul noeud qui ecoute la souris (zone de clic/drag du groupe) :
// l'image et son clip, de meme rayon, sont exclus du hit graph.
const plantNodes = computed(() =>
  plants.value.map((plant) => {
    const radius = plantRadius(plant.plantId)
    return {
      id: plant.id,
      group: { x: plant.x, y: plant.y, draggable: plant.state === 'A_PLANTER' },
      circle: {
        radius,
        stroke: plantStrokeColor(plant.state),
        strokeWidth: 2,
        fill: plant.id === selectedGardenPlantId.value ? '#2c8a3d' : undefined,
        perfectDrawEnabled: false,
      },
      clip: { clipFunc: clipFuncForRadius(radius), listening: false },
      image: {
        image: plantImage(plant.plantId),
        width: 2 * radius,
        height: 2 * radius,
        offsetX: radius,
        offsetY: radius,
        opacity: plant.state === 'RECOLTEE' ? 0.5 : 1,
        listening: false,
        perfectDrawEnabled: false,
      },
      onDragEnd: (konvaEvent) => onPlantDragEnd(plant, konvaEvent),
      onSelect: () => selectPlant(plant),
    }
  }),
)

const STATE_ORDER = ['A_PLANTER', 'PLANTEE', 'A_RECOLTER', 'RECOLTEE']

function nextState(state) {
  const index = STATE_ORDER.indexOf(state)
  return index >= 0 && index < STATE_ORDER.length - 1 ? STATE_ORDER[index + 1] : null
}

async function loadAll() {
  loading.value = true
  error.value = null
  try {
    const [gardenResult, plantsResult, areasResult, ownedResult] = await Promise.all([
      getGarden(gardenId),
      getGardenPlants(gardenId),
      getGardenAreas(gardenId),
      getAvailablePlants(),
    ])
    garden.value = gardenResult
    plants.value = plantsResult
    areas.value = areasResult
    ownedPlants.value = ownedResult
  } catch {
    error.value = 'Impossible de charger ce potager.'
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)

// --- Placement d'une plante (drag depuis la palette, drop sur le canvas) ---

function onPaletteDragStart(event, plant) {
  event.dataTransfer.setData('text/plain', String(plant.id))
}

async function onCanvasDrop(event) {
  const plantId = Number(event.dataTransfer.getData('text/plain'))
  if (!plantId) {
    return
  }

  const rect = event.currentTarget.getBoundingClientRect()
  const x = Math.round((event.clientX - rect.left - stagePos.value.x) / scale.value)
  const y = Math.round((event.clientY - rect.top - stagePos.value.y) / scale.value)

  try {
    const created = await addPlantToGarden(gardenId, { plantId, x, y })
    plants.value = [...plants.value, created]
    // addPlantToGarden ne renvoie que la plante créée (pas de score à jour) : on recharge le potager à part.
    garden.value = await getGarden(gardenId)
  } catch {
    error.value = 'Impossible de placer cette plante.'
  }
}

// --- Déplacement d'une plante déjà placée ---

async function onPlantDragEnd(plant, konvaEvent) {
  const x = Math.round(konvaEvent.target.x())
  const y = Math.round(konvaEvent.target.y())
  plant.x = x
  plant.y = y
  try {
    garden.value = await updatePlantPosition(gardenId, plant.id, { x, y })
  } catch {
    error.value = 'Impossible de déplacer cette plante.'
  }
}

// --- Sélection / état / retrait d'une plante ---

function selectPlant(plant) {
  selectedGardenPlantId.value = plant.id
}

async function changeState(newState) {
  if (!selectedPlant.value || !newState) {
    return
  }
  try {
    const gardenPlantId = selectedPlant.value.id
    garden.value = await setPlantState(gardenId, gardenPlantId, newState)
    if (newState === 'RECOLTEE') {
      // Une plante récoltée est archivée côté serveur : elle disparaît du potager actif.
      plants.value = plants.value.filter((p) => p.id !== gardenPlantId)
      selectedGardenPlantId.value = null
    } else {
      selectedPlant.value.state = newState
    }
  } catch {
    error.value = "Impossible de changer l'état de cette plante."
  }
}

async function removeSelectedPlant() {
  if (!selectedPlant.value) {
    return
  }
  if (!window.confirm('Retirer cette plante du potager ?')) {
    return
  }
  try {
    garden.value = await removePlantFromGarden(gardenId, selectedPlant.value.id)
    plants.value = plants.value.filter((p) => p.id !== selectedPlant.value.id)
    selectedGardenPlantId.value = null
  } catch {
    error.value = 'Impossible de retirer cette plante.'
  }
}

// --- Zones (affichage en lecture seule, gérées depuis GardenStructure.vue) ---
</script>

<template>
  <div class="page garden-detail">
    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="loading">Chargement…</p>

    <template v-else-if="garden">
      <div class="garden-header">
        <div>
          <h1>{{ garden.name }}</h1>
          <p v-if="garden.score !== null && garden.score !== undefined" class="garden-score">
            Score d'association : {{ garden.score.toFixed(1) }}/10
          </p>
          <p v-else class="garden-score hint">Score d'association : aucune association détectée</p>
        </div>
        <nav class="garden-nav">
          <RouterLink :to="{ name: 'garden-structure', params: { id: gardenId } }">
            Structure du potager
          </RouterLink>
          <RouterLink :to="{ name: 'garden-list' }">
            Mes potagers
          </RouterLink>
          <RouterLink :to="{ name: 'plant-list' }">
            Mes plantes
          </RouterLink>
        </nav>
      </div>

      <div class="garden-layout">
        <aside class="garden-sidebar">
          <section>
            <h2>Mes plantes disponibles</h2>
            <p v-if="ownedPlants.length === 0" class="hint">
              Vous n'avez pas encore de plante. Ajoutez-en depuis votre compte.
            </p>
            <ul v-else class="list-reset palette">
              <li
                v-for="plant in ownedPlants"
                :key="plant.id"
                class="list-card"
                draggable="true"
                @dragstart="onPaletteDragStart($event, plant)"
              >
                {{ plant.variety }}
              </li>
            </ul>
            <p class="hint">Glisser une plante sur le potager pour la placer.</p>
          </section>

          <section v-if="selectedPlant" class="plant-panel">
            <h2>{{ plantsById.get(selectedPlant.plantId)?.variety ?? 'Plante' }}</h2>
            <label class="field">
              État
              <select
                :value="selectedPlant.state"
                @change="changeState($event.target.value)"
              >
                <option :value="selectedPlant.state">{{ selectedPlant.state }}</option>
                <option v-if="nextState(selectedPlant.state)" :value="nextState(selectedPlant.state)">
                  {{ nextState(selectedPlant.state) }}
                </option>
              </select>
            </label>
            <button type="button" class="btn" @click="removeSelectedPlant">Retirer du potager</button>
          </section>
        </aside>

        <div class="canvas-wrapper" @dragover.prevent @drop="onCanvasDrop">
          <v-stage :config="stageConfig" @wheel="onWheel" @dragmove="onStageDragMove">
            <v-layer>
              <v-rect :config="backgroundConfig" />

              <template v-for="shape in areaShapes" :key="shape.id">
                <v-line :config="shape.fill" />
                <v-text v-for="label in shape.labels" :key="label.key" :config="label.config" />
              </template>

              <v-line v-for="line in associationLines" :key="line.key" :config="line.config" />

              <v-group
                v-for="node in plantNodes"
                :key="node.id"
                :config="node.group"
                @dragend="node.onDragEnd"
                @click="node.onSelect"
                @tap="node.onSelect"
              >
                <v-circle :config="node.circle" />
                <v-group :config="node.clip">
                  <v-image :config="node.image" />
                </v-group>
              </v-group>
            </v-layer>
          </v-stage>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.palette {
  margin-bottom: 8px;
  gap: 4px;
}

.palette li {
  cursor: grab;
}

.hint {
  font-size: 13px;
  color: var(--text);
}

.garden-score {
  margin: 4px 0 0;
  font-size: 14px;
}

.plant-panel {
  padding: 12px;
  border-radius: 6px;
  border: 1px solid var(--accent-border);
  background: var(--accent-bg);
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
