# Third-party notices

## AndroidLiquidGlass / Backdrop

Copyright 2025 Kyant

Source: https://github.com/Kyant0/AndroidLiquidGlass

Reference revision: `65ab177e90e5c1d8c62e70cf7755841982da65f6` (`kmp`).

The Backdrop implementation and catalog controls under
`app/src/main/java/com/kyant/` include code adapted from this project.
Local modifications integrate Android APIs, Quadrinheiro themes, accessible
touch targets, reduced motion, gesture cancellation and spring parameters.

Licensed under the Apache License, Version 2.0. See
`licenses/AndroidLiquidGlass-APACHE-2.0.txt` for the full license.

## Samsung Now Bar compatibility research

Reference: [MukulBolt9/xz (NowBrief)](https://github.com/MukulBolt9/xz),
[`NowBriefService.kt`](https://github.com/MukulBolt9/xz/blob/main/app/src/main/kotlin/vip/mystery0/pixel/meter/service/NowBriefService.kt),
Apache-2.0. The optional Samsung adapter uses the documented-in-source bundle
field names and legacy ongoing-activity bit investigated there. It preserves
Quadrinheiro's package identity, standard notification template, Android 7.0
fallback, and signature; no upstream application or library is bundled.
