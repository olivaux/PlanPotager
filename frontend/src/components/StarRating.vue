<script setup>
import { computed } from 'vue'

// Note affichee en etoiles, par demi-etoile : `value` va de 0 a `max` (5 par defaut).
const props = defineProps({
  value: { type: Number, required: true },
  max: { type: Number, default: 5 },
})

const STAR_PATH = 'M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z'

const rounded = computed(() => Math.round(Math.min(Math.max(props.value, 0), props.max) * 2) / 2)

// Part remplie (en %) de chaque etoile : 0, 50 ou 100.
const fills = computed(() =>
  Array.from({ length: props.max }, (_, i) => Math.min(Math.max(rounded.value - i, 0), 1) * 100),
)

const label = computed(() => `${String(rounded.value).replace('.', ',')} étoiles sur ${props.max}`)
</script>

<template>
  <span class="stars" role="img" :aria-label="label" :title="label">
    <span v-for="(fill, index) in fills" :key="index" class="star">
      <svg viewBox="0 0 24 24" class="star-icon star-empty"><path :d="STAR_PATH" /></svg>
      <span class="star-fill" :style="{ width: `${fill}%` }">
        <svg viewBox="0 0 24 24" class="star-icon star-full"><path :d="STAR_PATH" /></svg>
      </span>
    </span>
  </span>
</template>

<style scoped>
.stars {
  display: inline-flex;
  gap: 2px;
}

.star {
  position: relative;
  display: inline-block;
  width: 24px;
  height: 24px;
}

.star-icon {
  display: block;
  width: 24px;
  height: 24px;
}

.star-empty path {
  fill: rgba(255, 255, 255, 0.6);
  stroke: #9a9a9a;
  stroke-width: 1;
  stroke-linejoin: round;
}

.star-fill {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  overflow: hidden;
}

.star-full path {
  fill: #f5b301;
  stroke: #c98a00;
  stroke-width: 1;
  stroke-linejoin: round;
}
</style>
