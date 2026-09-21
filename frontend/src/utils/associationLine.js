// Distance maximale (centre a centre) pour qu'un lien existe : miroir de ASSOCIATION_RADIUS_CM cote backend.
const ASSOCIATION_RADIUS = 100

// Largeur du lien a la limite du rayon, en proportion de sa largeur maximale (plantes collees).
const MIN_WIDTH_RATIO = 0.2

// Opacite au coeur du lien, et part de la largeur (de chaque cote) ou elle s'estompe jusqu'a 0.
const CORE_ALPHA = 0.85
const FADE_RATIO = 0.35

export const ASSOCIATION_COLORS = {
  positive: '61, 224, 91',
  negative: '240, 98, 84',
}

// Plantes collees (distance = somme des rayons) : diametre de la plus petite. Plus elles s'eloignent, plus le lien
// s'affine, lineairement, jusqu'a MIN_WIDTH_RATIO de cette largeur au bord du rayon d'association.
export function associationLineWidth(from, to, fromRadius, toRadius) {
  const maxWidth = 2 * Math.min(fromRadius, toRadius)
  const touching = fromRadius + toRadius
  const distance = Math.hypot(to.x - from.x, to.y - from.y)
  const span = ASSOCIATION_RADIUS - touching
  const closeness = span <= 0 ? 1 : Math.min(1, Math.max(0, (ASSOCIATION_RADIUS - distance) / span))
  return maxWidth * (MIN_WIDTH_RATIO + (1 - MIN_WIDTH_RATIO) * closeness)
}

// sceneFunc Konva. Attributs du shape : `points` [x1, y1, x2, y2], `width` et `rgb` ("r, g, b").
// Bande pleine dont les bords se fondent dans le decor grace a un degrade perpendiculaire au lien.
export function drawAssociationLine(ctx, shape) {
  const { points, width, rgb } = shape.attrs
  const [x1, y1, x2, y2] = points
  const half = width / 2

  const gradient = ctx.createLinearGradient(0, -half, 0, half)
  gradient.addColorStop(0, `rgba(${rgb}, 0)`)
  gradient.addColorStop(FADE_RATIO, `rgba(${rgb}, ${CORE_ALPHA})`)
  gradient.addColorStop(1 - FADE_RATIO, `rgba(${rgb}, ${CORE_ALPHA})`)
  gradient.addColorStop(1, `rgba(${rgb}, 0)`)

  ctx.save()
  ctx.translate(x1, y1)
  ctx.rotate(Math.atan2(y2 - y1, x2 - x1))
  ctx.setAttr('fillStyle', gradient)
  ctx.fillRect(0, -half, Math.hypot(x2 - x1, y2 - y1), width)
  ctx.restore()
}
