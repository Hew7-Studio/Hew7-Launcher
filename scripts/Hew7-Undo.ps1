<#
Undoes Hew7-Setup.ps1, putting the TV back as it was. Replace YOUR-TV-IP with your TV's IP
address (Settings > Network and Internet > your network).

  powershell -ExecutionPolicy Bypass -File Hew7-Undo.ps1 -TvIp YOUR-TV-IP
  powershell -ExecutionPolicy Bypass -File Hew7-Undo.ps1 -TvIp YOUR-TV-IP -Uninstall

What it does:
  1. Turns Google TV's home screen back on (always first, so the Home button keeps working).
  2. Puts back the screensaver and screen-off timer saved by Setup (hew7-tv-backup.json).
  3. Takes back the permissions Setup gave Hew7.
  4. -Uninstall: removes Hew7 (this also switches off Hew7 Helper).
     Without it, switch Hew7 Helper off yourself in Settings > Accessibility.

Use -DryRun to see what it would do without changing anything.
#>
param(
    [Parameter(Mandatory = $true)][string]$TvIp,
    [switch]$Uninstall,
    [switch]$DryRun
)
$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "Hew7-Common.ps1")

$script:DryRun = $DryRun
$script:Adb = Find-Adb
$script:Target = Connect-Tv $script:Adb $TvIp
$BackupFile = Get-BackupFile $script:Target
$backup = if (Test-Path $BackupFile) { Get-Content $BackupFile -Raw | ConvertFrom-Json } else { $null }

# 1. Google TV's home screen back on, unless it was already off before Setup.
Step "Turning Google TV's home screen back on"
foreach ($p in $GoogleHome) {
    if ($backup -and ($backup.googleHomeDisabled -contains $p)) { Write-Host "  ($p was already off before Setup; leaving it off)"; continue }
    Tv "pm enable $p" | Out-Null
}

# 2. Screensaver and timer.
if ($backup) {
    Step "Putting back the screensaver and screen-off timer"
    if ($backup.screensaver -and $backup.screensaver -ne "null") { Tv "settings put secure screensaver_components $($backup.screensaver)" | Out-Null }
    else { Tv "settings delete secure screensaver_components" | Out-Null }
    if ($backup.screenOffTimeout -match '^\d+$') { Tv "settings put system screen_off_timeout $($backup.screenOffTimeout)" | Out-Null }
} else {
    Step "No backup from Setup was found: choose the screensaver and its timer in the TV's settings"
    Tv "settings delete secure screensaver_components" | Out-Null
}

# 3. Permissions (removed anyway if Hew7 is uninstalled).
if (-not $Uninstall) {
    Step "Taking back Hew7's extra permissions"
    Tv "pm revoke $Hew7 android.permission.DUMP" | Out-Null
    Tv "pm revoke $Hew7 android.permission.PACKAGE_USAGE_STATS" | Out-Null
    Tv "appops set $Hew7 GET_USAGE_STATS default" | Out-Null
    Tv "appops set $Hew7 WRITE_SETTINGS default" | Out-Null
    Tv "pm revoke $Hew7 android.permission.ACCESS_FINE_LOCATION" | Out-Null
}

# 4. Uninstall.
if ($Uninstall) {
    Step "Uninstalling Hew7"
    if ($DryRun) { Write-Host "  [dry run] adb uninstall $Hew7" } else { & $script:Adb -s $script:Target uninstall $Hew7 | Write-Host }
}

Tv "input keyevent KEYCODE_HOME" | Out-Null
Write-Host ""
Write-Host "Done."
if (-not $Uninstall) { Write-Host "Hew7 is still installed: switch 'Hew7 Helper' off in Settings > Accessibility, and uninstall Hew7 from Settings > Apps if you like." }
