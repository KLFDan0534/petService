// 端到端测试配置：真开浏览器，跑 e2e 目录下的用例
module.exports = {
  testDir: 'e2e',
  timeout: 30000,   // 单个用例最多等 30 秒
  use: {
    baseURL: 'http://localhost:5173',   // 用例里写相对路径就行
    headless: true,                     // 不弹浏览器窗口
    viewport: { width: 1280, height: 720 },
  },
};
