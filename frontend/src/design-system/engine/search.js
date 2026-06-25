/**
 * Search Engine - Ported from ui-ux-pro-max-skill Python core.py
 * Multi-domain BM25 search for UI/UX design recommendations
 */
import BM25 from './bm25'
import { products, styles, colors, typography, landing, uiReasoning } from '../data'

const domainConfigs = {
  product: {
    data: products,
    searchCols: ['Product Type', 'Keywords', 'Primary Style Recommendation', 'Key Considerations'],
    outputCols: ['Product Type', 'Keywords', 'Primary Style Recommendation', 'Secondary Styles', 'Landing Page Pattern', 'Dashboard Style (if applicable)', 'Color Palette Focus', 'Key Considerations'],
    maxResults: 1,
  },
  style: {
    data: styles,
    searchCols: ['Style Category', 'Keywords', 'Best For', 'Type', 'AI Prompt Keywords'],
    outputCols: ['Style Category', 'Type', 'Keywords', 'Primary Colors', 'Effects & Animation', 'Best For', 'Performance', 'Accessibility'],
    maxResults: 3,
  },
  color: {
    data: colors,
    searchCols: ['Product Type', 'Notes'],
    outputCols: ['Product Type', 'Primary', 'On Primary', 'Secondary', 'On Secondary', 'Accent', 'Background', 'Foreground', 'Card', 'Card Foreground', 'Muted', 'Muted Foreground', 'Border', 'Notes'],
    maxResults: 2,
  },
  typography: {
    data: typography,
    searchCols: ['Font Pairing Name', 'Category', 'Mood/Style Keywords', 'Best For', 'Heading Font', 'Body Font'],
    outputCols: ['Font Pairing Name', 'Category', 'Heading Font', 'Body Font', 'Mood/Style Keywords', 'Best For', 'Google Fonts URL', 'CSS Import', 'Notes'],
    maxResults: 2,
  },
  landing: {
    data: landing,
    searchCols: ['Pattern Name', 'Keywords', 'Conversion Optimization', 'Section Order'],
    outputCols: ['Pattern Name', 'Keywords', 'Section Order', 'Primary CTA Placement', 'Color Strategy', 'Conversion Optimization'],
    maxResults: 2,
  },
}

const domainKeywords = {
  color: ['color', 'palette', 'hex', '#', 'rgb', 'token', 'semantic', 'accent', 'muted', 'foreground'],
  style: ['style', 'design', 'ui', 'minimalism', 'glassmorphism', 'brutalism', 'dark mode', 'flat'],
  product: ['saas', 'ecommerce', 'fintech', 'healthcare', 'gaming', 'portfolio', 'beauty', 'pet'],
  landing: ['landing', 'page', 'cta', 'conversion', 'hero', 'testimonial', 'pricing', 'section'],
  typography: ['font', 'typography', 'serif', 'sans', 'heading font', 'body font'],
}

function detectDomain(query) {
  const q = query.toLowerCase()
  let best = 'style'
  let bestScore = 0
  for (const [domain, kws] of Object.entries(domainKeywords)) {
    const score = kws.filter(kw => q.includes(kw)).length
    if (score > bestScore) { bestScore = score; best = domain }
  }
  return best
}

function searchCSV(data, searchCols, outputCols, query, maxResults) {
  if (!data || data.length === 0) return []
  const documents = data.map(row => searchCols.map(col => row[col] || '').join(' '))
  const bm25 = BM25()
  bm25.fit(documents)
  const ranked = bm25.score(query)
  return ranked.slice(0, maxResults).map(r => {
    const row = data[r.idx]
    const result = {}
    outputCols.forEach(col => { result[col] = row[col] || '' })
    return result
  })
}

export function search(query, domain = null, maxResults = 3) {
  if (!domain) domain = detectDomain(query)
  const config = domainConfigs[domain] || domainConfigs.style
  const results = searchCSV(config.data, config.searchCols, config.outputCols, query, maxResults)
  return { domain, query, count: results.length, results }
}

export function generateDesignSystem(query, projectName = null) {
  // Step 1: Search product
  const productResult = search(query, 'product', 1)
  const productRow = productResult.results[0] || {}
  const category = productRow['Product Type'] || 'General'

  // Step 2: Find reasoning rule for category
  const reasoningRule = findReasoningRule(category)
  const stylePriority = (reasoningRule['Style_Priority'] || 'Minimalism + Flat Design').split('+').map(s => s.trim())

  // Step 3: Multi-domain search
  const styleResult = search(query, 'style', 3)
  const colorResult = search(query, 'color', 2)
  const typoResult = search(query, 'typography', 2)
  const landResult = search(query, 'landing', 2)

  // Step 4: Select best matches
  const bestStyle = selectBestMatch(styleResult.results, stylePriority)
  const bestColor = colorResult.results[0] || {}
  const bestTypo = typoResult.results[0] || {}
  const bestLand = landResult.results[0] || {}

  return {
    projectName: projectName || query,
    category,
    pattern: {
      name: bestLand['Pattern Name'] || reasoningRule['Recommended_Pattern'] || 'Hero + Features + CTA',
      sections: bestLand['Section Order'] || '',
      ctaPlacement: bestLand['Primary CTA Placement'] || 'Above fold',
      conversion: bestLand['Conversion Optimization'] || '',
    },
    style: {
      name: bestStyle['Style Category'] || stylePriority[0] || 'Minimalism',
      keywords: bestStyle['Keywords'] || '',
      effects: bestStyle['Effects & Animation'] || reasoningRule['Key_Effects'] || '',
      bestFor: bestStyle['Best For'] || '',
      performance: bestStyle['Performance'] || '',
      accessibility: bestStyle['Accessibility'] || '',
    },
    colors: {
      primary: bestColor['Primary'] || '#F97316',
      onPrimary: bestColor['On Primary'] || '#0F172A',
      secondary: bestColor['Secondary'] || '#FB923C',
      accent: bestColor['Accent'] || '#2563EB',
      background: bestColor['Background'] || '#FFF7ED',
      foreground: bestColor['Foreground'] || '#9A3412',
      card: bestColor['Card'] || '#FFFFFF',
      muted: bestColor['Muted'] || '#F1F0F0',
      border: bestColor['Border'] || '#FED7AA',
      destructive: bestColor['Destructive'] || '#DC2626',
      notes: bestColor['Notes'] || '',
    },
    typography: {
      heading: bestTypo['Heading Font'] || 'Inter',
      body: bestTypo['Body Font'] || 'Inter',
      mood: bestTypo['Mood/Style Keywords'] || reasoningRule['Typography_Mood'] || '',
      googleFontsUrl: bestTypo['Google Fonts URL'] || '',
      cssImport: bestTypo['CSS Import'] || '',
    },
    keyEffects: bestStyle['Effects & Animation'] || reasoningRule['Key_Effects'] || '',
    antiPatterns: reasoningRule['Anti_Patterns'] || '',
    checklist: [
      'No emojis as icons (use SVG: Heroicons/Lucide)',
      'cursor-pointer on all clickable elements',
      'Hover states with smooth transitions (150-300ms)',
      'Light mode: text contrast 4.5:1 minimum',
      'Focus states visible for keyboard nav',
      'prefers-reduced-motion respected',
      'Responsive: 375px, 768px, 1024px, 1440px',
    ],
  }
}

function findReasoningRule(category) {
  const cat = category.toLowerCase()
  for (const rule of uiReasoning) {
    const uiCat = (rule['UI_Category'] || '').toLowerCase()
    if (uiCat === cat || cat.includes(uiCat) || uiCat.includes(cat)) return rule
  }
  for (const rule of uiReasoning) {
    const uiCat = (rule['UI_Category'] || '').toLowerCase()
    const keywords = uiCat.replace(/[/-]/g, ' ').split(/\s+/)
    if (keywords.some(kw => cat.includes(kw))) return rule
  }
  return {}
}

function selectBestMatch(results, priorityKeywords) {
  if (!results || results.length === 0) return {}
  if (!priorityKeywords || priorityKeywords.length === 0) return results[0]

  for (const priority of priorityKeywords) {
    const p = priority.toLowerCase().trim()
    for (const result of results) {
      const styleName = (result['Style Category'] || '').toLowerCase()
      if (styleName.includes(p) || p.includes(styleName)) return result
    }
  }

  const scored = results.map(result => {
    const resultStr = JSON.stringify(result).toLowerCase()
    let score = 0
    priorityKeywords.forEach(kw => {
      const k = kw.toLowerCase().trim()
      if ((result['Style Category'] || '').toLowerCase().includes(k)) score += 10
      else if ((result['Keywords'] || '').toLowerCase().includes(k)) score += 3
      else if (resultStr.includes(k)) score += 1
    })
    return { score, result }
  })

  scored.sort((a, b) => b.score - a.score)
  return scored[0]?.score > 0 ? scored[0].result : results[0]
}
