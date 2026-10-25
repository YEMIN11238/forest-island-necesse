# libGDX render adaptation

Source: https://github.com/libgdx/libgdx/blob/03799a27a37fe8b10446872d48aa24cb68fb70f8/gdx/src/com/badlogic/gdx/graphics/g2d/SpriteBatch.java

License: Apache-2.0. Original copyright: 2011, libGDX AUTHORS. The inspected source and full license are preserved here. Runtime license is also included with the adapter resources.

The native adapter reuses SpriteBatch's buffer ownership/upload/draw separation, bounded CPU vertex buffer, static indices and texture/state flush pattern. `CrownSpriteBatch` packs original untrimmed textures into a NEAREST atlas and batches adjacent native-order sprites (256 maximum). Per-sprite wind, world foot, material and light data are vertex attributes. Exact three-color lamp palettes flush at a new color instead of discarding or quantizing lights. Native texture draws, native sorted objects and final sorted-draw boundaries explicitly flush; transparency ordering stays authoritative.

`WindMeshBuffer` uploads one immutable eight-segment mesh for atlas misses. The initial experiment with this mesh alone regressed frame time and is not presented as the successful batching optimization. No libGDX engine is embedded, and vegetation density, source art, wind formula, shadow resolution and light budget are retained.

All native shader, client-array and buffer state must be restored before returning to original drawing. The immediate path remains opt-in with `mosslume.wind.immediate=true`; `mosslume.wind.batch=false` disables cross-sprite batching for diagnosis. Source attribution is architectural adaptation, not a claim that the complete libGDX renderer was transplanted.
