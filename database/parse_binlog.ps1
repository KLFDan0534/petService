param(
    [string]$BinlogDir = "D:\code\ideaProject\petService\database",
    [string]$OutputFile = "D:\code\ideaProject\petService\database\restore_dml_only.sql",
    [string]$StopDatetime = "2026-06-28 00:33:38",
    [int[]]$Files = @(2..15)
)

# Step 1: Get column mapping from current database
Write-Host "=== Step 1: Getting column mapping from information_schema ==="
$env:Path = "D:\Other\MySQL\mysql-9.3.0-winx64\bin;$env:Path"

$colMap = @{}
$result = mysql -h localhost -P 3308 -u root -p1234 --default-character-set=utf8mb4 -N -e "
    SELECT TABLE_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY ORDINAL_POSITION SEPARATOR '|'),
           GROUP_CONCAT(ORDINAL_POSITION ORDER BY ORDINAL_POSITION SEPARATOR '|')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'pet_service'
    GROUP BY TABLE_NAME
    ORDER BY TABLE_NAME" 2>&1 | Where-Object { $_ -notmatch "Warning|password|insecure" }

foreach ($line in $result) {
    $parts = $line -split "`t"
    if ($parts.Count -ge 3) {
        $tbl = $parts[0]
        $cols = $parts[1] -split "\|"
        $positions = $parts[2] -split "\|"
        $colMap[$tbl] = @{}
        for ($i = 0; $i -lt $cols.Count; $i++) {
            $colMap[$tbl][($i+1).ToString()] = $cols[$i]
        }
    }
}
Write-Host "Loaded column mapping for $($colMap.Count) tables"

# Step 2: Generate mysqlbinlog verbose output
Write-Host "=== Step 2: Generating mysqlbinlog verbose output ==="
$tempFile = [System.IO.Path]::GetTempFileName()
$binlogFiles = $Files | ForEach-Object { "$BinlogDir\binlog.$($_.ToString('000000'))" }

$proc = Start-Process -FilePath "D:\Other\MySQL\mysql-9.3.0-winx64\bin\mysqlbinlog.exe" -ArgumentList @(
    "--no-defaults",
    "--base64-output=DECODE-ROWS",
    "-v",
    "--stop-datetime=$StopDatetime"
) + $binlogFiles -NoNewWindow -RedirectStandardOutput $tempFile -Wait -PassThru

Write-Host "mysqlbinlog exit code: $($proc.ExitCode)"
Write-Host "Output file size: $((Get-Item $tempFile).Length) bytes"

# Step 3: Parse the output and generate clean DML SQL
Write-Host "=== Step 3: Parsing and generating DML SQL ==="
$reader = [System.IO.StreamReader]::new($tempFile)
$writer = [System.IO.StreamWriter]::new($OutputFile)

$writer.WriteLine("-- ============================================================")
$writer.WriteLine("-- PetService DML-Only Recovery Script")
$writer.WriteLine("-- Generated from binlog.000002 ~ binlog.000015")
$writer.WriteLine("-- Stop before: $StopDatetime")
$writer.WriteLine("-- Contains only: INSERT / UPDATE / DELETE")
$writer.WriteLine("-- ============================================================")
$writer.WriteLine("")
$writer.WriteLine("SET NAMES utf8mb4;")
$writer.WriteLine("SET FOREIGN_KEY_CHECKS = 0;")
$writer.WriteLine("")

$currentEvent = $null  # "INSERT", "UPDATE", "DELETE"
$currentTable = $null
$currentWhere = @()
$currentSet = @()
$lineNum = 0
$counters = @{"INSERT" = 0; "UPDATE" = 0; "DELETE" = 0}
$tables = @{}

while (!$reader.EndOfStream) {
    $line = $reader.ReadLine()
    $lineNum++

    # Detect new event
    if ($line -match "^### INSERT INTO `\`(.+?)\`\.`\`(.+?)\`\``") {
        # Emit previous event
        Write-DmlEvent $writer $currentEvent $currentTable $currentWhere $currentSet $colMap $counters $tables
        $currentEvent = "INSERT"
        $currentTable = $matches[2]
        $currentWhere = @()
        $currentSet = @()
    }
    elseif ($line -match "^### UPDATE `\`(.+?)\`\.`\`(.+?)\`\``") {
        Write-DmlEvent $writer $currentEvent $currentTable $currentWhere $currentSet $colMap $counters $tables
        $currentEvent = "UPDATE"
        $currentTable = $matches[2]
        $currentWhere = @()
        $currentSet = @()
    }
    elseif ($line -match "^### DELETE FROM `\`(.+?)\`\.`\`(.+?)\`\``") {
        Write-DmlEvent $writer $currentEvent $currentTable $currentWhere $currentSet $colMap $counters $tables
        $currentEvent = "DELETE"
        $currentTable = $matches[2]
        $currentWhere = @()
        $currentSet = @()
    }
    elseif ($line -match "^###   @(\d+)=(.+)$") {
        $pos = $matches[1]
        $val = $matches[2]
        if ($currentEvent -eq "INSERT") {
            $currentSet += @{"pos" = $pos; "val" = $val}
        }
        elseif ($currentEvent -eq "UPDATE") {
            # In UPDATE, @N appears in both WHERE and SET sections
            if ($currentWhere.Count -eq 0 -and $currentSet.Count -eq 0) {
                # First items are WHERE (old values)
                $currentWhere += @{"pos" = $pos; "val" = $val}
            } else {
                $currentSet += @{"pos" = $pos; "val" = $val}
            }
        }
        elseif ($currentEvent -eq "DELETE") {
            $currentWhere += @{"pos" = $pos; "val" = $val}
        }
    }
    elseif ($line -match "^### SET$" -and $currentEvent -eq "UPDATE") {
        # Separator between WHERE and SET in UPDATE
        # Do nothing, just switch mode
    }
    elseif ($line -match "^### WHERE$") {
        # Start of WHERE section
    }
}

# Emit last event
Write-DmlEvent $writer $currentEvent $currentTable $currentWhere $currentSet $colMap $counters $tables

$writer.WriteLine("")
$writer.WriteLine("SET FOREIGN_KEY_CHECKS = 1;")
$writer.WriteLine("")
$writer.WriteLine("-- ============================================================")
$writer.WriteLine("-- Recovery Complete")
$writer.WriteLine("-- ============================================================")

$reader.Close()
$writer.Close()

Write-Host "=== Step 4: Summary ==="
Write-Host "Total INSERT: $($counters['INSERT'])"
Write-Host "Total UPDATE: $($counters['UPDATE'])"
Write-Host "Total DELETE: $($counters['DELETE'])"
Write-Host "Tables affected: $($tables.Keys.Count)"
Write-Host "Output: $OutputFile"

function Write-DmlEvent($writer, $event, $table, $where, $set, $colMap, $counters, $tables) {
    if (!$event -or !$table) { return }

    if ($colMap.ContainsKey($table)) {
        $cols = $colMap[$table]
    } else {
        Write-Warning "Unknown table: $table"
        return
    }

    $tables[$table] = $true

    if ($event -eq "INSERT") {
        # Build column names and values
        $colNames = @()
        $colValues = @()
        foreach ($item in $set) {
            $name = $cols[$item.pos]
            $val = Format-Value $item.val
            $colNames += $name
            $colValues += $val
        }
        $writer.WriteLine("INSERT INTO `$table` ($($colNames -join ', ')) VALUES ($($colValues -join ', '));")
        $counters['INSERT']++
    }
    elseif ($event -eq "UPDATE") {
        # Build SET clause from new values
        $setParts = @()
        $whereParts = @()
        foreach ($item in $set) {
            $name = $cols[$item.pos]
            if ($name) {
                $setParts += "$name = $(Format-Value $item.val)"
            }
        }
        foreach ($item in $where) {
            $name = $cols[$item.pos]
            if ($name) {
                $whereParts += "$name = $(Format-Value $item.val)"
            }
        }
        if ($setParts.Count -gt 0 -and $whereParts.Count -gt 0) {
            $writer.WriteLine("UPDATE `$table` SET $($setParts -join ', ') WHERE $($whereParts -join ' AND ');")
            $counters['UPDATE']++
        }
    }
    elseif ($event -eq "DELETE") {
        $whereParts = @()
        foreach ($item in $where) {
            $name = $cols[$item.pos]
            if ($name) {
                $whereParts += "$name = $(Format-Value $item.val)"
            }
        }
        if ($whereParts.Count -gt 0) {
            $writer.WriteLine("DELETE FROM `$table` WHERE $($whereParts -join ' AND ');")
            $counters['DELETE']++
        }
    }
}

function Format-Value($val) {
    if ($val -eq "NULL" -or $val -eq "null") { return "NULL" }
    if ($val -match "^\d+$" -or $val -match "^\d+\.\d+$" -or $val -match "^-?\d+") {
        # Check if it's really a number or a string that looks like one
        return $val
    }
    # String - escape single quotes and wrap
    $escaped = $val -replace "'", "''"
    return "'$escaped'"
}
