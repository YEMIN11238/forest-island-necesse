# Mosslume Mac 应用打包

入口为 `~/Applications/Mosslume.app`。工程入口 `outputs/mosslume/Mosslume.app` 和桌面入口指向同一个安装包。原有“林间小岛·Necesse”应用和 Steam 安装不修改。

打包前先完成 `outputs/necesse-island/scripts/build_mod.py`，再运行：

```sh
python3 outputs/mosslume/scripts/package_app.py --skip-icon
```

`--icon-only` 只将用户指定的背包 JPG 转为 `.iconset` / `.icns`；完整保留原构图和灰色背景，不裁切或重绘。原件保存在 `branding/app-icon/source-backpack.jpg`，来源哈希在 `provenance.json`。

`--compile-launcher-only --skip-icon` 只编译 universal arm64/x86_64 原生启动器，供打包前检查。

应用内包含已有源码构建内核、游戏资源、Java 与岛屿适配包。Java 运行库仍为 Intel 版本，因此 Apple Silicon 通过系统 Rosetta 运行。打包使用 macOS 自带的 clang、sips、iconutil、codesign。

启动器通过 `NSRunningApplication` 检查 `com.valvesoftware.steam`。若 Steam 尚未运行，使用 `NSWorkspace` 打开已安装的 Steam，并在约 15 秒内等待其应用进程完成启动；缺失或启动失败会显示可操作的中文提示。正常依赖检查不创建 `NSApplication`，不主动把 Mosslume 的原生错误提示层加入 Dock。Steam API 初始化、账号授权和游戏所有权验证仍由原内核和 Steam 处理；启动器不修改或跳过认证。

默认参数为 `-islandprofile -mosslume`，不自动加载存档，以展示独立首页。JVM 属性包括 `mosslume=true`、`mosslume.windowTitle=Mosslume`，Dock 使用用户提供的图标。具体游戏窗口标题由品牌适配层设置。

若品牌适配层已生成 `mod/resources/mosslume/branding/title.png` 与 `blank-logo.png`，打包脚本同时复制至 `Game/res/keyart.png`、`Game/res/logo.png`。这些是启动阶段加载器支持的松散资源覆盖，能在 Mod 初始化之前替换原生背景与旧 Logo。

新存档目录为 `~/Library/Application Support/Mosslume`，启动日志为其中的 `runtime.log`。第一次普通启动仅在新目录尚无 `saves` 时复制旧版 `ForestIslandNecesse/saves`；新目录尚无 `settings.cfg` 时复制旧版设置，保留中文和按键配置。完成后写入 `.mosslume-profile-v1`。之后不再覆盖旧版或新版存档。

可通过 `MOSSLUME_PROFILE=/absolute/test/path` 使用独立测试目录；测试模式不迁移个人存档。原生启动器检查 Java、内核、适配包及 Rosetta，失败时显示中文原生提示并保留日志；Java 成功启动后的异常继续由运行内核记录。

脚本不启动应用、不上传文件。实际打包来源和 SHA-256 写入 `build/package-manifest.json`。
