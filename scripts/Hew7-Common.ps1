# Shared by Hew7-Setup.ps1 and Hew7-Undo.ps1. Not meant to be run on its own.

$Hew7 = "dev.hew7.launcher"
$GoogleHome = @("com.google.android.apps.tv.launcherx", "com.google.android.tungsten.setupwraith")

function Find-Adb {
    $cmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    foreach ($p in @("$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe", "$PSScriptRoot\platform-tools\adb.exe", "$PSScriptRoot\adb.exe")) {
        if (Test-Path $p) { return $p }
    }
    throw "adb wasn't found. Download Google's 'SDK Platform-Tools for Windows', unzip it next to this script (a 'platform-tools' folder), and run the script again."
}

function Connect-Tv([string]$adb, [string]$ip) {
    $target = if ($ip -match ':') { $ip } else { "${ip}:5555" }
    Write-Host "Connecting to $target ..."
    # adb prints "not found" / "unauthorized" to stderr while it waits; Windows PowerShell would
    # treat that as fatal, so these calls go through cmd and ignore stderr.
    for ($i = 0; $i -lt 30; $i++) {
        cmd /c "`"$adb`" connect $target >nul 2>&1"
        $state = (cmd /c "`"$adb`" -s $target get-state 2>nul")
        if ($state -eq "device") { return $target }
        if ($i -eq 0) { Write-Host "Waiting for the TV. If it asks 'Allow USB debugging?', choose 'Always allow from this computer' and OK." }
        Start-Sleep 2
    }
    throw "Couldn't connect to $target. Check the IP address, that the TV is on, and that USB debugging is enabled (Settings > System > Developer options)."
}

# Runs a command on the TV (or only prints it in a dry run).
function Tv([string]$cmd) {
    if ($script:DryRun) { Write-Host "  [dry run] adb shell $cmd"; return "" }
    return (& $script:Adb -s $script:Target shell $cmd 2>&1) -join "`n"
}

function Step([string]$text) { Write-Host "- $text" }

# One backup per TV, named after its address.
function Get-BackupFile([string]$target) { Join-Path $PSScriptRoot ("hew7-tv-backup-" + ($target -replace '[:.]', '-') + ".json") }
