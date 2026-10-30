// 后端存图片地址是一串用逗号拼起来的，这里拆成数组顺手去掉空项
export function parseCommaSeparatedUrls(value) {
  return String(value || '')
    .split(',')
    .map(url => url.trim())
    .filter(Boolean)
}

