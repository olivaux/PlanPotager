<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import logo from '../assets/logo.png'
import { usePageTitleState } from '../composables/usePageTitle.js'

const route = useRoute()
const dynamicTitle = usePageTitleState()

const pageTitle = computed(() => dynamicTitle.value ?? route.meta.title ?? '')
</script>

<template>
  <header class="app-header">
    <RouterLink :to="{ name: 'garden-list' }" class="app-header-logo">
      <img :src="logo" alt="PlanPotager" />
    </RouterLink>

    <span class="app-header-title">{{ pageTitle }}</span>

    <RouterLink :to="{ name: 'profile' }" class="btn app-header-account">Mon compte</RouterLink>
  </header>
</template>

<style scoped>
.app-header {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid var(--border);
}

.app-header-logo {
  display: inline-flex;
  align-items: center;
  justify-self: start;
}

.app-header-logo img {
  height: 60px;
  display: block;
}

.app-header-title {
  font-size: 1.25rem;
  font-weight: 600;
  text-align: center;
}

.app-header-account {
  justify-self: end;
}
</style>
