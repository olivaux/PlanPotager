import { computed } from 'vue'
import { EDGES, edgeMidpoint, edgeLength } from '../utils/areaGeometry.js'
import { PAPER_COLOR } from '../utils/markerSketch.js'

// Configs Konva des zones (remplissage + longueur des cotes), calcules une fois par changement des zones
// et non a chaque rendu du template (pan/zoom). Tout est statique : rien n'ecoute la souris.
export function useAreaShapes(areas, areaFillConfig) {
  return computed(() =>
    areas.value.map((area) => ({
      id: area.id,
      area,
      fill: areaFillConfig(area),
      labels: EDGES.map(([keyA, keyB]) => {
        const { x, y } = edgeMidpoint(area, keyA, keyB)
        return {
          key: `${area.id}-${keyA}-${keyB}`,
          config: {
            x,
            y,
            text: edgeLength(area, keyA, keyB),
            fontSize: 12,
            fill: '#2b1d10',
            // Halo couleur papier : le texte reste lisible par-dessus les hachures.
            stroke: PAPER_COLOR,
            strokeWidth: 3,
            fillAfterStrokeEnabled: true,
            listening: false,
          },
        }
      }),
    })),
  )
}
