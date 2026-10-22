# Simple Wind Shader 2D 移植记录

- 上游：[JaccomoLorenz/godot-simple-wind-shader-2d](https://github.com/JaccomoLorenz/godot-simple-wind-shader-2d)
- 固定版本：`5a1ee79571530749b707c8fa3154acbe90aee727`，2026-10-22 获取。
- 原始文件：`assets/maujoe.simple_wind_shader_2d/shaders/wind_2d.shader`，完整原文保存在 `upstream/wind_2d.shader`。
- 许可：MIT，版权为 Jaccomo Lorenz (Maujoe)，完整声明保存在 `LICENSE.md`，并随运行资源提供。

实际移植位于 `outputs/necesse-island/mod/resources/shaders/vertex/mosslumeWind.glsl` 和 `mod/src/localforest/visual/CrownWindRenderer.java`。不是把 Godot 项目作为新游戏底座。

保留的上游公式为 `time = timer * speed + phase`、`diff = (maxStrength - minStrength)^2`、`strength = clamp(minStrength + diff + sin(time / interval) * diff, minStrength, maxStrength) * strengthScale` 以及 `(sin(time) + cos(time * detail)) * strength * heightWeight`。

适配包括 GLSL 1.20 内建顶点与矩阵、引擎着色器栈、原生光照色和透明度；原 Godot 的位置畸变改为轻微的固定世界位置相位，避免镜头移动影响风；高度权重增加幂次，使根部至冠顶逐渐弯曲。采用 8 段共边网格，而不是原实现的逐像素行位移取整。

初始参数：`minStrength=.38`、`maxStrength=.70`、`interval=3.5`、`detail=1`；树冠 `speed=2.05`、幅度比例 `.052×显示宽度`、根部范围 `.04`、高度幂 `1.35`；小植物 `speed=1.9`、幅度比例 `.034×显示宽度`、根部范围 `.08`、高度幂 `1.8`。这些是待原生对照检查的起点，不代表已达到参考视频。

QA 接口：`-Dmosslume.wind.legacy=true` 回退旧逐行渲染；`-Dmosslume.wind.time=秒数`（也兼容 `mosslume.wind.fixedSeconds`）固定采样时间。原生探针可每帧调用 `CrownWindRenderer.setProbeTimeSeconds(double)`，完成后 `clearProbeTime()`；兼容动态属性 `mosslume.probe.animationSeconds`。默认使用连续单调时钟，不依赖模拟 tick。

风动片段采样复用引擎已有 `fragPixelScaling.glsl` 中 Morris Tabor Arroad 的 CC0「[Sub-pixel accurate pixel sprite filtering](https://godotshaders.com/shader/sub-pixel-accurate-pixel-sprite-filtering/)」UV 映射，源公式采用 `fwidth`、像素边缘与夹取权重，保持源像素内部平坦，只在像素边界提供屏幕像素足迹范围的覆盖。为避免影响其他物件，贴图继续 NEAREST，改用四个像素中心采样及预乘 Alpha 插值，再还原为引擎使用的直通 Alpha；不改变全局 MIN/MAG 过滤器，不模糊整屏，也不改变源像素尺寸。

该覆盖默认开启，`-Dmosslume.wind.coverage=false` 可对照纯 NEAREST。新增 uniform 为 `texture_pixel_size=(1/w,1/h)`、`smoothing_factor=1.0`、`pixelCoverage`；`usesPixelCoverage()` 与启动日志可核对实际设置。CC0 来源声明随片段着色器及运行许可目录保存。

编译成功不等于运行效果通过。着色器链接、实际帧绘制、根部稳定、连续运动、显示锐度及帧时间由主任务在真实应用中另行核验。
