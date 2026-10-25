# Hew7 name and logo policy

Hew7's **source code** is open source under the [Apache License 2.0](LICENSE). You're welcome to
read it, learn from it, change it and build your own app from it.

Hew7's **name and brand are not** part of that license. Section 6 of the Apache License says so,
and this page explains what that means.

## What belongs to Hew7 Studio

- The names **Hew7**, **Hew7 Launcher** and **Hew7 Studio**, and anything confusingly similar
  (for example "Hew 7", "Hew7 Pro", "Hew7X").
- The **Hew7 logo** (the seven-band sunrise over a horizon line), the app icon, the TV banner and
  the store graphics: `app/src/main/res/drawable/ic_hew7_fg.xml`,
  `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`, `app/src/main/res/drawable-xhdpi/banner.png`,
  `app/src/main/java/dev/hew7/launcher/ui/BootIntro.kt` (the intro artwork) and everything in
  [`brand/`](brand). All rights reserved.
- The app ID **`dev.hew7.launcher`**.

## If you build your own app from this code

You must:

1. **Rename it.** Use a name that doesn't include or resemble "Hew7".
2. **Replace the logo, icon, TV banner and intro artwork** with your own.
3. **Change the app ID** (`applicationId` in `app/build.gradle.kts`) to one of your own.
4. Keep the `LICENSE` and `NOTICE` files, as the Apache License requires.
5. Not suggest that your app is made, endorsed or supported by Hew7 Studio.

## What's always fine

- Saying truthfully that your app is "based on Hew7" or "forked from Hew7", in plain text.
- Writing about, reviewing or linking to Hew7 by name.
- Building and running the unchanged code on your own devices for your own use.

## Questions

For permission to use the Hew7 name or logo, contact Hew7 Studio through the GitHub repository.
