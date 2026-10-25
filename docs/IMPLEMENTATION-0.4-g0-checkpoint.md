# G0 性能开发检查点

日期：2026-10-23。状态：有已验证改善，**G0 未通过，不能发布为性能问题已解决**。游戏版本字段仍为 0.4.0-r2；这份记录不是完整 0.5 发布说明。

## 已完成的改动

- 按 [libGDX 固定提交](https://github.com/libgdx/libgdx/blob/03799a27a37fe8b10446872d48aa24cb68fb70f8/gdx/src/com/badlogic/gdx/graphics/g2d/SpriteBatch.java) 的有界缓冲和 flush 机制适配原生 GL 2.1。78 张原尺寸纹理进入 NEAREST 图集；每批最多 256 个相邻物件，原生物件和纹理绘制边界先提交，不跨深度重排。原文、Apache-2.0 许可证与修改说明在 third-party/libgdx-render。
- 保留原有八段树冠网格、连续风公式、脚底锚点、像素覆盖过滤、昼夜材质、三盏局部表面灯和八光源投影预算。每帧风时间统一；每个物件的灯位置/半径/强度随顶点传入，三色精确调色板遇到第四种颜色就提交，不量化颜色或丢灯。透明区域和零强度灯跳过无贡献计算。
- 岸线几何缓存与水色时间分开。每次绘制准备核对原生地形/已加载区域，改变时重建；不缓存动画颜色、不降低海面层数。与原直接浅水算法逐点比较，浮点结果一致，覆盖地形编辑、加载状态、负边界和相机移动。
- 建筑元数据在绘制准备时读取，避免每个地块构造 GND 字符串；道路坐标哈希避免对角线集中碰撞。原生元数据和地块仍是权威。
- 在 Apple M5 上改用同版本 Temurin 17.0.19+10 ARM JRE，保持原内核和游戏原生库不变。原 LWJGL 有 ARM 原生包，Steamworks 动态库为通用架构。预加载原有 Byte Buddy agent，解决紧凑 ARM JRE 无 attach 模块导致的启动失败。
- ARM JRE 来源是 [Temurin 官方发行](https://github.com/adoptium/temurin17-binaries/releases/tag/jdk-17.0.19%2B10)，归档 SHA-256 为 `cef790b404cf168fd1a8a7abc5054fbb442c7d4bfe390cceccfe3f64b9b776a9`。安装脚本校验归档；打包不会混合两个架构的 JRE。

最初“仅共用 VBO”的试验发生回退，不能把它作为性能收益。ARM + ZGC/3G 的短样本接近目标，但 180 秒试验出现 1.9 秒长帧及未能自动退出，故没有采用。当前候选为 G1、初始堆 512M、最大 2G，暂停目标 10ms 只是请求，不保证实际暂停都在 10ms 内。失败试验保存在忽略的 build/refactor-evidence 中。

## 实测范围与结果

环境为 Apple M5，GL 2.1 Metal，逻辑 1200×900、帧缓冲 2400×1800。测量是完整帧间隔，包含绘制、交换和调度等待；不等于 GPU 耗时。测量时无截图和 PNG 编码，后续取图阶段独立。每一行代表不同检查点或场景，不能混成一个最终提升百分比。

| 检查点 | 场景/时长 | 平均 fps | p95 / p99 ms | 超过 50ms |
| --- | --- | --- | --- | --- |
| Intel 修改前 | 生成营地，30s | 35.85 | 45.82 / 73.84 | 35 / 1072 |
| ARM、缓存合批、G1 小堆 | 生成营地，100s，带 JFR | 58.58 | 21.27 / 31.89 | 17 / 5854 |
| 直接传灯数据 | 白天林地，90s | 57.44 | 26.70 / 42.05 | 31 / 5161 |
| 岸线缓存前 | 原视觉岛夜间海岸，60s | 35.94 | 38.51 / 53.23 | 28 / 2154 |
| 岸线缓存后、当前候选 | 同海岸，120s | 48.44 | 37.25 / 67.66 | 133 / 5809 |

海岸平均帧率改善，但长帧分布没有过关；不能只报告平均值。候选经过多次改动，营地和林地不是最新包的重新验收。最新海岸样本约一半时间在后台；已禁用失焦省电，仍记录前后台帧数，不能由此排除系统负载影响。

[证据汇总](../mosslume/validation/performance-g0-checkpoint/summary.json) 包含各自 mod SHA、环境、完整帧 CSV、统计和 14 套纯本地回归结果。生成世界候选的原生库存/制作/伙伴检查完成；性能模块变化以外的旧生命周期证据只在相应代码未变时复用。本轮付费模型请求数为零。

## 光影、风动检查的实际边界

已检查实际昼夜帧缓冲、植物/物品和岸线海面投影，未出现 shader 回退；源风公式和美术比例保留。海岸截图中玩家最终进入房屋，触发原有屋顶透明状态，所以这组图不能作固定人物姿态的逐像素等价证明。风动动画在前一候选的实际时间戳帧序列中检查；海岸固定风相位用于观察材质/阴影，不作为动态流畅度证据。

还缺少同相位严格图像对照、玩家移动路线、窗口缩放和两个伙伴工作/战斗场景的完整验收。原生回调检查不等于物理鼠标验收。新的角色、台地和战斗素材尚未因这次性能改动自动实现。

JFR 在明确的 `mosslume.RenderMeasurement` 区间内采样。营地样本中，主线程仍有大量原生绘制调用；分配热点涉及地面装饰、荧光、阴影实例和照明。分配权重是估计值，native 等待样本不是 GPU 计时。async-profiler 工具已取得，但紧凑 JRE 的 attach 未成功，不能声称已有该工具的有效报告。分析脚本采用有界流式解析，避免一次载入数百 MB 的 JFR JSON。

## 包、回退和下一步

独立测试包为 `~/Applications/Mosslume ARM Performance Preview.app`，默认资料目录 `~/Library/Application Support/Mosslume-Performance-Preview`。它没有替换仍在运行的 `Mosslume 0.4 Preview.app` 或修改个人档；QA 都是一次性独立目录。测试包没有自动复制个人模型凭据。

```sh
python3 scripts/prepare_java_runtime.py
python3 scripts/check_refactor.py
python3 scripts/package_refactor.py --skip-build \
  --app-path "$HOME/Applications/Mosslume ARM Performance Preview.app" \
  --profile-name Mosslume-Performance-Preview --gc g1
```

诊断回退入口为 `mosslume.wind.batch=false` 或 `mosslume.wind.immediate=true`。包的 Java 路径和 GC 可显式指定；原 Intel 运行时未删除。

后续仍停留在 G0：对残余原生绘制/分配和长帧继续定位，稳定后按 PRD 补齐每场景 180 秒路线与视觉对照。不要先扩大模型居民和怪物数量。用户最新提出的深度交流、更多居民、台地、建筑材质及攻击模式已写入 [PRD 第 16 节](PRD-0.5-feature-optimization.md#16-追加需求更多有记忆的角色有层次的世界与完整战斗)，其中每项都明确为待实现内容。
