<#
Sets up Hew7 on an Android TV / Google TV over the network. Replace YOUR-TV-IP with your TV's IP
address (Settings > Network and Internet > your network).

  powershell -ExecutionPolicy Bypass -File Hew7-Setup.ps1 -TvIp YOUR-TV-IP

What it does (everything can be undone with Hew7-Undo.ps1):
  1. Installs Hew7.apk (from this folder) if it isn't installed yet. -Reinstall installs it again.
  2. Gives Hew7 the permissions only ADB can give: memory and usage access (Running Apps),
     the screensaver timer, and location (Wi-Fi name).
  3. Makes Hew7 the screensaver.
  4. Turns off Google TV's own home screen so the Home button opens Hew7.
  5. Opens the TV's Accessibility settings: switch on "Hew7 Helper" there yourself
     (Android only lets a person do that).

It first saves the TV's current settings to hew7-tv-backup.json, so Undo can put them back.
Use -DryRun to see what it would do without changing anything.
#>
param(
    [Parameter(Mandatory = $true)][string]$TvIp,
    [string]$Apk = "",
    [switch]$Reinstall,
    [switch]$DryRun
)
$ErrorActionPreference = "Stop"
if (-not $Apk) { $Apk = Join-Path $PSScriptRoot "Hew7.apk" }
. (Join-Path $PSScriptRoot "Hew7-Common.ps1")

$script:DryRun = $DryRun
$script:Adb = Find-Adb
$script:Target = Connect-Tv $script:Adb $TvIp
Write-Host "Connected to $(& $script:Adb -s $script:Target shell getprop ro.product.model) (Android $(& $script:Adb -s $script:Target shell getprop ro.build.version.release))."

# 1. Save the TV's current settings (only the first time, so a second run doesn't overwrite the originals).
$BackupFile = Get-BackupFile $script:Target
if (-not (Test-Path $BackupFile)) {
    $already = (& $script:Adb -s $script:Target shell pm list packages -d $GoogleHome[0]) -match [regex]::Escape($GoogleHome[0])
    if ($already -and ((& $script:Adb -s $script:Target shell pm list packages $Hew7) -match [regex]::Escape($Hew7))) {
        Write-Host "  Note: Hew7 already looks set up on this TV, so the saved 'original' settings may not be the real originals."
    }
    $backup = [ordered]@{
        target              = $script:Target
        savedAt             = (Get-Date).ToString("s")
        screenOffTimeout    = (& $script:Adb -s $script:Target shell settings get system screen_off_timeout).Trim()
        screensaver         = (& $script:Adb -s $script:Target shell settings get secure screensaver_components).Trim()
        googleHomeDisabled  = @($GoogleHome | Where-Object { (& $script:Adb -s $script:Target shell pm list packages -d $_) -match [regex]::Escape($_) })
    }
    if ($DryRun) { Step "[dry run] would save current settings to $BackupFile" }
    else { $backup | ConvertTo-Json | Set-Content -Encoding UTF8 $BackupFile; Step "Saved the TV's current settings to $(Split-Path $BackupFile -Leaf)" }
} else {
    Step "A backup already exists ($(Split-Path $BackupFile -Leaf)); keeping the original one"
}

# 2. Install.
$installed = (& $script:Adb -s $script:Target shell pm list packages $Hew7) -match [regex]::Escape($Hew7)
if (-not $installed -or $Reinstall) {
    if (-not (Test-Path $Apk)) { throw "Hew7.apk wasn't found at $Apk" }
    Step "Installing Hew7"
    if (-not $DryRun) { & $script:Adb -s $script:Target install -r $Apk | Write-Host }
    if ($installed) { Write-Host "  Note: reinstalling stops Hew7 Helper; switch it off and on again afterwards." }
} else {
    Step "Hew7 is already installed (use -Reinstall to install it again)"
}

# 3. Permissions.
Step "Giving Hew7 its permissions"
Tv "pm grant $Hew7 android.permission.DUMP" | Out-Null
Tv "pm grant $Hew7 android.permission.PACKAGE_USAGE_STATS" | Out-Null
Tv "appops set $Hew7 GET_USAGE_STATS allow" | Out-Null
Tv "appops set $Hew7 WRITE_SETTINGS allow" | Out-Null
Tv "pm grant $Hew7 android.permission.ACCESS_FINE_LOCATION" | Out-Null

# 4. Screensaver.
Step "Making Hew7 Weather the screensaver"
Tv "settings put secure screensaver_components $Hew7/$Hew7.dream.WeatherDream" | Out-Null
Tv "settings put secure screensaver_enabled 1" | Out-Null

# 5. Home screen.
Step "Turning off Google TV's home screen so Home opens Hew7"
foreach ($p in $GoogleHome) {
    if ((& $script:Adb -s $script:Target shell pm list packages $p) -match [regex]::Escape($p)) { Tv "pm disable-user --user 0 $p" | Out-Null }
}
Tv "input keyevent KEYCODE_HOME" | Out-Null

# 6. Hew7 Helper: the person switches it on.
Step "Opening Accessibility settings on the TV"
Tv "am start -a android.settings.ACCESSIBILITY_SETTINGS" | Out-Null
Write-Host ""
Write-Host "Done. On the TV, select 'Hew7 Helper' and switch it on."
Write-Host "To undo everything later: powershell -ExecutionPolicy Bypass -File Hew7-Undo.ps1 -TvIp $TvIp"
