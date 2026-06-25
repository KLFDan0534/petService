/**
 * BM25 Search Engine - Ported from ui-ux-pro-max-skill Python core.py
 * UI/UX Pro Max Design Intelligence
 */
const BM25 = (k1 = 1.5, b = 0.75) => {
  let corpus = []
  let docLengths = []
  let avgdl = 0
  let idf = {}
  let docFreqs = {}
  let N = 0

  const tokenize = (text) => {
    const t = String(text).toLowerCase().replace(/[^\w\s]/g, ' ')
    return t.split(/\s+/).filter(w => w.length > 2)
  }

  const fit = (documents) => {
    corpus = documents.map(d => tokenize(d))
    N = corpus.length
    if (N === 0) return
    docLengths = corpus.map(d => d.length)
    avgdl = docLengths.reduce((a, b) => a + b, 0) / N
    docFreqs = {}
    corpus.forEach(doc => {
      const seen = new Set()
      doc.forEach(word => {
        if (!seen.has(word)) {
          docFreqs[word] = (docFreqs[word] || 0) + 1
          seen.add(word)
        }
      })
    })
    idf = {}
    for (const [word, freq] of Object.entries(docFreqs)) {
      idf[word] = Math.log((N - freq + 0.5) / (freq + 0.5) + 1)
    }
  }

  const score = (query) => {
    const queryTokens = tokenize(query)
    const scores = corpus.map((doc, idx) => {
      let score = 0
      const docLen = docLengths[idx]
      const termFreqs = {}
      doc.forEach(w => { termFreqs[w] = (termFreqs[w] || 0) + 1 })
      queryTokens.forEach(token => {
        if (idf[token] !== undefined) {
          const tf = termFreqs[token] || 0
          const idfVal = idf[token]
          const numerator = tf * (k1 + 1)
          const denominator = tf + k1 * (1 - b + b * docLen / avgdl)
          score += idfVal * numerator / denominator
        }
      })
      return { idx, score }
    })
    return scores.filter(s => s.score > 0).sort((a, b) => b.score - a.score)
  }

  return { fit, score, tokenize }
}

export default BM25
