// Rendu "feutre" des zones : contour noir et, au choix, hachures brunes, legerement deformes comme dessines a la main.
// Le remplissage (hachures ou texture de terre) est choisi dans useGardenBackground ; le contour est toujours trace.
// Tout est deterministe (graine = id de la zone) : rien ne scintille d'un rendu a l'autre.
// Les hachures sont ancrees au repere du monde (index de ligne), pas a la zone : deplacer un sommet
// ne fait donc pas "glisser" le dessin, seules les extremites des traits bougent.

export const PAPER_COLOR = '#f6efdc'

// Survol (page structure) : l'element survole passe du noir au vert.
export const HOVER_COLOR = '#2fbf4a'
const HOVER_OUTLINE_COLORS = [HOVER_COLOR, 'rgba(47, 191, 74, 0.5)'] // un par passage de OUTLINE_PASSES
const HOVER_AREA_FILL = 'rgba(47, 191, 74, 0.4)'

const HATCH_COLOR = 'rgba(133, 84, 40, 0.8)' // semi-transparent : les traits qui se chevauchent foncent
const HATCH_WIDTH = 8 // proche de l'ecart : les traits deformes se touchent et se recouvrent par endroits
const HATCH_ANGLE = -Math.PI / 4
const HATCH_SPACING = 15
const HATCH_SPACING_JITTER = 0.25 // part de l'ecart, en +/-
const HATCH_WOBBLE = 1.4 // deformation laterale max d'un trait
const HATCH_TILT = 1.5 // ecart max de pente entre les deux bouts d'un trait
const HATCH_INSET_MIN = HATCH_WIDTH / 2 - 1 // le bout arrondi du trait vient effleurer le contour
const HATCH_INSET_VARIATION = 5
const HATCH_MIN_LENGTH = 4

// `width` = epaisseur maximale du trait de pinceau (voir brushWidth).
const OUTLINE_PASSES = [
  { color: '#161616', width: 5, wobble: 1.6 },
  { color: 'rgba(22, 22, 22, 0.5)', width: 3, wobble: 2.2 }, // second passage : le feutre "bave"
]
const OUTLINE_OVERSHOOT = 3 // un cote depasse un peu du coin
const OUTLINE_STEP = 4 // pas d'echantillonnage du ruban : assez fin pour qu'il paraisse lisse
// Effet pinceau : le trait attaque a moitie d'epaisseur, s'epaissit, puis s'effile en fin de geste.
const BRUSH_ATTACK_LENGTH = 6
const BRUSH_ATTACK_MIN = 0.5
const BRUSH_RELEASE_LENGTH = 16
const BRUSH_RELEASE_MIN = 0.12
const BRUSH_PRESSURE = 0.2 // variation d'epaisseur le long du trait, en +/-

const STEP = 18 // pas d'echantillonnage des traits deformes
const TAU = Math.PI * 2

// Hash entier -> [0, 1). Meme entrees, meme sortie.
function rand(seed, i, j = 0) {
  let h = (Math.imul(seed, 374761393) + Math.imul(i, 668265263) + Math.imul(j, 2246822519)) | 0
  h = Math.imul(h ^ (h >>> 13), 1274126177)
  h ^= h >>> 16
  return (h >>> 0) / 4294967296
}

// Deformation lisse et continue en fonction de la position le long du trait.
function wobble(seed, key, pos, amplitude) {
  const phase1 = rand(seed, key, 1) * TAU
  const phase2 = rand(seed, key, 2) * TAU
  return amplitude * (0.65 * Math.sin(pos / 23 + phase1) + 0.35 * Math.sin(pos / 9 + phase2))
}

function hatchStroke(seed, key, offset, t0, t1, ux, uy) {
  const nx = -uy
  const ny = ux
  const length = t1 - t0
  const count = Math.max(1, Math.ceil(length / STEP))
  const tilt = (rand(seed, key, 3) - 0.5) * 2 * HATCH_TILT
  const pts = []
  for (let j = 0; j <= count; j++) {
    const ratio = j / count
    const t = t0 + length * ratio
    const lateral = offset + wobble(seed, key, t, HATCH_WOBBLE) + tilt * (ratio - 0.5)
    pts.push(ux * t + nx * lateral, uy * t + ny * lateral)
  }
  return pts
}

// Traits paralleles decoupes par le polygone (formes concaves comprises : croisements pris deux par deux).
export function buildHatch(points, seed) {
  const ux = Math.cos(HATCH_ANGLE)
  const uy = Math.sin(HATCH_ANGLE)
  const nx = -uy
  const ny = ux
  const count = points.length / 2

  let minD = Infinity
  let maxD = -Infinity
  for (let i = 0; i < count; i++) {
    const d = points[2 * i] * nx + points[2 * i + 1] * ny
    minD = Math.min(minD, d)
    maxD = Math.max(maxD, d)
  }

  const strokes = []
  for (let k = Math.ceil(minD / HATCH_SPACING) - 1; k <= Math.floor(maxD / HATCH_SPACING) + 1; k++) {
    const offset = k * HATCH_SPACING + (rand(seed, k, 4) - 0.5) * 2 * HATCH_SPACING * HATCH_SPACING_JITTER

    const crossings = []
    for (let i = 0; i < count; i++) {
      const ax = points[2 * i]
      const ay = points[2 * i + 1]
      const bx = points[2 * ((i + 1) % count)]
      const by = points[2 * ((i + 1) % count) + 1]
      const da = ax * nx + ay * ny - offset
      const db = bx * nx + by * ny - offset
      if (da < 0 !== db < 0) {
        const f = da / (da - db)
        crossings.push((ax + f * (bx - ax)) * ux + (ay + f * (by - ay)) * uy)
      }
    }
    crossings.sort((a, b) => a - b)

    for (let i = 0; i + 1 < crossings.length; i += 2) {
      const key = k * 8 + i / 2
      const t0 = crossings[i] + HATCH_INSET_MIN + rand(seed, key, 5) * HATCH_INSET_VARIATION
      const t1 = crossings[i + 1] - HATCH_INSET_MIN - rand(seed, key, 6) * HATCH_INSET_VARIATION
      if (t1 - t0 >= HATCH_MIN_LENGTH) {
        strokes.push(hatchStroke(seed, key, offset, t0, t1, ux, uy))
      }
    }
  }
  return strokes
}

// Epaisseur du pinceau a la position `pos` d'un trait de longueur `total`.
function brushWidth(seed, key, pos, total, maxWidth) {
  const attack = Math.min(1, BRUSH_ATTACK_MIN + ((1 - BRUSH_ATTACK_MIN) * pos) / BRUSH_ATTACK_LENGTH)
  const release = Math.min(1, BRUSH_RELEASE_MIN + ((1 - BRUSH_RELEASE_MIN) * (total - pos)) / BRUSH_RELEASE_LENGTH)
  const pressure = 1 + BRUSH_PRESSURE * wobble(seed, key + 500, pos, 1)
  return maxWidth * attack * release * pressure
}

// Un trait de pinceau par cote (et non un chemin ferme) : les bouts effiles se croisent aux coins.
// Chaque trait est un polygone (ruban a largeur variable autour de la ligne deformee) a remplir.
function buildOutlinePass(points, seed, passIndex) {
  const { wobble: amplitude, width } = OUTLINE_PASSES[passIndex]
  const count = points.length / 2
  const polygons = []
  for (let e = 0; e < count; e++) {
    const key = 100 + passIndex * 10 + e
    let ax = points[2 * e]
    let ay = points[2 * e + 1]
    let bx = points[2 * ((e + 1) % count)]
    let by = points[2 * ((e + 1) % count) + 1]
    const length = Math.hypot(bx - ax, by - ay)
    if (length === 0) {
      polygons.push([]) // garde l'indice du polygone aligne sur celui du cote
      continue
    }
    const dx = (bx - ax) / length
    const dy = (by - ay) / length
    const over0 = rand(seed, key, 7) * OUTLINE_OVERSHOOT
    const over1 = rand(seed, key, 8) * OUTLINE_OVERSHOOT
    ax -= dx * over0
    ay -= dy * over0
    bx += dx * over1
    by += dy * over1

    const total = length + over0 + over1
    const steps = Math.max(1, Math.ceil(total / OUTLINE_STEP))
    const left = []
    const right = []
    for (let j = 0; j <= steps; j++) {
      const pos = (total * j) / steps
      // Position mesuree depuis le coin, pas depuis le debut du trait : la deformation ne depend que de
      // l'endroit sur le cote, donc allonger un cote ne redessine pas ce qui existait deja.
      const along = pos - over0
      const lateral = wobble(seed, key, along, amplitude) * Math.min(1, pos / 10, (total - pos) / 10)
      const cx = ax + dx * pos - dy * lateral
      const cy = ay + dy * pos + dx * lateral
      const half = brushWidth(seed, key, pos, total, width) / 2
      left.push(cx - dy * half, cy + dx * half)
      right.push(cx + dy * half, cy - dx * half)
    }
    for (let j = steps; j >= 0; j--) {
      left.push(right[2 * j], right[2 * j + 1])
    }
    polygons.push(left)
  }
  return polygons
}

// Contour d'une zone a partir de ses sommets ([x0, y0, x1, y1, ...]) : un jeu de polygones par passage.
export function buildOutlines(points, seed) {
  return OUTLINE_PASSES.map((_, passIndex) => buildOutlinePass(points, seed, passIndex))
}

// Chemin lisse : courbes quadratiques passant par le milieu de chaque segment.
function tracePath(ctx, pts) {
  ctx.moveTo(pts[0], pts[1])
  const last = pts.length / 2 - 1
  if (last < 2) {
    ctx.lineTo(pts[2 * last], pts[2 * last + 1])
    return
  }
  for (let i = 1; i < last; i++) {
    const mx = (pts[2 * i] + pts[2 * i + 2]) / 2
    const my = (pts[2 * i + 1] + pts[2 * i + 3]) / 2
    ctx.quadraticCurveTo(pts[2 * i], pts[2 * i + 1], mx, my)
  }
  ctx.lineTo(pts[2 * last], pts[2 * last + 1])
}

// Un stroke par trait : l'encre s'accumule quand deux traits se recouvrent (un seul stroke n'aurait pas cet effet).
function strokeEach(ctx, paths, color, width) {
  ctx.setAttr('strokeStyle', color)
  ctx.setAttr('lineWidth', width)
  paths.forEach((pts) => {
    ctx.beginPath()
    tracePath(ctx, pts)
    ctx.stroke()
  })
}

function polygonPath(ctx, points) {
  ctx.beginPath()
  ctx.moveTo(points[0], points[1])
  for (let i = 2; i < points.length; i += 2) {
    ctx.lineTo(points[i], points[i + 1])
  }
  ctx.closePath()
}

// Un fill par trait : deux traits qui se croisent (aux coins) s'accumulent si la couleur est transparente.
// Le polygone d'indice `hoverEdge` (le cote survole) prend la couleur de survol.
function fillPolygons(ctx, polygons, color, hoverColor, hoverEdge) {
  polygons.forEach((points, edgeIndex) => {
    if (points.length === 0) {
      return
    }
    ctx.setAttr('fillStyle', edgeIndex === hoverEdge ? hoverColor : color)
    polygonPath(ctx, points)
    ctx.fill()
  })
}

// sceneFunc Konva. Attributs du shape : `outlines` (obligatoire), puis `hatch` (hachures) et/ou
// `points` + fillPatternImage (texture), selon le remplissage choisi.
// Attributs optionnels de survol : `hoverArea` (surface teintee de vert) et `hoverEdge` (indice du cote en vert).
export function drawMarkerArea(ctx, shape) {
  const { hatch, outlines, points, hoverArea, hoverEdge } = shape.attrs
  if (points && shape.fillPatternImage()) {
    polygonPath(ctx, points)
    ctx.fillShape(shape) // remplit avec le pattern (fillPattern*) du shape
    if (hoverArea) {
      ctx.setAttr('fillStyle', HOVER_AREA_FILL)
      ctx.fill()
    }
  }
  ctx.setAttr('lineCap', 'round')
  ctx.setAttr('lineJoin', 'round')
  if (hatch) {
    strokeEach(ctx, hatch, HATCH_COLOR, HATCH_WIDTH)
  }
  outlines.forEach((polygons, passIndex) => {
    fillPolygons(ctx, polygons, OUTLINE_PASSES[passIndex].color, HOVER_OUTLINE_COLORS[passIndex], hoverEdge)
  })
}
