<script setup>
import { ref, onMounted } from 'vue'
import { getProfile } from '../../services/profileService.js'

const profile = ref(null)
const error = ref(null)

onMounted(async () => {
  try {
    profile.value = await getProfile()
  } catch {
    error.value = "Impossible de charger le profil."
  }
})
</script>

<template>
  <div class="page page-medium">
    <h1>Mon profil</h1>

    <p v-if="error" class="error">{{ error }}</p>

    <dl v-else-if="profile">
      <dt>Email</dt>
      <dd>{{ profile.email }}</dd>

      <dt>Unité</dt>
      <dd>{{ profile.unit }}</dd>

      <dt>Langue</dt>
      <dd>{{ profile.language }}</dd>

      <dt>Fournisseur de connexion</dt>
      <dd>{{ profile.provider }}</dd>
    </dl>

    <p v-else>Chargement…</p>

    <RouterLink v-if="profile" class="btn" :to="{ name: 'profile-edit' }">Modifier le profil</RouterLink>

    <p class="credits">
      <a href="https://www.flaticon.com/fr/icones-gratuites/avatar-homme" title="avatar homme icônes" target="_blank" rel="noopener noreferrer">Avatar homme icônes créées par Smashicons - Flaticon</a>
      <br />
      <a href="https://www.flaticon.com/fr/icones-gratuites/modifier" title="modifier icônes" target="_blank" rel="noopener noreferrer">Modifier icônes créées par Karacis - Flaticon</a>
    </p>
  </div>
</template>

<style scoped>
dl {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 8px 16px;
}

dt {
  color: var(--text);
  font-weight: 500;
}

dd {
  margin: 0;
  color: var(--text-h);
}

a.btn {
  display: inline-block;
  margin-top: 16px;
}

.credits {
  margin-top: 32px;
  font-size: 0.8rem;
}
</style>
