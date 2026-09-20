import { ref, watchEffect, onUnmounted, toValue } from 'vue'

// Titre dynamique affiché dans l'entête (prioritaire sur meta.title de la route)
const dynamicTitle = ref(null)

export function usePageTitleState() {
  return dynamicTitle
}

// À appeler depuis une vue dont le titre dépend de données chargées (ex: nom du potager).
// Accepte une valeur, une ref ou un getter ; une valeur vide retombe sur meta.title.
export function usePageTitle(title) {
  watchEffect(() => {
    dynamicTitle.value = toValue(title) || null
  })
  onUnmounted(() => {
    dynamicTitle.value = null
  })
}
