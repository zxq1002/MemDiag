<template>
  <div id="app">
    <header class="header">
      <h1>MemDiag</h1>
      <p class="subtitle">JVM Memory Diagnosis Tool</p>
    </header>
    <nav class="nav">
      <router-link to="/">Dashboard</router-link>
      <router-link to="/histogram">Heap Histogram</router-link>
      <router-link to="/diagnose">Diagnosis</router-link>
      <router-link to="/threads">Threads</router-link>
      <router-link to="/nmt">NMT</router-link>
    </nav>
    <main class="main">
      <router-view />
    </main>
  </div>
</template>

<script>
export default {
  name: 'App'
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
  background: #f5f7fa;
  color: #333;
}

#app {
  min-height: 100vh;
}

.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 2rem;
  text-align: center;
}

.header h1 {
  font-size: 2.5rem;
  margin-bottom: 0.5rem;
}

.subtitle {
  opacity: 0.9;
  font-size: 1.1rem;
}

.nav {
  background: white;
  padding: 1rem 2rem;
  display: flex;
  gap: 2rem;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.nav a {
  text-decoration: none;
  color: #667eea;
  font-weight: 500;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  transition: all 0.2s;
}

.nav a:hover,
.nav a.router-link-exact-active {
  background: #667eea;
  color: white;
}

.main {
  padding: 2rem;
  max-width: 1400px;
  margin: 0 auto;
}
</style>
