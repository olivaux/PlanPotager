import { ref, computed } from 'vue'

export function useKonvaZoomPan(stageSize) {
  const scale = ref(1.2)
  const stagePos = ref({ x: 0, y: 0 })

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
    stagePos.value = { x: stage.x(), y: stage.y() }
  }

  function onWheel(konvaEvent) {
    konvaEvent.evt.preventDefault()
    const stage = konvaEvent.target.getStage()
    const oldScale = scale.value
    const pointer = stage.getPointerPosition()
    if (!pointer) {
      return
    }

    const mousePointTo = {
      x: (pointer.x - stagePos.value.x) / oldScale,
      y: (pointer.y - stagePos.value.y) / oldScale,
    }

    const direction = konvaEvent.evt.deltaY > 0 ? -1 : 1
    const factor = 1.05
    const newScale = Math.min(Math.max(direction > 0 ? oldScale * factor : oldScale / factor, 0.3), 3)

    scale.value = newScale
    stagePos.value = {
      x: pointer.x - mousePointTo.x * newScale,
      y: pointer.y - mousePointTo.y * newScale,
    }
  }

  return { scale, stagePos, stageConfig, onWheel, onStageDragMove }
}
