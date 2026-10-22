# Grounded shadow projection port

- Source: https://github.com/PHUICMT/SDV-Radiance
- Pinned commit: `bff10581b2fd104f4ed094f67ae5afaa7a024c67`
- License: MIT, copyright 2026 phuicmt; included here and in the runtime JAR.
- Ported source: `src/Shadow/ShadowRenderer.cs`, `ShadowProjection.ForSolid`, its across/along basis and both-component mirror correction.
- Related design reference: `src/Shadow/ShadowRenderer.GroundedCasts.cs`, grounded feet projection.
- Native implementation: `localforest.visual.RadianceProjection` and `RadianceShadowRenderer`.

The port keeps the feet anchored and projects sprite width onto a foreshortened ground perpendicular, instead of stretching sprite rows horizontally. The renderer samples the entire sprite alpha mask at full scene resolution. Moving vegetation uses the same nine mesh vertices, gust equation, source coordinates and render time as the wind mesh.

The receiver implementation is local integration code. It keeps a separate GPU union mask for each light using `GL_MAX` blending and composites the blocked-light fraction in a GLSL 1.20 shader, allowing another unblocked lamp to fill the shadow. Up to eight visible lights are retained, including the directional source. Shoreline edges come from the actual native land/liquid boundary; their projected quads are clipped to water by a small map texture. A lower receiver plane adds displacement for tree and object shadows reaching the sea. The native framebuffer, matrices, shader stack and GL state are restored after the receiver pass.

Casters sharing an alpha mask are submitted as one interleaved client-array batch per light and receiver. Degenerate strip endpoints prevent geometry connecting separate trees. Projection and wind vertices are computed once per caster/light and reused for the lower water plane. Client-array state and array-buffer bindings are restored. This reduces draw submissions without changing shadow resolution or the eight-light budget.

This is **not** the complete SDV-Radiance lighting pipeline. It does not implement volumetric light, authored normal maps, wall receiver climbing, its reflection pipeline, or cross-sprite lighting occlusion. It renders ground and water receiver shadows only. The original CPU receiver remains behind `-Dmosslume.shadow.cpu=true` for comparison; the first native integration showed an unacceptable frame-rate regression and it is no longer the default. `MOSSLUME_SHADOW_COST` logs measure CPU submission time, not GPU execution time. Runtime checks are recorded separately from source compilation.
