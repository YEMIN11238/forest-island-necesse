# SDV-Radiance surface-lighting adaptation

- Repository: https://github.com/PHUICMT/SDV-Radiance
- Pin: `65c153a9c2f979ed7ac5e39b6ecf69fc66ba7ecb`
- License: MIT, retained here.
- `shaders/normals.fx` is retained in `upstream/`.

`VisualMaterialDefinition.bakeNormals` is a CPU-once Java adaptation of the source's two-ring Sobel alpha bevel and painted luminance gradient. Source rectangle bounds prevent sampling other atlas entries. Painted-relief weight is reduced for this game's noisy art. Native sprite alpha and NEAREST color art are preserved; generated normals occupy a parallel atlas.

`SurfaceOcclusion` and the wind fragment shaders use SurfaceMap/floodlight's height/classification/occlusion concepts, with a bounded RGBA8 map of loaded native objects and fixed-count height-ray tests for the three local surface lamps. Stardew location labels and C# services are not imported. Existing projected alpha silhouettes still supply land/water shadows and shoreline receivers, and a narrow compose-only penumbra smooths those masks.

This is the minimal GL2.1 surface path, **not** the upstream complete `reliefreplay`/floodlight pipeline. It does not implement exact per-pixel inter-sprite relief rank, MRT replay, authored wall/roof height maps, reflections or volumetric lighting. It requires no MRT, records the device draw-buffer count, and uses one RGBA8 receiver map with CPU-baked normal data. Full relief replay remains a separately measured extension; a successful compile does not establish visual equivalence to the reference video.

Normal textures, the visible mesh and its shadow share material anchors and the same deterministic WindField deformation. Scene mutations can call `SceneLighting.invalidate(level)` to refresh lamp/occlusion data; periodic bounded refresh is retained as a recovery path. Dynamic lamp intensity is evaluated per render frame, not baked into the old 120ms static-source cache.
