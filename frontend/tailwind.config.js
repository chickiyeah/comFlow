/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // ── Surface ──────────────────────────────────────
        'surface':                    '#f7f9fb',
        'surface-dim':                '#d8dadc',
        'surface-bright':             '#f7f9fb',
        'surface-variant':            '#e0e3e5',
        'surface-container-lowest':   '#ffffff',
        'surface-container-low':      '#f2f4f6',
        'surface-container':          '#eceef0',
        'surface-container-high':     '#e6e8ea',
        'surface-container-highest':  '#e0e3e5',
        'on-surface':                 '#191c1e',
        'on-surface-variant':         '#444651',
        'inverse-surface':            '#2d3133',
        'inverse-on-surface':         '#eff1f3',
        'surface-tint':               '#4059aa',

        // ── Primary (Deep Navy) ──────────────────────────
        'primary':                    '#00236f',
        'on-primary':                 '#ffffff',
        'primary-container':          '#1e3a8a',
        'on-primary-container':       '#90a8ff',
        'inverse-primary':            '#b6c4ff',
        'primary-fixed':              '#dce1ff',
        'primary-fixed-dim':          '#b6c4ff',
        'on-primary-fixed':           '#00164e',
        'on-primary-fixed-variant':   '#264191',

        // ── Secondary (Accent Lime) ──────────────────────
        'secondary':                  '#476800',
        'on-secondary':               '#ffffff',
        'secondary-container':        '#bcf063',
        'on-secondary-container':     '#4b6d00',
        'secondary-fixed':            '#bff365',
        'secondary-fixed-dim':        '#a4d64c',
        'on-secondary-fixed':         '#131f00',
        'on-secondary-fixed-variant': '#354e00',

        // ── Tertiary ─────────────────────────────────────
        'tertiary':                   '#222a3e',
        'on-tertiary':                '#ffffff',
        'tertiary-container':         '#384055',
        'on-tertiary-container':      '#a4acc5',
        'tertiary-fixed':             '#dae2fd',
        'tertiary-fixed-dim':         '#bec6e0',
        'on-tertiary-fixed':          '#131b2e',
        'on-tertiary-fixed-variant':  '#3f465c',

        // ── Error ────────────────────────────────────────
        'error':                      '#ba1a1a',
        'on-error':                   '#ffffff',
        'error-container':            '#ffdad6',
        'on-error-container':         '#93000a',

        // ── Background & Outline ─────────────────────────
        'background':                 '#f7f9fb',
        'on-background':              '#191c1e',
        'outline':                    '#757682',
        'outline-variant':            '#c5c5d3',
      },

      // ── Border Radius (DESIGN.md 스펙 그대로) ───────────
      borderRadius: {
        'sm':      '0.25rem',   // 4px
        DEFAULT:   '0.5rem',    // 8px
        'md':      '0.75rem',   // 12px
        'lg':      '1rem',      // 16px  ← 카드 기본
        'xl':      '1.5rem',    // 24px  ← 피처 카드
        '2xl':     '1.5rem',    // 호환성 유지
        '3xl':     '1.5rem',    // 호환성 유지
        'full':    '9999px',    // pill
      },

      // ── Spacing ──────────────────────────────────────────
      spacing: {
        'base_unit':         '8px',
        'rail_width':        '80px',
        'container_padding': '24px',
        'gutter':            '16px',
        'card_gap':          '24px',
      },

      // ── Typography ────────────────────────────────────────
      fontFamily: {
        'space': ['"Space Grotesk"', 'sans-serif'],
        'inter': ['Inter', 'sans-serif'],
      },
      fontSize: {
        'display-lg':  ['57px', { lineHeight: '64px',  letterSpacing: '-0.25px', fontWeight: '700' }],
        'headline-md': ['28px', { lineHeight: '36px',  fontWeight: '600' }],
        'title-lg':    ['22px', { lineHeight: '28px',  fontWeight: '500' }],
        'body-lg':     ['16px', { lineHeight: '24px',  letterSpacing: '0.5px',  fontWeight: '400' }],
        'body-md':     ['14px', { lineHeight: '20px',  letterSpacing: '0.25px', fontWeight: '400' }],
        'label-md':    ['12px', { lineHeight: '16px',  letterSpacing: '0.5px',  fontWeight: '500' }],
      },

      // ── Box Shadow (navy tint per design) ────────────────
      boxShadow: {
        'card':    '0 2px 12px rgba(30,58,138,0.08)',
        'card-md': '0 4px 24px rgba(30,58,138,0.12)',
        'nav':     '4px 0 24px rgba(30,58,138,0.10)',
        'top':     '0 1px 8px rgba(30,58,138,0.06)',
        'lime':    '0 0 20px rgba(191,243,101,0.35)',
      },
    },
  },
  plugins: [],
}
