# Draws Hew7's Google Play graphics from the logo (seven-band sunrise over a horizon line):
#   icon-512.png (app icon), tv-banner-1280x720.png (Android TV banner), feature-1024x500.png.
#   powershell -ExecutionPolicy Bypass -File make-store-art.ps1
param([string]$Font = "$PSScriptRoot\..\app\src\main\res\font\nunito.ttf", [string]$Out = "$PSScriptRoot")
Add-Type -AssemblyName System.Drawing
New-Item -ItemType Directory -Force $Out | Out-Null
$colors = '#FF4D4D', '#FF9F43', '#FECA57', '#1DD1A1', '#48DBFB', '#54A0FF', '#A55EEA'
$pfc = New-Object System.Drawing.Text.PrivateFontCollection
$pfc.AddFontFile((Resolve-Path $Font).Path)

# Logo centred on (cx, cy) = the middle of the horizon bar; s = scale (1 = 208 px wide).
function Draw-Logo($g, [float]$cx, [float]$cy, [float]$s) {
    $r0 = 92 * $s; $step = 11.5 * $s; $sw = 8 * $s
    for ($i = 0; $i -lt 7; $i++) {
        $r = $r0 - $i * $step
        $pen = New-Object System.Drawing.Pen([System.Drawing.ColorTranslator]::FromHtml($colors[$i]), $sw)
        $pen.StartCap = 'Round'; $pen.EndCap = 'Round'
        $g.DrawArc($pen, $cx - $r, $cy - $r, 2 * $r, 2 * $r, 180, 180)
        $pen.Dispose()
    }
    $bar = New-Object System.Drawing.Drawing2D.GraphicsPath
    $bx = $cx - 104 * $s; $by = $cy + 6 * $s; $bw = 208 * $s; $bh = 9 * $s
    $bar.AddArc($bx, $by, $bh, $bh, 90, 180); $bar.AddArc($bx + $bw - $bh, $by, $bh, $bh, 270, 180); $bar.CloseFigure()
    $g.FillPath([System.Drawing.Brushes]::White, $bar)
}

function New-Canvas([int]$w, [int]$h) {
    $bmp = New-Object System.Drawing.Bitmap($w, $h, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'AntiAlias'; $g.TextRenderingHint = 'AntiAliasGridFit'
    # Dark, slightly warm vertical gradient, like the TV at dusk.
    $brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush((New-Object System.Drawing.Rectangle 0, 0, $w, $h), [System.Drawing.Color]::FromArgb(255, 30, 32, 42), [System.Drawing.Color]::FromArgb(255, 12, 13, 18), 90)
    $g.FillRectangle($brush, 0, 0, $w, $h)
    return @($bmp, $g)
}

function Draw-Word($g, [string]$text, [float]$x, [float]$y, [float]$px, [int]$alpha = 255, [string]$style = 'Bold') {
    $font = New-Object System.Drawing.Font($pfc.Families[0], $px, [System.Drawing.FontStyle]::$style, [System.Drawing.GraphicsUnit]::Pixel)
    $fmt = New-Object System.Drawing.StringFormat; $fmt.LineAlignment = 'Center'
    $b = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb($alpha, 255, 255, 255))
    $g.DrawString($text, $font, $b, $x, $y, $fmt)
}

# App icon: logo only, centred.
$bmp, $g = New-Canvas 512 512
Draw-Logo $g 256 338 1.9
$g.Dispose(); $bmp.Save("$Out\icon-512.png", [System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose()

# TV banner: logo and wordmark.
$bmp, $g = New-Canvas 1280 720
Draw-Logo $g 380 410 2.0
Draw-Word $g "Hew7" 640 356 170
$g.Dispose(); $bmp.Save("$Out\tv-banner-1280x720.png", [System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose()

# Feature graphic: logo, wordmark and "Launcher".
$bmp, $g = New-Canvas 1024 500
Draw-Logo $g 250 285 1.45
Draw-Word $g "Hew7" 470 225 120
Draw-Word $g "Launcher" 478 318 44 200 'Regular'
$g.Dispose(); $bmp.Save("$Out\feature-1024x500.png", [System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose()

Get-ChildItem $Out -Filter *.png | ForEach-Object { "$($_.Name)" }

