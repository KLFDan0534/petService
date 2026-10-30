// 开发环境打 debug，线上把 debug 换成空函数，省得控制台被刷满
const isDev = import.meta.env.MODE === 'development'

const noop = () => {}

export const logger = {
  debug: isDev ? (...args) => { console.debug('[DEBUG]', ...args) } : noop,
  info: (...args) => { console.info('[INFO]', ...args) },
  warn: (...args) => { console.warn('[WARN]', ...args) },
  error: (...args) => { console.error('[ERROR]', ...args) },
}

export default logger
