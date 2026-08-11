/** @type {import('tailwindcss').Config} */
// Meridian 디자인 시스템 — CampusFlow(navy #00236f) + NovaClass(blue-purple #3B37CC)를
// 딥 인디고 primary(#23255E) + 라이트퍼플 "spark" accent(#A78BFA)로 통합.
// 기존 토큰 이름을 유지하고 값만 Meridian으로 매핑 → 기존 페이지가 자동 리스킨된다.
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // ── Surface ──────────────────────────────────────
        'surface':                    '#f6f7fb',
        'surface-dim':                '#e1e2f0',
        'surface-bright':             '#ffffff',
        'surface-variant':            '#e9eaf5',
        'surface-container-lowest':   '#ffffff',
        'surface-container-low':      '#f0f1f8',
        'surface-container':          '#e9eaf5',
        'surface-container-high':     '#e1e2f0',
        'surface-container-highest':  '#d4d5e8',
        'on-surface':                 '#181a2e',
        'on-surface-variant':         '#4d4f6b',
        'inverse-surface':            '#25274a',
        'inverse-on-surface':         '#f0f1f8',
        'surface-tint':               '#4B4FD6',

        // ── Primary (Deep Indigo) ────────────────────────
        'primary':                    '#23255E',
        'primary-dark':               '#171838',
        'primary-light':              '#4B4FD6',
        'on-primary':                 '#ffffff',
        'primary-container':          '#e3e3f9',
        'on-primary-container':       '#23255E',
        'inverse-primary':            '#c7c8f5',
        'primary-fixed':              '#e3e3f9',
        'primary-fixed-dim':          '#b6b8e8',
        'on-primary-fixed':           '#171838',
        'on-primary-fixed-variant':   '#2c2e70',

        // ── Secondary = Spark accent (Light Purple) ──────
        'secondary':                  '#7C5FE0',
        'on-secondary':               '#ffffff',
        'secondary-container':        '#EDE7FB',
        'on-secondary-container':     '#4C2E8C',
        'secondary-fixed':            '#A78BFA',
        'secondary-fixed-dim':        '#7C5FE0',
        'on-secondary-fixed':         '#ffffff',
        'on-secondary-fixed-variant': '#4C2E8C',

        // ── Accent (Spark) — 신규 이름 ───────────────────
        'accent':                     '#A78BFA',
        'accent-dark':                '#7C5FE0',
        'on-accent':                  '#ffffff',
        'accent-container':           '#EDE7FB',
        'on-accent-container':        '#4C2E8C',

        // ── Tertiary (indigo variant) ────────────────────
        'tertiary':                   '#2c2e70',
        'on-tertiary':                '#ffffff',
        'tertiary-container':         '#e3e3f9',
        'on-tertiary-container':      '#23255E',
        'tertiary-fixed':             '#e3e3f9',
        'tertiary-fixed-dim':         '#b6b8e8',
        'on-tertiary-fixed':          '#171838',
        'on-tertiary-fixed-variant':  '#2c2e70',

        // ── Semantic ─────────────────────────────────────
        'success':                    '#16A34A',
        'success-bg':                 '#dcfce7',
        'success-text':               '#166534',
        'warning':                    '#F59E0B',
        'warning-bg':                 '#fef3c7',
        'warning-text':               '#92400e',
        'danger':                     '#EF4444',
        'danger-bg':                  '#fee2e2',
        'danger-text':                '#b91c1c',

        // ── Error (M3 이름 유지 → danger 매핑) ────────────
        'error':                      '#EF4444',
        'on-error':                   '#ffffff',
        'error-container':            '#fee2e2',
        'on-error-container':         '#b91c1c',

        // ── Background & Outline ─────────────────────────
        'background':                 '#f6f7fb',
        'on-background':              '#181a2e',
        'outline':                    '#7b7d99',
        'outline-variant':            '#d4d5e8',
      },

      borderRadius: {
        'sm':      '0.25rem',
        DEFAULT:   '0.5rem',
        'md':      '0.75rem',
        'lg':      '1rem',
        'xl':      '1.5rem',
        '2xl':     '1.5rem',
        '3xl':     '1.5rem',
        'full':    '9999px',
      },

      spacing: {
        'base_unit':         '8px',
        'rail_width':        '80px',
        'sidebar_width':     '248px',
        'container_padding': '24px',
        'gutter':            '16px',
        'card_gap':          '24px',
      },

      fontFamily: {
        'space':   ['"Space Grotesk"', '"Noto Sans KR"', 'sans-serif'],
        'inter':   ['Inter', '"Noto Sans KR"', '"Noto Sans Myanmar"', 'sans-serif'],
        'display': ['"Space Grotesk"', '"Noto Sans KR"', 'sans-serif'],
        'body':    ['Inter', '"Noto Sans KR"', '"Noto Sans Myanmar"', 'sans-serif'],
      },
      fontSize: {
        'display-lg':  ['56px', { lineHeight: '62px', letterSpacing: '-0.25px', fontWeight: '700' }],
        'headline-md': ['28px', { lineHeight: '36px', fontWeight: '600' }],
        'title-lg':    ['22px', { lineHeight: '28px', fontWeight: '600' }],
        'body-lg':     ['16px', { lineHeight: '24px', fontWeight: '400' }],
        'body-md':     ['14px', { lineHeight: '20px', fontWeight: '400' }],
        'label-md':    ['12px', { lineHeight: '16px', fontWeight: '600' }],
      },

      // 인디고 틴트 구조 그림자 + 라이트퍼플 spark 글로우 (Meridian 시그니처)
      boxShadow: {
        'card':        '0 2px 12px rgba(35,37,94,0.10)',
        'card-md':     '0 6px 24px rgba(35,37,94,0.16)',
        'nav':         '4px 0 24px rgba(35,37,94,0.12)',
        'top':         '0 1px 8px rgba(35,37,94,0.08)',
        'spark':       '0 0 20px rgba(167,139,250,0.40)',
        'spark-hover': '0 0 28px rgba(167,139,250,0.55)',
        'lime':        '0 0 20px rgba(167,139,250,0.40)', // 하위호환 alias → spark
      },
    },
  },
  plugins: [],
}
