const isDev = import.meta.env.MODE === 'development'

const noop = () => {}

export const logger = {
  debug: isDev ? (...args) => { console.debug('[DEBUG]', ...args) } : noop,
  info: (...args) => { console.info('[INFO]', ...args) },
  warn: (...args) => { console.warn('[WARN]', ...args) },
  error: (...args) => { console.error('[ERROR]', ...args) },
}

export default logger
