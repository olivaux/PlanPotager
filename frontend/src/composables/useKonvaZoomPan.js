import { ref, computed, onScopeDispose } from 'vue'

export function useKonvaZoomPan(stageSize) {
  const scale = ref(1.2)
  const stagePos = ref({ x: 0, y: 0 })

  // Etat a jour de maniere synchrone : plusieurs evenements (wheel, dragmove) peuvent arriver dans la meme frame
  // et chacun doit partir du resultat du precedent. Seule l'ecriture dans les refs (qui declenche le rendu) est
  // regroupee, une fois par frame.
  let latest = { scale: scale.value, pos: stagePos.value }
  let frameId = null

  function scheduleFlush() {
    if (frameId !== null) {
      return
    }
    frameId = requestAnimationFrame(() => {
      frameId = null
      scale.value = latest.scale
      stagePos.value = latest.pos
    })
  }

  onScopeDispose(() => {
    if (frameId !== null) {
      cancelAnimationFrame(frameId)
    }
  })

  const stageConfig = computed(() => ({
    width: stageSize.width,
    height: stageSize.height,
    scaleX: scale.value,
    scaleY: scale.value,
    x: stagePos.value.x,
    y: stagePos.value.y,
    draggable: true,
  }))

  function onStageDragMove(konvaEvent) {
    // Ignore le pan quand on déplace un élément (plante, point de zone...) à l'intérieur du stage.
    if (konvaEvent.target !== konvaEvent.target.getStage()) {
      return
    }
    const stage = konvaEvent.target.getStage()
    latest = { scale: latest.scale, pos: { x: stage.x(), y: stage.y() } }
    scheduleFlush()
  }

  function onWheel(konvaEvent) {
    konvaEvent.evt.preventDefault()
    const stage = konvaEvent.target.getStage()
    const oldScale = latest.scale
    const pointer = stage.getPointerPosition()
    if (!pointer) {
      return
    }

    const mousePointTo = {
      x: (pointer.x - latest.pos.x) / oldScale,
      y: (pointer.y - latest.pos.y) / oldScale,
    }

    const direction = konvaEvent.evt.deltaY > 0 ? -1 : 1
    const factor = 1.05
    const newScale = Math.min(Math.max(direction > 0 ? oldScale * factor : oldScale / factor, 0.3), 3)

    latest = {
      scale: newScale,
      pos: {
        x: pointer.x - mousePointTo.x * newScale,
        y: pointer.y - mousePointTo.y * newScale,
      },
    }
    scheduleFlush()
  }

  return { scale, stagePos, stageConfig, onWheel, onStageDragMove }
}
