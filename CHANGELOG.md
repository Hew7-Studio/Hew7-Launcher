# Hew7 changelog

## 2.0.0 — 6 November 2026 (new design)

**Updating from 1.0.x:** Android stops Hew7 Helper when Hew7 is updated. Afterwards, go to the
TV's Settings › Accessibility › Hew7 Helper and switch it off and on again.

**Night glass look:** a new design throughout, with smoky glass panels, the Outfit display font
and softer text shadows. Inter is the new default font.

**Spotlight:** the home screen follows the app you select in the dock: a soft glow in the app's
colour, its artwork, and what it's doing.

**Now playing:** the Spotlight shows what's playing or paused in an app (title, artwork, progress
and time left), gathered from media sessions and media notifications. What each app last played
is remembered for 14 days; apps that share nothing show when you last opened them. With nothing
selected, whatever is playing right now is shown. Needs Notification access, which the setup
script gives (the Undo script takes it back).

**Follows the day:** the wallpaper warms in the hour around sunrise and sunset and dims at night,
with a warmer tint late at night. The home screen looks the same at night as during the day.

**Bigger clock and greeting.** The weather pill opens sideways to show the next hours and
tomorrow.

**Dock:** seven apps at a time, with page dots and a Play Store icon before them. The selected
app lifts out in 3D instead of an outline. The highlight moves to the 4th app, stays there while
the apps glide past, then moves on to the last one; Right on the last app no longer wraps round.
Hew7 TV is the first app when it's installed, otherwise Netflix.

**Screensaver:** in the style of the old night mode: a clock at the bottom left, a glass weather
pill at the top right and a glass card at the bottom right (Now playing, or the TV and network).
Everything drifts smoothly, and the text turns dark on bright pictures. Rain splashes on the glass
and snow settles on it.

**Settings:** a compact glass panel in the top-right corner instead of the full-screen pages.
Coloured categories (Appearance, Dock, Remote, Screensaver, Quick Settings, About); Left and Right
step through each setting's options one by one and apply as you go; Up and Down stay inside a
category. Remote buttons have their own category. Power has separate Sleep, Restart and Shut Down
buttons.

**Machine Room:** a live dashboard with CPU, memory, storage and network gauges, and Running Apps
in two columns right below. It opens on the first app, so closing one is a single OK.

**Hew7 Helper is quieter:** while it closes apps or opens a settings page, the TV's click sounds
are switched off, and the progress spinner shows on the button you pressed instead of a separate
"Closing…" box.

## 1.0.1 — 26 October 2026

**Hew7 Helper status everywhere it's needed:** Running Apps, Quick Settings, Power, Remote Buttons
and Screensaver (Sleep After) now say clearly when Hew7 Helper is off or has stopped (Android stops
it whenever Hew7 is updated), with a **Restart Hew7 Helper** / **Turn On Hew7 Helper** button and
step-by-step instructions. Features that need it explain what to do when tapped; Restart no longer
sends you to the TV's System settings.

**Setup script** finds the release download (`Hew7.Launcher-<version>.apk`) by itself.

## 1.0.0 — 25 October 2026 (first stable release)

**Home screen:** clock, date, weather and greeting with readable "ink" on any wallpaper; app dock
with 3D pop highlight, wrap-around, hidden Play Store tile, reorder and app menu; resting state;
Back from an app highlights it.

**Wallpapers and screensaver:** Bing daily or random 4K photos, decoded at screen size; weather
screensaver with effects on/off and a Start After timer.

**Screensaver energy saving**, both chosen on the Screensaver page:
- **Calm After** (15 min, 30 min, 1 hour, Never; default 30 min): the picture stops zooming and
  dims a little, the weather animation stops, and the scene is redrawn once a minute (the clock,
  with a small shift so nothing burns in) instead of every frame.
- **Sleep After** (1, 2, 4 hours, Never; default 2 hours): the TV goes to standby, as a press of
  the power button does. Uses Hew7 Helper when it's on.

**Panels:** Inputs (the TV's own menu), Machine Room with Running Apps (close one or all, in the
background), Hew7 Settings (theme, font, greetings, tiles, names, wallpapers, temperature,
transparency, screensaver, remote buttons, about) and Quick Settings (Display and Sound, Network,
Bluetooth and Remotes, Accessibility, Apps, All Settings, Accounts, Power with Restart and Shut
Down).

**Remote Buttons:** reads button names from the TV; Settings, Apps, Menu and Input defaults;
assign any button; app buttons (Netflix, YouTube, Prime Video…) open their apps from anywhere.

**Hew7 Helper (optional Accessibility service):** with an in-app consent screen first.

**Release:** open source under the Apache License 2.0 (the Hew7 names and logo stay trademarks
of Hew7 Studio); signed with the Hew7 Studio upload key; HTTPS only; no backups of app data;
test-only hooks removed; intro shows "Hew7" only.
