<script setup>
import { ref, computed, watch, nextTick, onMounted } from 'vue'
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
import { CORNER_KEYS, edgeMidpoint, edgeLength, setEdgeLength } from '../../utils/areaGeometry.js'
import { usePageTitle } from '../../composables/usePageTitle.js'

const route = useRoute()
const gardenId = Number(route.params.id)

const garden = ref(null)
usePageTitle(() => (garden.value ? `Structure du potager ${garden.value.name}` : null))
const areas = ref([]) // AreaDTO[]
const loading = ref(true)
const error = ref(null)

const { stageConfig, stagePos, scale, onWheel, onStageDragMove } = useKonvaZoomPan({ width: 560, height: 560 })
const { backgroundConfig, areaFillConfig } = useGardenBackground({ stagePos, scale, stageConfig })
const areaShapes = useAreaShapes(areas, areaFillConfig, { editableLabels: true })

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

// --- Saisie manuelle de la longueur d'un cote (clic sur sa dimension) ---

const editing = ref(null) // { area, keyA, keyB, value }
const lengthInput = ref(null)

// Le champ de saisie est un <input> HTML superpose au canvas, centre sur le milieu du cote (suit le pan/zoom).
const editingStyle = computed(() => {
  if (!editing.value) {
    return null
  }
  const { area, keyA, keyB } = editing.value
  const { x, y } = edgeMidpoint(area, keyA, keyB)
  return {
    left: `${x * scale.value + stagePos.value.x}px`,
    top: `${y * scale.value + stagePos.value.y}px`,
  }
})

watch(editing, async (value) => {
  if (value) {
    await nextTick()
    lengthInput.value?.select()
  }
})

function startEditLength(area, label) {
  editing.value = {
    area,
    keyA: label.keyA,
    keyB: label.keyB,
    value: edgeLength(area, label.keyA, label.keyB),
  }
}

function cancelEditLength() {
  editing.value = null
}

async function commitEditLength() {
  // Efface l'etat avant tout : le retrait de l'input declenche un blur qui ne doit pas revalider.
  const current = editing.value
  editing.value = null
  if (!current) {
    return
  }

  const length = Number(String(current.value).replace(',', '.'))
  if (!Number.isFinite(length) || length <= 0) {
    error.value = 'Longueur invalide : saisissez un nombre positif.'
    return
  }

  const { area, keyA, keyB } = current
  if (length === Number(edgeLength(area, keyA, keyB))) {
    return
  }

  const previous = { x: area[`${keyB}X`], y: area[`${keyB}Y`] }
  error.value = null
  setEdgeLength(area, keyA, keyB, length)
  try {
    await updateArea(gardenId, area.id, area)
  } catch {
    area[`${keyB}X`] = previous.x
    area[`${keyB}Y`] = previous.y
    error.value = 'Impossible de modifier cette longueur.'
  }
}

function setCursor(konvaEvent, cursor) {
  konvaEvent.target.getStage().container().style.cursor = cursor
}

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
                <v-shape :config="shape.fill" />
                <v-text
                  v-for="label in shape.labels"
                  :key="label.key"
                  :config="label.config"
                  @click="startEditLength(shape.area, label)"
                  @tap="startEditLength(shape.area, label)"
                  @mouseenter="setCursor($event, 'pointer')"
                  @mouseleave="setCursor($event, '')"
                />
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

          <input
            v-if="editing"
            ref="lengthInput"
            v-model="editing.value"
            class="length-input"
            :style="editingStyle"
            type="text"
            inputmode="decimal"
            aria-label="Longueur du côté"
            @keydown.enter.prevent="commitEditLength"
            @keydown.esc.prevent="cancelEditLength"
            @blur="commitEditLength"
          />
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

.canvas-wrapper {
  position: relative;
}

.length-input {
  position: absolute;
  z-index: 1;
  width: 64px;
  box-sizing: border-box;
  padding: 2px 4px;
  transform: translate(-50%, -50%);
  border: 1px solid var(--accent-border);
  border-radius: 4px;
  background: var(--bg);
  color: var(--text-h);
  font: inherit;
  font-size: 13px;
  text-align: center;
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