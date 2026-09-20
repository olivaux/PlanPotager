import { computed } from 'vue'
import { useImage } from './useImage.js'
import { areaToPoints } from '../utils/areaGeometry.js'
import { PAPER_COLOR, buildHatch, buildOutlines, drawMarkerArea } from '../utils/markerSketch.js'
import dirtUrl from '../assets/dirt512.jpg'

// Remplissage des zones : 'texture' (image de terre) ou 'hatch' (hachures au feutre brun, dans markerSketch.js).
// Le contour noir au feutre est trace dans les deux cas.
const AREA_FILL = 'texture'

// Objet constant : une reference stable evite qu'un config identique soit reapplique au noeud Konva a chaque rendu.
const DIRT_PATTERN_SCALE = { x: 0.5, y: 0.5 }

export function useGardenBackground({ stagePos, scale, stageConfig }) {
  const dirtImage = useImage(dirtUrl)

  const backgroundConfig = computed(() => ({
    x: -stagePos.value.x / scale.value,
    y: -stagePos.value.y / scale.value,
    width: stageConfig.value.width / scale.value,
    height: stageConfig.value.height / scale.value,
    fill: PAPER_COLOR,
    listening: false,
    perfectDrawEnabled: false,
  }))

  // Config d'un <v-shape> : la geometrie (contour, et hachures le cas echeant) est calculee ici, une fois par
  // changement de la zone, et sceneFunc ne fait que la tracer a chaque rendu (pan/zoom).
  function areaFillConfig(area) {
    const points = areaToPoints(area)
    const seed = area.id ?? 0
    const config = {
      outlines: buildOutlines(points, seed),
      sceneFunc: drawMarkerArea,
      listening: false,
      perfectDrawEnabled: false,
    }

    if (AREA_FILL === 'hatch') {
      config.hatch = buildHatch(points, seed)
    } else {
      config.points = points
      config.fillPatternImage = dirtImage.value
      config.fillPatternRepeat = 'repeat'
      // Pattern ancre a l'origine du monde (la shape est en 0, 0) : il ne glisse pas quand une zone bouge.
      config.fillPatternScale = DIRT_PATTERN_SCALE
    }
    return config
  }

  return { backgroundConfig, areaFillConfig }
}
