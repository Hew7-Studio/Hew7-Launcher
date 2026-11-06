# Security policy

## Supported versions

Only the latest release gets security fixes. Older versions are not patched; update to the latest
release from [Releases](../../releases).

| Version | Supported |
|---------|-----------|
| 2.0.x   | Yes       |
| < 2.0   | No        |

## Reporting a vulnerability

**Please don't report security problems in public issues.**

Report them privately through GitHub: on this repository's **Security** tab, choose
**Report a vulnerability**. Only Hew7 Studio can see the report.

Please include:

- the Hew7 version (Settings › About) and the TV's make, model and Android version;
- what an attacker can do, and what they need first (for example: an app already installed on
  the TV, access to the same network, or physical access to the remote);
- steps to reproduce it, or a proof of concept.

Hew7 is maintained in spare time, so there's no guaranteed response time. Reports are read and
confirmed as soon as possible, and fixed issues are mentioned in the [changelog](CHANGELOG.md) (with
credit, if you want it). Please give a reasonable time for a fix before you publish details.

## What Hew7 can do on your TV

So you can judge the risks, this is everything Hew7 is allowed to do:

| Permission or access | Given by | Used for |
|---|---|---|
| Internet | installing | weather (approximate location from your internet address, Open-Meteo) and wallpapers (Bing or Picsum), over HTTPS only |
| Memory, usage access, write system settings, location | the setup script, over ADB | Running Apps, last-opened times, the screensaver timer, the Wi-Fi name |
| Notification access (optional) | the setup script, over ADB | Now playing: reads only media sessions and media notifications |
| Accessibility: Hew7 Helper (optional) | you, on the TV | presses buttons on the TV's own settings and power screens; turns the system sounds off while it works |

Hew7 doesn't open any network ports, has no server or account, collects no analytics, and
doesn't load code from the internet. Its background services (Hew7 Helper, Now playing and the
screensaver) can only be connected to by Android itself, not by other apps.
Plain-text (HTTP) network traffic is blocked.

## Keeping your TV safe

- **Turn ADB off after setup.** The setup and undo scripts need **USB debugging** (network ADB).
  While it's on, a device on your network that you approve on the TV can control the TV. When
  setup is done, turn it off in the TV's Developer options, and only ever approve your own
  computer.
- **Install Hew7 only from this repository's [Releases](../../releases).** Each release lists the
  APK's SHA-256 so you can check the download, and every release is signed with the same Hew7
  Studio key (Android refuses an update signed with a different key).
- **Hew7 Helper is optional.** Leave it off if you don't need closing apps, Restart and Shut Down,
  or the remote's Settings, Input and Menu buttons.

## No warranty

Hew7 is provided "as is", without warranty of any kind, and Hew7 Studio isn't liable for damage
arising from its use, including security incidents, as set out in sections 7 and 8 of the
[Apache License 2.0](LICENSE). See also the disclaimer in the [README](README.md#disclaimer).
