import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],

  build: {
    target: 'es2015',
    minify: 'esbuild',          // terser 대비 30~90x 빠름, 압축률 차이 미미
    chunkSizeWarningLimit: 600,
    rollupOptions: {
      output: {
        manualChunks: {
          'react-vendor': ['react', 'react-dom'],
          'router':       ['react-router-dom'],
          'i18n':         ['i18next', 'react-i18next'],
          'http':         ['axios'],
        },
      },
    },
  },

  server: {
    allowedHosts: true,
    host: '0.0.0.0',
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
