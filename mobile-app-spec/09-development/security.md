# Security（安全要求）

## 继承的项目原则（EXISTING PRODUCT.md / 代码）

- API 内容按纯文本渲染；禁 unsafe HTML（Web 反面参考 → App 不引入富文本解释器）
- 状态/表格数据文本化；服务端 message 透传（不拼接用户输入进 UI 逻辑）

## Token 安全 [P/R]

- access/refresh 存 EncryptedDataStore；refresh 仅在 401 流程使用
- 不进日志/崩溃上报；Logcat 仅 debug 构建且脱敏
- 传输全 HTTPS；证书校验默认，禁信任用户 CA（release）

## 输入与内容

- 所有用户输入服务端校验为准；客户端校验仅 UX
- 图片上传走压缩管道，剥离 EXIF 位置 [R·隐私]
- 深链参数校验（id 合法性），防伪造跳转

## 移动端特有 [R]

- 支付确认：支付密码 + BiometricPrompt 快捷（本地解锁仍需服务端校验）
- 防截屏：支付/钱包页 `FLAG_SECURE` [R]
- 混淆 + 资源压缩；序列化模型 keep 规则
- 崩溃/性能上报（如 Crashlytics）默认脱敏，隐私政策声明

## 权限最小化

见 08-android/permissions.md：仅相机/相册/定位/通知；定位仅前台打点；不做后台追踪。
