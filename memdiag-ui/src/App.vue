<script setup>
import { onMounted } from 'vue'
import Layout from './components/Layout.vue'
import Toast from 'primevue/toast'
import { useConnectionStore } from './stores/connectionStore'

const connectionStore = useConnectionStore()

onMounted(() => {
  // Initialize global connection data once on app start
  connectionStore.fetchConnections()
})
</script>

<template>
  <div class="app-root">
    <Toast />
    <Layout>
      <router-view v-slot="{ Component }">
        <transition 
          name="fade" 
          mode="out-in"
        >
          <component :is="Component" />
        </transition>
      </router-view>
    </Layout>
  </div>
</template>

<style>
/* Global resets and utility classes handled by style.css + tailwind */

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* Custom scrollbar for better look */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background-color: #e2e8f0; /* bg-slate-200 */
  border-radius: 9999px;
}

::-webkit-scrollbar-thumb:hover {
  background-color: #cbd5e1; /* bg-slate-300 */
}

/* Helper classes without @apply to avoid build issues in v4 */
.card-glass {
  background-color: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 20px 25px -5px rgba(226, 232, 240, 0.5);
}

.text-gradient {
  background-clip: text;
  -webkit-background-clip: text;
  color: transparent;
  background-image: linear-gradient(to right, #4f46e5, #7c3aed);
}
</style>
