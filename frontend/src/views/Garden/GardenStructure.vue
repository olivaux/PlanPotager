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
import { PAPER_COLOR, HOVER_COLOR } from '../../utils/markerSketch.js'
import { CORNER_KEYS, EDGES, areaToPoints, edgeMidpoint, edgeLength, setEdgeLength } from '../../utils/areaGeometry.js'
import { usePageTitle } from '../../composables/usePageTitle.js'

const route = useRoute()
const gardenId = Number(route.params.id)

const garden = ref(null)
usePageTitle(() => (garden.value ? `Structure du potager ${garden.value.name}` : null))
const areas = ref([]) // AreaDTO[]
const loading = ref(true)
const error = ref(null)

const { stageConfig, stagePos, scale, onWheel, onStageDragMove } = useKonvaZoomPan({ width: 560, height: 560 })
const { backgroundConfig, gridConfig, areaFillConfig } = useGardenBackground({ stagePos, scale, stageConfig })
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

// --- Survol : point, arete ou surface survole passe du noir au vert ---

const INK_COLOR = '#161616'

// Un seul element est survole a la fois (Konva ne signale que la forme la plus haute sous le curseur).
// { kind: 'corner' | 'edge' | 'area', areaId, keyA?, keyB? } ou null.
const hovered = ref(null)

function sameHover(a, b) {
  return a?.kind === b.kind && a.areaId === b.areaId && a.keyA === b.keyA && a.keyB === b.keyB
}

function hover(target) {
  hovered.value = target
}

function unhover(target) {
  // Ne rien effacer si un autre element a deja pris le relais (l'ordre leave/enter n'est pas garanti).
  if (sameHover(hovered.value, target)) {
    hovered.value = null
  }
}

// Le survol d'une arete ou d'une surface est dessine par le shape de la zone lui-meme (contour et remplissage
// recolores, voir drawMarkerArea) : on lui passe l'etat de survol, sans ajouter de forme. Recalcule uniquement
// quand les zones ou le survol changent, pas a chaque pan/zoom.
const areaFillConfigs = computed(
  () =>
    new Map(
      areaShapes.value.map((shape) => {
        const target = hovered.value?.areaId === shape.id ? hovered.value : null
        const hoverEdge =
          target?.kind === 'edge'
            ? EDGES.findIndex(([keyA, keyB]) => keyA === target.keyA && keyB === target.keyB)
            : -1
        return [shape.id, { ...shape.fill, hoverArea: target?.kind === 'area', hoverEdge }]
      }),
    ),
)

// Configs et handlers des poignees, calcules quand les zones (ou le survol) changent et non a chaque rendu (pan/zoom).
const cornerShapes = computed(
  () =>
    new Map(
      areas.value.map((area) => [
        area.id,
        CORNER_KEYS.map((key) => {
          const target = { kind: 'corner', areaId: area.id, keyA: key }
          return {
            key,
            config: {
              x: area[`${key}X`],
              y: area[`${key}Y`],
              radius: 6,
              fill: sameHover(hovered.value, target) ? HOVER_COLOR : INK_COLOR,
              // Liseré clair : un point noir reste visible sur la texture de terre.
              stroke: PAPER_COLOR,
              strokeWidth: 1.5,
              draggable: true,
              perfectDrawEnabled: false,
            },
            onDragMove: (konvaEvent) => onCornerDragMove(area, key, konvaEvent),
            onDragEnd: () => onCornerDragEnd(area),
            onEnter: () => hover(target),
            onLeave: () => unhover(target),
          }
        }),
      ]),
    ),
)

// --- Deplacement d'une arete (2 sommets) ou d'une zone entiere (4 sommets) ---

// Tous les sommets concernes suivent la meme translation. Les lignes de saisie sont invisibles et restent en (0, 0) :
// leur position pendant le glisser est donc directement le deplacement. Le deplacement est toujours calcule depuis
// les positions de depart, memorisees au debut du glisser.
let moveDragStart = null

function onMoveDragStart(area, keys) {
  moveDragStart = keys.map((key) => ({ key, x: area[`${key}X`], y: area[`${key}Y`] }))
}

function onMoveDragMove(area, konvaEvent) {
  const dx = Math.round(konvaEvent.target.x())
  const dy = Math.round(konvaEvent.target.y())
  for (const start of moveDragStart) {
    area[`${start.key}X`] = start.x + dx
    area[`${start.key}Y`] = start.y + dy
  }
}

async function onMoveDragEnd(area, konvaEvent, errorMessage) {
  // Les sommets ont deja bouge : la ligne revient en (0, 0) pour se recaler sur ses nouveaux points.
  konvaEvent.target.position({ x: 0, y: 0 })
  moveDragStart = null
  try {
    await updateArea(gardenId, area.id, area)
  } catch {
    error.value = errorMessage
  }
}

// Config et handlers d'une ligne de deplacement : l'appelant fournit sa forme et les sommets qu'elle deplace.
function moveHandle(area, keys, target, lineConfig, errorMessage) {
  return {
    config: { ...lineConfig, draggable: true, perfectDrawEnabled: false },
    onDragStart: () => onMoveDragStart(area, keys),
    onDragMove: (konvaEvent) => onMoveDragMove(area, konvaEvent),
    onDragEnd: (konvaEvent) => onMoveDragEnd(area, konvaEvent, errorMessage),
    onEnter: (konvaEvent) => {
      setCursor(konvaEvent, 'move')
      hover(target)
    },
    onLeave: (konvaEvent) => {
      setCursor(konvaEvent, '')
      unhover(target)
    },
  }
}

// Transparentes mais cliquables : seule la zone de saisie compte (hitStrokeWidth pour les aretes, fill pour la zone).
const TRANSPARENT = 'rgba(0, 0, 0, 0)'

const edgeHandles = computed(
  () =>
    new Map(
      areas.value.map((area) => [
        area.id,
        EDGES.map(([keyA, keyB]) => ({
          key: `${area.id}-${keyA}-${keyB}`,
          ...moveHandle(
            area,
            [keyA, keyB],
            { kind: 'edge', areaId: area.id, keyA, keyB },
            {
              points: [area[`${keyA}X`], area[`${keyA}Y`], area[`${keyB}X`], area[`${keyB}Y`]],
              stroke: TRANSPARENT,
              strokeWidth: 1,
              hitStrokeWidth: 16,
            },
            'Impossible de déplacer cette arête.',
          ),
        })),
      ]),
    ),
)

const areaHandles = computed(
  () =>
    new Map(
      areas.value.map((area) => [
        area.id,
        moveHandle(
          area,
          CORNER_KEYS,
          { kind: 'area', areaId: area.id },
          { points: areaToPoints(area), closed: true, fill: TRANSPARENT },
          'Impossible de déplacer cette zone.',
        ),
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
              <v-shape :config="gridConfig" />

              <template v-for="shape in areaShapes" :key="shape.id">
                <v-shape :config="areaFillConfigs.get(shape.id)" />
                <v-line
                  :config="areaHandles.get(shape.id).config"
                  @dragstart="areaHandles.get(shape.id).onDragStart"
                  @dragmove="areaHandles.get(shape.id).onDragMove"
                  @dragend="areaHandles.get(shape.id).onDragEnd"
                  @mouseenter="areaHandles.get(shape.id).onEnter"
                  @mouseleave="areaHandles.get(shape.id).onLeave"
                />
                <v-line
                  v-for="edge in edgeHandles.get(shape.id)"
                  :key="edge.key"
                  :config="edge.config"
                  @dragstart="edge.onDragStart"
                  @dragmove="edge.onDragMove"
                  @dragend="edge.onDragEnd"
                  @mouseenter="edge.onEnter"
                  @mouseleave="edge.onLeave"
                />
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
                  @mouseenter="corner.onEnter"
                  @mouseleave="corner.onLeave"
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