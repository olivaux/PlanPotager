<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import {
  getGarden,
  getGardenPlants,
  getGardenAreas,
  addPlantToGarden,
  updatePlantPosition,
  updatePlantMatrix,
  setPlantState,
  removePlantFromGarden,
} from '../../services/gardenService.js'
import { getAvailablePlants } from '../../services/plantService.js'
import { useKonvaZoomPan } from '../../composables/useKonvaZoomPan.js'
import { usePlantImage, resolveSpeciesImageUrl } from '../../composables/usePlantImage.js'
import { useGardenBackground } from '../../composables/useGardenBackground.js'
import { useAreaShapes } from '../../composables/useAreaShapes.js'
import { usePageTitle } from '../../composables/usePageTitle.js'
import StarRating from '../../components/StarRating.vue'
import {
  ASSOCIATION_COLORS,
  associationLineWidth,
  drawAssociationLine,
} from '../../utils/associationLine.js'
import editIcon from '../../assets/edit.png'
import leftIcon from '../../assets/left.png'

const DEFAULT_PLANT_RADIUS = 30

const route = useRoute()
const gardenId = Number(route.params.id)

const garden = ref(null)
usePageTitle(() => (garden.value ? `Potager "${garden.value.name}"` : null))
const plants = ref([]) // GardenPlantDTO[]
const areas = ref([]) // AreaDTO[]
const ownedPlants = ref([]) // PlantDTO[], toutes les plantes du compte (placées ou non)
const loading = ref(true)
const error = ref(null)

// Le canvas occupe toute la place disponible : la taille du stage suit celle de son conteneur.
const stageSize = reactive({ width: 560, height: 560 })
const canvasEl = ref(null)

const resizeObserver = new ResizeObserver(([entry]) => {
  stageSize.width = Math.max(1, Math.floor(entry.contentRect.width))
  stageSize.height = Math.max(1, Math.floor(entry.contentRect.height))
})

// Le conteneur n'existe qu'une fois le potager charge (v-if) : on l'observe des qu'il apparait.
watch(canvasEl, (el, previous) => {
  if (previous) {
    resizeObserver.unobserve(previous)
  }
  if (el) {
    resizeObserver.observe(el)
  }
})

onBeforeUnmount(() => resizeObserver.disconnect())

const { stageConfig, stagePos, scale, onWheel, onStageDragMove } = useKonvaZoomPan(stageSize)
const { backgroundConfig, gridConfig, areaFillConfig } = useGardenBackground({ stagePos, scale, stageConfig })

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
          width: associationLineWidth(from, to, plantRadius(from.plantId), plantRadius(to.plantId)),
          rgb: link.positive ? ASSOCIATION_COLORS.positive : ASSOCIATION_COLORS.negative,
          sceneFunc: drawAssociationLine,
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

// Une plante occupe un rectangle de matrixX x matrixY cases de 2 * rayon de cote, centre sur son point (x, y).
// Les coins sont arrondis au rayon de la plante : en 1 x 1 le rectangle est exactement le cercle d'origine.
function roundedRectClip(ctx, width, height, cornerRadius) {
  ctx.roundRect(-width / 2, -height / 2, width, height, cornerRadius)
}

// Un clipFunc par dimension (et non par plante et par rendu) : sa reference reste stable d'un rendu a l'autre.
const clipFuncsBySize = new Map()

function clipFuncForSize(width, height, cornerRadius) {
  const key = `${width}x${height}x${cornerRadius}`
  if (!clipFuncsBySize.has(key)) {
    clipFuncsBySize.set(key, (ctx) => roundedRectClip(ctx, width, height, cornerRadius))
  }
  return clipFuncsBySize.get(key)
}

// Configs Konva de chaque plante, recalculees uniquement quand les plantes, la selection ou les images changent,
// pas a chaque pan/zoom. Le rectangle est le seul noeud qui ecoute la souris (zone de clic/drag du groupe) :
// l'image, son clip et la quantite sont exclus du hit graph.
// Le legume n'est dessine qu'une fois, a la taille de la plus petite dimension du rectangle et centre dessus ;
// la quantite (x<colonnes * lignes>) se place en bas a droite de cette image.
const plantNodes = computed(() =>
  plants.value.map((plant) => {
    const radius = plantRadius(plant.plantId)
    const width = plant.matrixX * 2 * radius
    const height = plant.matrixY * 2 * radius
    const cornerRadius = Math.min(radius, width / 2, height / 2)
    const imageSize = Math.min(width, height)
    const quantity = plant.matrixX * plant.matrixY
    const fontSize = Math.max(10, radius * 0.7)
    return {
      id: plant.id,
      group: { x: plant.x, y: plant.y, draggable: plant.state === 'A_PLANTER' },
      rect: {
        x: -width / 2,
        y: -height / 2,
        width,
        height,
        cornerRadius,
        stroke: plantStrokeColor(plant.state),
        strokeWidth: 2,
        fill: plant.id === selectedGardenPlantId.value ? '#2c8a3d' : undefined,
        perfectDrawEnabled: false,
      },
      clip: { clipFunc: clipFuncForSize(width, height, cornerRadius), listening: false },
      image: {
        image: plantImage(plant.plantId),
        width: imageSize,
        height: imageSize,
        offsetX: imageSize / 2,
        offsetY: imageSize / 2,
        opacity: plant.state === 'RECOLTEE' ? 0.5 : 1,
        listening: false,
        perfectDrawEnabled: false,
      },
      quantity:
        quantity > 1
          ? {
              text: `x${quantity}`,
              x: -imageSize / 2,
              y: imageSize / 2 - fontSize - 4,
              width: imageSize - 4,
              align: 'right',
              fontSize,
              fontStyle: 'bold',
              fill: '#ffffff',
              stroke: '#000000',
              strokeWidth: 3,
              fillAfterStrokeEnabled: true,
              listening: false,
              perfectDrawEnabled: false,
            }
          : null,
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

const paletteOpen = ref(false)
const paletteSearch = ref('')

// Insensible a la casse et aux accents : "ete" retrouve "Été".
function normalizeText(text) {
  return text.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()
}

const filteredPlants = computed(() => {
  const query = normalizeText(paletteSearch.value.trim())
  if (query === '') {
    return ownedPlants.value
  }
  return ownedPlants.value.filter(
    (p) => normalizeText(p.species).includes(query) || normalizeText(p.variety).includes(query),
  )
})
const showAssociations = ref(true)

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

// --- Matrice de legumes (X colonnes x Y lignes) de la plante selectionnee ---

const MAX_MATRIX_SIZE = 50

const matrixXInput = ref(1)
const matrixYInput = ref(1)

// Les champs suivent la plante selectionnee.
watch(
  selectedPlant,
  (plant) => {
    matrixXInput.value = plant?.matrixX ?? 1
    matrixYInput.value = plant?.matrixY ?? 1
  },
  { immediate: true },
)

function isValidMatrixSize(size) {
  return Number.isInteger(size) && size >= 1 && size <= MAX_MATRIX_SIZE
}

const canApplyMatrix = computed(
  () => isValidMatrixSize(matrixXInput.value) && isValidMatrixSize(matrixYInput.value),
)

async function applyMatrix() {
  if (!selectedPlant.value || !canApplyMatrix.value) {
    return
  }
  try {
    const updated = await updatePlantMatrix(gardenId, selectedPlant.value.id, {
      x: matrixXInput.value,
      y: matrixYInput.value,
    })
    selectedPlant.value.matrixX = updated.matrixX
    selectedPlant.value.matrixY = updated.matrixY
  } catch {
    error.value = 'Impossible de modifier la quantité de cette plante.'
  }
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
      <div ref="canvasEl" class="canvas-wrapper" @dragover.prevent @drop="onCanvasDrop">
          <v-stage :config="stageConfig" @wheel="onWheel" @dragmove="onStageDragMove">
            <v-layer>
              <v-rect :config="backgroundConfig" />
              <v-shape :config="gridConfig" />

              <template v-for="shape in areaShapes" :key="shape.id">
                <v-shape :config="shape.fill" />
                <v-text v-for="label in shape.labels" :key="label.key" :config="label.config" />
              </template>

              <template v-if="showAssociations">
                <v-shape v-for="line in associationLines" :key="line.key" :config="line.config" />
              </template>

              <v-group
                v-for="node in plantNodes"
                :key="node.id"
                :config="node.group"
                @dragend="node.onDragEnd"
                @click="node.onSelect"
                @tap="node.onSelect"
              >
                <v-rect :config="node.rect" />
                <v-group :config="node.clip">
                  <v-image :config="node.image" />
                </v-group>
                <v-text v-if="node.quantity" :config="node.quantity" />
              </v-group>
            </v-layer>
          </v-stage>

          <p v-if="garden.score !== null && garden.score !== undefined" class="garden-score">
            <!-- Le serveur calcule un score sur 10 : converti en 0 a 5 etoiles. -->
            <StarRating :value="garden.score / 2" />
            <span class="garden-score-value">{{ garden.score.toFixed(1).replace('.', ',') }}/10</span>
          </p>
          <p v-else class="garden-score hint">Score d'association : aucune plante voisine</p>

          <RouterLink
            :to="{ name: 'garden-list' }"
            class="back-to-gardens"
            title="Mes potagers"
            aria-label="Mes potagers"
          >
            <img :src="leftIcon" alt="" />
          </RouterLink>

          <RouterLink
            :to="{ name: 'garden-structure', params: { id: gardenId } }"
            class="edit-structure"
            title="Structure du potager"
            aria-label="Structure du potager"
          >
            <img :src="editIcon" alt="" />
          </RouterLink>

          <button
            type="button"
            class="associations-toggle"
            :class="{ off: !showAssociations }"
            :title="showAssociations ? 'Masquer les associations' : 'Afficher les associations'"
            :aria-label="showAssociations ? 'Masquer les associations' : 'Afficher les associations'"
            :aria-pressed="showAssociations"
            @click="showAssociations = !showAssociations"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M1.5 12S5.5 5 12 5s10.5 7 10.5 7-4 7-10.5 7S1.5 12 1.5 12z" />
              <circle cx="12" cy="12" r="3" />
              <line v-if="!showAssociations" x1="4" y1="20" x2="20" y2="4" />
            </svg>
          </button>

          <button
            type="button"
            class="palette-toggle"
            title="Grainetier"
            aria-label="Grainetier"
            @click="paletteOpen = !paletteOpen"
          >
            +
          </button>

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
            <form class="matrix-form" @submit.prevent="applyMatrix">
              <label class="field">
                X
                <input v-model.number="matrixXInput" type="number" min="1" :max="MAX_MATRIX_SIZE" step="1" />
              </label>
              <label class="field">
                Y
                <input v-model.number="matrixYInput" type="number" min="1" :max="MAX_MATRIX_SIZE" step="1" />
              </label>
              <button type="submit" class="btn" :disabled="!canApplyMatrix">OK</button>
            </form>
            <button type="button" class="btn" @click="removeSelectedPlant">Retirer du potager</button>
          </section>

          <div
            v-if="paletteOpen"
            class="palette-backdrop"
            @click.self="paletteOpen = false"
          >
            <!-- .stop : on ne peut pas deposer une plante sur la fenetre elle-meme (elle masque le potager) -->
            <div class="palette-window" @dragover.stop @drop.stop>
              <div class="palette-header">
                <h2>Grainetier</h2>
                <input
                  v-model="paletteSearch"
                  type="search"
                  class="palette-search"
                  placeholder="Rechercher une espèce ou variété"
                  aria-label="Rechercher une plante par espèce ou variété"
                />
                <RouterLink
                  :to="{ name: 'plant-list' }"
                  class="edit-plants"
                  title="Mes plantes"
                  aria-label="Mes plantes"
                >
                  <img :src="editIcon" alt="" />
                </RouterLink>
              </div>
              <p v-if="ownedPlants.length === 0" class="hint">
                Vous n'avez pas encore de plante. Ajoutez-en depuis votre compte.
              </p>
              <p v-else-if="filteredPlants.length === 0" class="hint">
                Aucune plante ne correspond à votre recherche.
              </p>
              <template v-else>
                <ul class="list-reset palette">
                  <li
                    v-for="plant in filteredPlants"
                    :key="plant.id"
                    class="list-card"
                    draggable="true"
                    @dragstart="onPaletteDragStart($event, plant)"
                  >
                    <img :src="resolveSpeciesImageUrl(plant.species)" :alt="plant.species" class="palette-thumb" />
                    <span class="palette-text">
                      <span class="palette-species">{{ plant.species }}</span>
                      <span class="palette-variety">{{ plant.variety }}</span>
                      <span class="palette-comment" :class="{ empty: !plant.comment }">
                        {{ plant.comment || 'Aucune remarque' }}
                      </span>
                    </span>
                  </li>
                </ul>
                <p class="hint">Glisser une plante sur le potager pour la placer.</p>
              </template>
            </div>
          </div>
        </div>
    </template>
  </div>
</template>

<style scoped>
/* La vue occupe toute la fenetre sous l'entete ; le canvas prend tout l'espace restant. */
.garden-detail {
  display: flex;
  flex-direction: column;
  max-width: none;
  height: calc(100dvh - var(--header-height));
  margin: 0;
  padding: 0;
  overflow: hidden;
}

.garden-detail > p {
  margin: 0;
  padding: 12px 20px;
}

.canvas-wrapper {
  position: relative;
  flex: 1;
  min-height: 0;
  width: 100%;
  box-sizing: border-box;
  border: none;
  border-radius: 0;
}

.back-to-gardens,
.edit-structure {
  position: absolute;
  top: 12px;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 2px solid var(--accent-border);
  background: var(--bg);
  box-shadow: var(--shadow);
}

.back-to-gardens {
  left: 12px;
}

.edit-structure {
  right: 12px;
}

.back-to-gardens img {
  height: 20px;
  width: 20px;
  display: block;
}

.edit-structure img {
  height: 24px;
  width: 24px;
  display: block;
}

.palette-toggle,
.associations-toggle {
  position: absolute;
  left: 12px;
  bottom: 12px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 2px solid var(--accent-border);
  background: var(--bg);
  color: var(--text-h);
  font-size: 24px;
  line-height: 1;
  cursor: pointer;
  box-shadow: var(--shadow);
}

/* Au-dessus du bouton "+" (40px + 8px d'ecart). */
.associations-toggle {
  bottom: 60px;
  padding: 0;
}

.associations-toggle svg {
  width: 22px;
  height: 22px;
  display: block;
  margin: auto;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.associations-toggle.off {
  color: var(--text);
}

.palette-backdrop {
  position: absolute;
  inset: 0;
}

.palette-window {
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 60px;
  height: 50%;
  box-sizing: border-box;
  overflow-y: auto;
  padding: 12px;
  border-radius: 6px;
  border: 1px solid var(--accent-border);
  background: var(--bg);
  box-shadow: var(--shadow);
}

.palette-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.palette-header h2 {
  margin: 0;
}

.palette-search {
  flex: 1;
  min-width: 0;
  padding: 6px 8px;
  border-radius: 6px;
  border: 1px solid var(--border);
  background: var(--bg);
  color: var(--text-h);
  font: inherit;
}

.edit-plants {
  display: inline-flex;
  flex-shrink: 0;
}

.edit-plants img {
  height: 32px;
  width: 32px;
  display: block;
}

.palette {
  margin-bottom: 8px;
  gap: 4px;
}

.palette li {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: grab;
}

.palette-thumb {
  width: 48px;
  height: 48px;
  object-fit: contain;
  flex-shrink: 0;
}

.palette-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.palette-text > span {
  min-height: 1.3em;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.palette-species {
  font-weight: bold;
}

.palette-comment {
  font-size: 13px;
}

.palette-comment.empty {
  font-style: italic;
  opacity: 0.6;
}

.hint {
  font-size: 13px;
  color: var(--text);
}

.garden-score {
  position: absolute;
  top: 12px;
  left: 64px;
  right: 64px;
  z-index: 1;
  margin: 0;
  text-align: center;
  font-size: 14px;
  text-shadow: 0 0 4px var(--bg), 0 0 4px var(--bg);
  pointer-events: none;
}

.garden-score-value {
  display: block;
  color: #888;
}

/* Panneau de la plante selectionnee : superpose au canvas (en bas a droite) pour ne pas le decaler. */
.plant-panel {
  position: absolute;
  right: 12px;
  bottom: 12px;
  width: 240px;
  max-width: calc(100% - 24px);
  box-sizing: border-box;
  padding: 12px;
  border-radius: 6px;
  border: 1px solid var(--accent-border);
  background: var(--bg);
  box-shadow: var(--shadow);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.matrix-form {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.matrix-form .field {
  flex: 1;
  min-width: 0;
}
</style>
