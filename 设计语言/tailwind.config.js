/**
 * Wraps a CSS custom property so Tailwind's `/NN` opacity modifiers keep
 * working. The tokens are authored as hex in index.css, so alpha is applied
 * with `color-mix()` rather than an rgb channel split — this means
 * `bg-ink/50`, `border-critical/35` and `bg-brand/[0.06]` all resolve.
 */
const withAlpha =
  (variable) =>
  ({ opacityValue } = {content: [
  './index.html',
  './src/**/*.{js,ts,jsx,tsx}'
],}) => {
    if (opacityValue === undefined) return `var(${variable})`
    const percentage = Number(opacityValue) * 100
    return `color-mix(in srgb, var(${variable}) ${percentage}%, transparent)`
  }

export default {
  theme: {
    extend: {
      colors: {
        'primary-visual-reference-src-views-user-services-vue': 'var(--primary-visual-reference-src-views-user-services-vue)',
        'ref-canvas': 'var(--ref-canvas)',
        'ref-surface': 'var(--ref-surface)',
        'ref-sand': 'var(--ref-sand)',
        'ref-cream': 'var(--ref-cream)',
        'ref-line': 'var(--ref-line)',
        'ref-ink': 'var(--ref-ink)',
        'ref-ink-soft': 'var(--ref-ink-soft)',
        'ref-muted': 'var(--ref-muted)',
        'ref-brand': 'var(--ref-brand)',
        'ref-brand-deep': 'var(--ref-brand-deep)',
        'color-success': 'var(--color-success)',
        'color-warning': 'var(--color-warning)',
        'color-info': 'var(--color-info)',
        'color-destructive': 'var(--color-destructive)',
        'ref-focus-ring-recipe-proposed-token': 'var(--ref-focus-ring-recipe-proposed-token)',
        'ledger-governing-removal-rules': 'var(--ledger-governing-removal-rules)',
        'color-muted-foreground': 'var(--color-muted-foreground)',
        'color-border': 'var(--color-border)',
        'color-primary': 'var(--color-primary)',
        'color-on-primary': 'var(--color-on-primary)',
        'color-secondary': 'var(--color-secondary)',
        'color-on-secondary': 'var(--color-on-secondary)',
        'color-on-accent': 'var(--color-on-accent)',
        'clay-border': 'var(--clay-border)',
        'color-danger': 'var(--color-danger)',
        'color-error': 'var(--color-error)',
        'color-on-destructive': 'var(--color-on-destructive)',
        'color-accent': 'var(--color-accent)',
        'color-background': 'var(--color-background)',
        'color-foreground': 'var(--color-foreground)',
        'color-card': 'var(--color-card)',
        'color-card-foreground': 'var(--color-card-foreground)',
        'color-muted': 'var(--color-muted)',
        'color-ring': 'var(--color-ring)',
        'el-color-primary-element-plus-bridge': 'var(--el-color-primary-element-plus-bridge)',
        'rule-services-vue-is-the-primary-visual-reference-any-value-conflicting-between-views-resolves-to-the-src-views-user-services-vue-value': 'var(--rule-services-vue-is-the-primary-visual-reference-any-value-conflicting-between-views-resolves-to-the-src-views-user-services-vue-value)',
        'rule-removal-safety-remove-a-token-only-when-confirmed-zero-source-and-zero-runtime-refs-never-remove-a-still-referenced-token-migrate-call-sites-first-src-stores-design-js-and-src-stores-app-js-are-runtime-dependencies-that-set-css-vars-in-js-verification-of-both-is-mandatory-before-any-destructive-cleanup-confirmed': 'var(--rule-removal-safety-remove-a-token-only-when-confirmed-zero-source-and-zero-runtime-refs-never-remove-a-still-referenced-token-migrate-call-sites-first-src-stores-design-js-and-src-stores-app-js-are-runtime-dependencies-that-set-css-vars-in-js-verification-of-both-is-mandatory-before-any-destructive-cleanup-confirmed)',
        'card-body-padding-applied-confirmed-services-vue-538': 'var(--card-body-padding-applied-confirmed-services-vue-538)',
        'control-height-button-button-cta-intentionally-distinct-from-chip-input-do-not-collapse-or-fix-to-44-px-confirmed-across-40-views-e-g-customer-service-apply-vue-479-revenue-center-vue-283-profile-vue-1248': 'var(--control-height-button-button-cta-intentionally-distinct-from-chip-input-do-not-collapse-or-fix-to-44-px-confirmed-across-40-views-e-g-customer-service-apply-vue-479-revenue-center-vue-283-profile-vue-1248)',
        'control-height-chip-input-chip-input-intentionally-distinct-from-button-confirmed-services-vue-396-chip-446-search-input-487-reset': 'var(--control-height-chip-input-chip-input-intentionally-distinct-from-button-confirmed-services-vue-396-chip-446-search-input-487-reset)',
        'shadow-1-px-border-is-the-primary-structural-device-cards-have-no-shadow-at-rest-confirmed-services-vue-517-519': 'var(--shadow-1-px-border-is-the-primary-structural-device-cards-have-no-shadow-at-rest-confirmed-services-vue-517-519)',
        'radius-card-cards-empty-states-confirmed-services-vue-518-599-order-card-vue-268-order-detail-view-vue-502-506-638': 'var(--radius-card-cards-empty-states-confirmed-services-vue-518-599-order-card-vue-268-order-detail-view-vue-502-506-638)',
        'radius-control-chips-inputs-buttons-confirmed-services-vue-398-449-489-582': 'var(--radius-control-chips-inputs-buttons-confirmed-services-vue-398-449-489-582)',
        'duration-fast-color-border-transitions-confirmed-services-vue-405-453': 'var(--duration-fast-color-border-transitions-confirmed-services-vue-405-453)',
        'shadow-paired-with-shadow-card-hover-confirmed-services-vue-525': 'var(--shadow-paired-with-shadow-card-hover-confirmed-services-vue-525)',
        'ledger-radius-lg-declared-16-px-still-actively-used-keep-user-layout-vue-598-in-src-plus-frontend-handoff-pages-ai-vue-296-and-ai-chat-vue-240-near-duplicate-of-applied-radius-card-18-px-confirmed': 'var(--ledger-radius-lg-declared-16-px-still-actively-used-keep-user-layout-vue-598-in-src-plus-frontend-handoff-pages-ai-vue-296-and-ai-chat-vue-240-near-duplicate-of-applied-radius-card-18-px-confirmed)',
        'ledger-clay-border-still-actively-used-keep-app-css-577-and-640-equivalent-to-the-applied-1-px-hairline-confirmed': 'var(--ledger-clay-border-still-actively-used-keep-app-css-577-and-640-equivalent-to-the-applied-1-px-hairline-confirmed)',
        'ref-critical': 'var(--ref-critical)',
        'ref-caution': 'var(--ref-caution)',
        'ref-moss': 'var(--ref-moss)',
        'ref-positive': 'var(--ref-positive)',
        'ref-informative': 'var(--ref-informative)',
        'ref-radius-card': 'var(--ref-radius-card)',
        'clay-shadow': 'var(--clay-shadow)',
        'clay-radius': 'var(--clay-radius)',
        canvas: 'withAlpha(\'--ref-canvas\')',
        surface: 'withAlpha(\'--ref-surface\')',
        sand: 'withAlpha(\'--ref-sand\')',
        cream: 'withAlpha(\'--ref-cream\')',
        line: 'withAlpha(\'--ref-line\')',
        ink: 'withAlpha(\'--ref-ink\')',
        'ink-soft': 'withAlpha(\'--ref-ink-soft\')',
        muted: 'withAlpha(\'--ref-muted\')',
        brand: 'withAlpha(\'--ref-brand\')',
        'brand-deep': 'withAlpha(\'--ref-brand-deep\')',
        critical: 'withAlpha(\'--ref-critical\')',
        caution: 'withAlpha(\'--ref-caution\')',
        positive: 'withAlpha(\'--ref-positive\')',
        informative: 'withAlpha(\'--ref-informative\')',
        moss: 'withAlpha(\'--ref-moss\')'
      },
      borderRadius: {
        tile: 'var(--ref-radius-tile)',
        control: 'var(--ref-radius-control)',
        card: 'var(--ref-radius-card)',
        frame: 'var(--ref-radius-frame)'
      },
      fontSize: {
        'display-xl': ['clamp(30px, 4.6vw, 46px)', { lineHeight: '1.06', letterSpacing: '-0.02em' }],
        'display-lg': ['clamp(26px, 3.6vw, 36px)', { lineHeight: '1.12', letterSpacing: '-0.02em' }],
        'display-md': ['clamp(23px, 3vw, 30px)', { lineHeight: '1.18', letterSpacing: '-0.01em' }],
        'display-sm': ['clamp(20px, 2.6vw, 26px)', { lineHeight: '1.22', letterSpacing: '-0.01em' }],
        'display-xs': ['18px', { lineHeight: '1.3', letterSpacing: '-0.01em' }],
        control: ['13px', { lineHeight: '1.25' }],
        meta: ['12.5px', { lineHeight: '1.5' }],
        label: ['10px', { lineHeight: '1.2' }],
        eyebrow: ['9.5px', { lineHeight: '1', letterSpacing: '0.28em' }]
      },
      letterSpacing: {
        editorial: '-0.01em',
        label: '0.14em'
      },
      spacing: {
        gutter: 'var(--ref-gutter)',
        band: 'var(--ref-band)'
      },
      maxWidth: {
        shell: 'var(--ref-shell-max)'
      },
      transitionDuration: {
        fast: 'var(--ref-duration-fast)',
        normal: 'var(--ref-duration-normal)',
        slow: 'var(--ref-duration-slow)'
      },
      transitionTimingFunction: {
        editorial: 'var(--ref-ease-editorial)'
      },
      keyframes: {
        'ref-fade': {
          from: {
            opacity: 0
          },
          to: {
            opacity: 1
          }
        },
        'ref-pop': {
          from: {
            opacity: 0,
            transform: 'scale(0.98)'
          },
          to: {
            opacity: 1,
            transform: 'scale(1)'
          }
        }
      },
      animation: {
        fade: 'ref-fade var(--ref-duration-normal) var(--ref-ease-editorial) both',
        pop: 'ref-pop var(--ref-duration-normal) var(--ref-ease-editorial) both'
      },
      boxShadow: {
        '1-px-border-is-the-primary-structural-device-cards-have-no-shadow-at-rest-confirmed-services-vue-517-519': 'var(--shadow-1-px-border-is-the-primary-structural-device-cards-have-no-shadow-at-rest-confirmed-services-vue-517-519)',
        'the-only-elevation-hover-only-standardised-on-the-services-vue-60-ink-mix-confirmed-services-vue-524': 'var(--shadow-the-only-elevation-hover-only-standardised-on-the-services-vue-60-ink-mix-confirmed-services-vue-524)',
        'confirmed-services-vue-464-uses-12-12-22-range-observed-elsewhere-standardise-on-12': 'var(--shadow-confirmed-services-vue-464-uses-12-12-22-range-observed-elsewhere-standardise-on-12)',
        'confirmed-order-detail-view-vue-638': 'var(--shadow-confirmed-order-detail-view-vue-638)',
        'paired-with-shadow-card-hover-confirmed-services-vue-525': 'var(--shadow-paired-with-shadow-card-hover-confirmed-services-vue-525)',
        light: 'var(--shadow-light)',
        'declaration-only-design-tokens-css-67': 'var(--shadow-declaration-only-design-tokens-css-67)',
        'service-grid-vue-138-144-pet-list-vue-164-170-banner-carousel-vue-140-amap-address-picker-vue-529-588-user-layout-vue-599-admin-coupons-vue-698-admin-dashboard-vue-158-admin-statistics-vue-200-merchant-customer-service-vue-154': 'var(--shadow-service-grid-vue-138-144-pet-list-vue-164-170-banner-carousel-vue-140-amap-address-picker-vue-529-588-user-layout-vue-599-admin-coupons-vue-698-admin-dashboard-vue-158-admin-statistics-vue-200-merchant-customer-service-vue-154)',
        sm: 'var(--shadow-sm)',
        md: 'var(--shadow-md)',
        lg: 'var(--shadow-lg)',
        xl: 'var(--shadow-xl)',
        inner: 'var(--shadow-inner)',
        shadow: 'var(--shadow-shadow)',
        lift: 'var(--ref-shadow-lift)',
        focus: 'var(--ref-shadow-focus)'
      },
      fontFamily: {
        heading: ['"Noto Serif SC"', '"Songti SC"', 'STSong', 'SimSun', 'Georgia', '"Times New Roman"', 'serif'],
        display: ['"Noto Serif SC"', '"Songti SC"', 'STSong', 'SimSun', 'Georgia', '"Times New Roman"', 'serif'],
        body: ['Inter', '"Segoe UI"', 'system-ui', 'sans-serif'],
        mono: ['ui-monospace', 'SFMono-Regular', 'Consolas', 'monospace']
      }
    }
  }
}