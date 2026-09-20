import { computed } from 'vue'
import { useImage } from './useImage.js'
import { areaToPoints } from '../utils/areaGeometry.js'
import grassUrl from '../assets/grass.jpg'
import dirtUrl from '../assets/dirt.jpg'

// Objets constants : une reference stable evite qu'un config identique soit reapplique au noeud Konva a chaque rendu.
const GRASS_PATTERN_SCALE = { x: 0.5, y: 0.5 }
const DIRT_PATTERN_SCALE = { x: 0.5, y: 0.5 }

export function useGardenBackground({ stagePos, scale, stageConfig }) {
  const grassImage = useImage(grassUrl)
  const dirtImage = useImage(dirtUrl)

  const backgroundConfig = computed(() => {
    const x = -stagePos.value.x / scale.value
    const y = -stagePos.value.y / scale.value
    return {
      x,
      y,
      width: stageConfig.value.width / scale.value,
      height: stageConfig.value.height / scale.value,
      fillPatternImage: grassImage.value,
      fillPatternRepeat: 'repeat',
      fillPatternScale: GRASS_PATTERN_SCALE,
      // Konva ancre le pattern à l'origine locale du shape (x, y), qui bouge ici à
      // chaque pan/zoom pour garder le rect collé au viewport. On compense pour que
      // le motif reste ancré au même point fixe que celui des zones (x=0, y=0).
      fillPatternX: -x,
      fillPatternY: -y,
      listening: false,
      perfectDrawEnabled: false,
    }
  })

  function areaFillConfig(area) {
    return {
      points: areaToPoints(area),
      closed: true,
      stroke: '#aa3bff',
      strokeWidth: 2,
      fillPatternImage: dirtImage.value,
      fillPatternRepeat: 'repeat',
      fillPatternScale: DIRT_PATTERN_SCALE,
      listening: false,
      perfectDrawEnabled: false,
    }
  }

  return { backgroundConfig, areaFillConfig }
}
