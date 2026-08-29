# 组装自包含交接文档（.NET IO 版）
$ErrorActionPreference = 'Stop'
$frontend = 'd:\code\ideaProject\petService\frontend'
$handoff = 'd:\code\ideaProject\petService\frontend-handoff'
$outFile = Join-Path $handoff '宠物寄养平台-用户端页面适配交接文档.md'
$header = Join-Path $handoff '_header.md'
$enc = [System.Text.Encoding]::UTF8
$utf8bom = New-Object System.Text.UTF8Encoding($true)
$fenceJs = '```js'
$fenceVue = '```vue'
$fenceEnd = '```'

function Read-Src([string]$rel) {
  $p = Join-Path $frontend $rel
  if (-not (Test-Path -LiteralPath $p)) { throw "MISSING: $p" }
  return ([System.IO.File]::ReadAllText($p, $enc)).TrimEnd()
}

$dep = [System.Text.StringBuilder]::new()
function Dep([string]$title, [string]$rel, [string]$lang) {
  [void]$dep.AppendLine($title)
  if ($lang -eq 'js') { [void]$dep.AppendLine($fenceJs) } else { [void]$dep.AppendLine($fenceVue) }
  [void]$dep.AppendLine((Read-Src $rel))
  [void]$dep.AppendLine($fenceEnd)
  [void]$dep.AppendLine('')
}

[void]$dep.AppendLine('### 3.1 工具函数（src/utils）')
Dep '#### `utils/request.js` — Axios 请求封装（统一 /api 前缀、鉴权头、错误处理）' 'src\utils\request.js' 'js'
Dep '#### `utils/safeRedirect.js` — 安全跳转（白名单校验，防开放重定向）' 'src\utils\safeRedirect.js' 'js'
Dep '#### `utils/fileUrls.js` — 文件 URL 拼接' 'src\utils\fileUrls.js' 'js'
Dep '#### `utils/orderPaymentTimeout.js` — 订单支付超时逻辑' 'src\utils\orderPaymentTimeout.js' 'js'
Dep '#### `utils/profileRequirements.js` — 个人资料校验规则' 'src\utils\profileRequirements.js' 'js'

[void]$dep.AppendLine('### 3.2 服务层（src/services）')
$svcFiles = @(
  'favoriteService.js','fileService.js','keeperService.js','merchantService.js',
  'orderService.js','petService.js','ratingService.js'
)
foreach ($s in $svcFiles) {
  Dep ('#### `services/' + $s + '`') ('src\services\' + $s) 'js'
}

[void]$dep.AppendLine('### 3.3 常量（src/constants）')
Dep '#### `constants/statusMaps.js` — 各类状态映射 / 徽章 / 文案' 'src\constants\statusMaps.js' 'js'
Dep '#### `constants/favorite.js` — 收藏目标类型映射' 'src\constants\favorite.js' 'js'

[void]$dep.AppendLine('### 3.4 状态管理（src/stores, Pinia）')
Dep '#### `stores/app.js` — 应用级状态（主题、toast、全局）' 'src\stores\app.js' 'js'
Dep '#### `stores/auth.js` — 登录态 / 角色 / 用户信息' 'src\stores\auth.js' 'js'
Dep '#### `stores/notification.js` — 消息通知未读数' 'src\stores\notification.js' 'js'

[void]$dep.AppendLine('### 3.5 共享组件（src/components）')
$comps = @(
  'common\MediaWithFallback.vue','common\MessageIndicator.vue','common\ThemeToggle.vue',
  'common\AmapAddressPicker.vue','common\DataTable.vue','common\EmptyState.vue','common\LoadingSpinner.vue',
  'pet\PetFormDialog.vue','pet\PetList.vue',
  'keeper\KeeperAttendancePanel.vue','keeper\KeeperCertTab.vue','keeper\KeeperDailyTab.vue',
  'keeper\KeeperOrdersTab.vue','keeper\KeeperProfileTab.vue'
)
foreach ($c in $comps) {
  Dep ('#### `components/' + $c + '`') ('src\components\' + $c) 'vue'
}

# ---------- 4. API 接口层 ----------
$apiSb = [System.Text.StringBuilder]::new()
$apiFiles = Get-ChildItem (Join-Path $frontend 'src\api') -Filter '*.js' | Sort-Object Name
$ai = 0
foreach ($f in $apiFiles) {
  $ai++
  [void]$apiSb.AppendLine(('### 4.' + $ai + ' `api/' + $f.Name + '`'))
  [void]$apiSb.AppendLine($fenceJs)
  [void]$apiSb.AppendLine(([System.IO.File]::ReadAllText($f.FullName, $enc)).TrimEnd())
  [void]$apiSb.AppendLine($fenceEnd)
  [void]$apiSb.AppendLine('')
}

# ---------- 5. 示例页 ----------
$exSb = [System.Text.StringBuilder]::new()
$exOrder = @('Dashboard.vue','Services.vue','ServiceDetail.vue','Orders.vue','Profile.vue')
$ei = 0
foreach ($name in $exOrder) {
  $ei++
  [void]$exSb.AppendLine(('### 5.' + $ei + ' `views/user/' + $name + '`（示例，目标风格）'))
  [void]$exSb.AppendLine($fenceVue)
  [void]$exSb.AppendLine((Read-Src ('src\views\user\' + $name)))
  [void]$exSb.AppendLine($fenceEnd)
  [void]$exSb.AppendLine('')
}

# ---------- 6. 待适配页面 ----------
$pgSb = [System.Text.StringBuilder]::new()
$pages = Get-ChildItem (Join-Path $frontend 'src\views\user') -Filter '*.vue' | Where-Object { $_.Name -notin $exOrder } | Sort-Object Name
$idx = 0
foreach ($p in $pages) {
  $idx++
  $content = [System.IO.File]::ReadAllText($p.FullName, $enc)
  $apiDeps = [regex]::Matches($content, 'from\s+['']@/api/([^'' /]+)') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique
  $svcDeps = [regex]::Matches($content, 'from\s+['']@/services/([^'' /]+)') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique
  $compDeps = [regex]::Matches($content, 'from\s+['']@/components/([^'' ]+)') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique
  [void]$pgSb.AppendLine(('### 6.' + $idx + ' `views/user/' + $p.Name + '`'))
  [void]$pgSb.AppendLine('')
  [void]$pgSb.AppendLine('- **API 依赖**：' + ($(if ($apiDeps.Count) { $apiDeps -join ', ' } else { '（无直接依赖）' })))
  [void]$pgSb.AppendLine('- **服务层依赖**：' + ($(if ($svcDeps.Count) { $svcDeps -join ', ' } else { '（无直接依赖）' })))
  [void]$pgSb.AppendLine('- **共享组件依赖**：' + ($(if ($compDeps.Count) { $compDeps -join ', ' } else { '（无）' })))
  [void]$pgSb.AppendLine('- **改造要点**：按第二部分设计令牌与第五部分示例风格，重写模板结构与 `<style scoped>` 样式，保留 `<script setup>` 全部逻辑与字段。')
  [void]$pgSb.AppendLine('')
  [void]$pgSb.AppendLine($fenceVue)
  [void]$pgSb.AppendLine($content.TrimEnd())
  [void]$pgSb.AppendLine($fenceEnd)
  [void]$pgSb.AppendLine('')
}

# ---------- 组装 ----------
$headerText = [System.IO.File]::ReadAllText($header, $enc)
$headerText = $headerText.Replace('__DESIGN_TOKENS__', (Read-Src 'src\assets\css\design-tokens.css'))
$headerText = $headerText.Replace('__DEPENDENCIES__', $dep.ToString().TrimEnd())
$headerText = $headerText.Replace('__API_MODULES__', $apiSb.ToString().TrimEnd())
$headerText = $headerText.Replace('__EXAMPLES__', $exSb.ToString().TrimEnd())
$headerText = $headerText.Replace('__PAGES__', $pgSb.ToString().TrimEnd())

[System.IO.File]::WriteAllText($outFile, $headerText, $utf8bom)

$lines = ([System.IO.File]::ReadAllText($outFile, $enc) -split "`n").Count
$size = (Get-Item $outFile).Length
Write-Output ('OK: ' + $outFile)
Write-Output ('lines=' + $lines + ' size=' + $size + ' bytes')
