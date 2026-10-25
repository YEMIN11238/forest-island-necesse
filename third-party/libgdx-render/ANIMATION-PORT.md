# Animation playback reference

Source: libGDX `Animation.java`, commit `03799a27a37fe8b10446872d48aa24cb68fb70f8`.
https://github.com/libgdx/libgdx/blob/03799a27a37fe8b10446872d48aa24cb68fb70f8/gdx/src/com/badlogic/gdx/graphics/g2d/Animation.java

Apache-2.0, license retained in this directory and in mod resources.
`CharacterAnimation.frame` adapts NORMAL playback's integer frame selection and
last-frame clamp to native normalized attack progress. There is no libGDX runtime.
Damage, cooldowns, attack directions and projectiles remain native/server-owned.
Attack strips are original imagegen art, exported without repainting; provenance,
source PNGs, source bounds and anchors are in `mosslume/art/characters-r6`.
