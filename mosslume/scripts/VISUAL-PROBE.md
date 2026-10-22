# 原生视觉对照仪器

仅在独立测试资料目录使用。它固定相机与测试客户端夜间环境，不移动玩家、不改变库存、地图或服务端存档内容。完成时通过已有服务端退出流程保存并关闭，因此仍应使用可丢弃的测试副本。

JVM 参数必须放在 `-jar` 或主类之前：

```text
-Dmosslume.visualProbe=/absolute/path/to/evidence/s1-new
-Dmosslume.visualProbe.warmupSeconds=4
-Dmosslume.visualProbe.perfSeconds=10
-Dmosslume.visualProbe.captureSeconds=8
-Dmosslume.visualProbe.captureFps=30
-Dmosslume.visualProbe.captureWidth=1200
-Dmosslume.visualProbe.captureHeight=900
-Dmosslume.visualProbe.windowWidth=1200
-Dmosslume.visualProbe.windowHeight=900
-Dmosslume.visualProbe.centerX=1536
-Dmosslume.visualProbe.centerY=1216
-Dmosslume.visualProbe.hour=21
-Dmosslume.visualProbe.maxFps=60
-Dmosslume.visualProbe.stop=true
```

也可用原生启动参数 `-islandvisualprobe /absolute/path/to/output` 启用，具体参数分隔方式以现有启动器解析为准，优先使用上面的 JVM 属性。使用既有 `-islandprofile -mosslume -load ForestIsland` 或首页 QA 入口加载测试岛屿。不要依赖旧 `-islandsmoke` 的三截图提前退出；新仪器启用时会隔离该计时器。

默认先预热，再记录无截图、无 PNG 编码的实际 `MainGame.frameTick` 调用间隔。它包含渲染、交换缓冲、垂直同步及调度等待，**不等于 CPU 或 GPU 单项耗时**。性能期完成后才启动 PNG 写入线程和截图，录制期全部排除在性能统计之外。

录制读取原生 `GL_FRONT`，即上一已呈现帧。`full-frame.png` 保存完整帧缓冲；连续帧保存中央原尺寸像素裁剪，无缩小或补帧，裁剪坐标在 `manifest.json`。`frames.csv` 记录实际采样时间、上一呈现帧的风动时间和读回开销；PNG 队列有上限，队列阻塞与错过采样间隔在清单记录。

比较旧风动时追加 `-Dmosslume.wind.legacy=true`。最终显示着色器的旧方案开关以实际实现为准，当前仪器记录 `mosslume.pixel.legacy`。清晰度采样和场景比例改变时应使用对应的相同配置另跑对照，不把不同比例的两图直接当锐度测量。

产物包括 `performance.json`、`performance-frames.csv`、`manifest.json`、`frames.csv`、`full-frame.png` 和 `frames/frame-*.png`。前后两次运行使用同一测试副本的拷贝、同镜头、同窗口、同环境和同性能时长；保存构建与第三方源码版本。

制作时间正确的预览列表：

```sh
python3 scripts/make_visual_probe_video.py /absolute/path/to/probe
```

有 ffmpeg 时追加 `--ffmpeg /absolute/path/to/ffmpeg` 可导出 MP4。它使用真实采样间隔，不将不规则截图强行冒充固定 30 或 60 fps，亦不插帧。实际风动是否连续仍需逐帧检查；真实物理鼠标交互、完整存档重读和玩法回归不属于此仪器的已通过范围。
