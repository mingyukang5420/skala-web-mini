import { createApp } from 'vue'
import './style.css'
import '@mdi/font/css/materialdesignicons.css'
import 'vuetify/styles'
import { createVuetify } from 'vuetify'
import App from './App.vue'
import router from './router'

const kokbaejeongLight = {
  dark: false,
  colors: {
    background: '#F4F7FB',
    surface: '#FFFFFF',
    primary: '#1B2A4A',
    'primary-darken-1': '#101A30',
    secondary: '#2F8FE8',
    accent: '#2F8FE8',
    success: '#1A7F37',
    warning: '#B45309',
    error: '#D1293D',
    info: '#2F8FE8',
  },
}

const kokbaejeongDark = {
  dark: true,
  colors: {
    background: '#0F172A',
    surface: '#16213E',
    primary: '#3B82F6',
    'primary-darken-1': '#1B2A4A',
    secondary: '#60A5FA',
    accent: '#60A5FA',
    success: '#4ADE80',
    warning: '#FBBF24',
    error: '#FF6B6B',
    info: '#60A5FA',
  },
}

const vuetify = createVuetify({
  theme: {
    defaultTheme: 'kokbaejeongLight',
    themes: { kokbaejeongLight, kokbaejeongDark },
  },
  defaults: {
    VBtn: { rounded: 'lg' },
    VCard: { rounded: 'lg' },
    VTextField: { variant: 'outlined', density: 'comfortable' },
    VSelect: { variant: 'outlined', density: 'comfortable' },
  },
})

createApp(App).use(router).use(vuetify).mount('#app')
