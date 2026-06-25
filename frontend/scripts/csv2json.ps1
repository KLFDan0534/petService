$skillDir = "D:\code\ideaProject\petService\ui-ux-pro-max-skill\src\ui-ux-pro-max\data"
$outDir = "D:\code\ideaProject\petService\frontend\src\design-system\data"

$files = @("products.csv", "styles.csv", "colors.csv", "typography.csv", "landing.csv", "ui-reasoning.csv")

foreach ($file in $files) {
  $csvPath = Join-Path $skillDir $file
  if (!(Test-Path $csvPath)) { Write-Output "SKIP: $file"; continue }
  
  $jsonName = [System.IO.Path]::ChangeExtension($file, ".json")
  $jsonPath = Join-Path $outDir $jsonName
  
  $csv = Import-Csv -Path $csvPath
  $json = $csv | Select-Object -ExcludeProperty No | ConvertTo-Json -Depth 3 -Compress:$false
  
  # Replace emoji chars that break encoding
  $json = $json -replace "[\x{2705}\x{2714}]", "OK"
  $json = $json -replace "[\x{274C}]", "NO"

  [System.IO.File]::WriteAllText($jsonPath, $json, [System.Text.Encoding]::UTF8)
  $count = $csv.Count
  Write-Output ("Converted " + $file + " -> " + $jsonName + " (" + $count + " rows)")
}
