import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import LanguageDetector from 'i18next-browser-languagedetector'

import ko from './locales/ko.json'
import en from './locales/en.json'
import zh from './locales/zh.json'
import ja from './locales/ja.json'
import vi from './locales/vi.json'
import my from './locales/my.json'

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      ko: { translation: ko },
      en: { translation: en },
      zh: { translation: zh },
      ja: { translation: ja },
      vi: { translation: vi },
      my: { translation: my },
    },
    // 미얀마어(my)는 아직 부분 번역 — 미번역 키는 영어로 폴백(그다음 한국어)
    fallbackLng: { my: ['en', 'ko'], default: ['ko'] },
    interpolation: { escapeValue: false },
    detection: {
      order: ['localStorage', 'navigator'],
      caches: ['localStorage'],
      lookupLocalStorage: 'campusflow-lang',
    },
  })

export default i18n
