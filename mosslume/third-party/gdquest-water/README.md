# GDQuest 2D water adaptation

- Repository: https://github.com/gdquest-demos/godot-shaders
- Pin: `05a3931acc85513b67b80b6e88671a596a3f9693`
- Source: `godot/Shaders/water_2D.gdshader`, retained in `upstream/`.
- Code: MIT. Repository artwork is separately CC-BY-NC-SA 4.0; no upstream artwork is used.

`OceanSurfaceRenderer` and `mosslumeOcean.glsl` adapt the source's two-channel noise UV displacement and sine/cosine variation to Java/LWJGL and GLSL 1.20. Noise data is generated with our already included MIT FastNoiseLite; ocean art is our existing art. World UVs, two flow directions, shallow-water modulation and thin foam are local composition. The original Godot NORMAL_MAP interface is not imported. The initial pipeline retains native terrain illumination and separate SDV-derived coast/water shadows; it does not claim full reflection or refraction simulation.

The water is a bounded native terrain batch, without per-tile shader switches or texture uploads. Both wind and water use the same paused visual clock. Original ocean art remains NEAREST; only the generated distortion data is LINEAR.
