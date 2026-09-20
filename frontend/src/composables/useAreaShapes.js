import { computed } from 'vue'
import { EDGES, edgeMidpoint, edgeLength } from '../utils/areaGeometry.js'
import { PAPER_COLOR } from '../utils/markerSketch.js'

// Configs Konva des zones (remplissage + longueur des cotes), calcules une fois par changement des zones
// et non a chaque rendu du template (pan/zoom). Par defaut tout est statique : rien n'ecoute la souris.
// Avec editableLabels, les longueurs de cotes deviennent cliquables (zone de clic elargie par un padding).
const LABEL_HIT_PADDING = 6

export function useAreaShapes(areas, areaFillConfig, { editableLabels = false } = {}) {
  const padding = editableLabels ? LABEL_HIT_PADDING : 0

  return computed(() =>
    areas.value.map((area) => ({
      id: area.id,
      area,
      fill: areaFillConfig(area),
      labels: EDGES.map(([keyA, keyB]) => {
        const { x, y } = edgeMidpoint(area, keyA, keyB)
        return {
          key: `${area.id}-${keyA}-${keyB}`,
          keyA,
          keyB,
          config: {
            // Le padding decale le texte : on le compense pour que l'affichage reste identique.
            x: x - padding,
            y: y - padding,
            padding,
            text: edgeLength(area, keyA, keyB),
            fontSize: 12,
            fill: '#2b1d10',
            // Halo couleur papier : le texte reste lisible par-dessus les hachures.
            stroke: PAPER_COLOR,
            strokeWidth: 3,
            fillAfterStrokeEnabled: true,
            listening: editableLabels,
          },
        }
      }),
    })),
  )
}
