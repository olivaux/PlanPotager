export const CORNER_KEYS = ['leftUp', 'rightUp', 'rightDown', 'leftDown']

export const EDGES = [
  ['leftUp', 'rightUp'],
  ['rightUp', 'rightDown'],
  ['rightDown', 'leftDown'],
  ['leftDown', 'leftUp'],
]

export function areaToPoints(area) {
  return [
    area.leftUpX, area.leftUpY,
    area.rightUpX, area.rightUpY,
    area.rightDownX, area.rightDownY,
    area.leftDownX, area.leftDownY,
  ]
}

export function cornerPos(area, key) {
  return { x: area[`${key}X`], y: area[`${key}Y`] }
}

export function edgeMidpoint(area, keyA, keyB) {
  const a = cornerPos(area, keyA)
  const b = cornerPos(area, keyB)
  return { x: (a.x + b.x) / 2, y: (a.y + b.y) / 2 }
}

export function edgeLength(area, keyA, keyB) {
  const a = cornerPos(area, keyA)
  const b = cornerPos(area, keyB)
  return Math.hypot(b.x - a.x, b.y - a.y).toFixed(1)
}

// Impose la longueur d'un cote : le coin keyA reste en place, keyB glisse le long du cote (les deux cotes voisins
// de keyB changent donc aussi). Un cote de longueur nulle n'a pas de direction : on prend l'horizontale.
export function setEdgeLength(area, keyA, keyB, length) {
  const a = cornerPos(area, keyA)
  const b = cornerPos(area, keyB)
  const current = Math.hypot(b.x - a.x, b.y - a.y)
  const ux = current > 0 ? (b.x - a.x) / current : 1
  const uy = current > 0 ? (b.y - a.y) / current : 0
  area[`${keyB}X`] = Math.round((a.x + ux * length) * 100) / 100
  area[`${keyB}Y`] = Math.round((a.y + uy * length) * 100) / 100
}
