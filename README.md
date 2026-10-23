# Mosslume 世界与伙伴重构

当前开发分支是 **0.4.0-r2 世界与 DeepSeek 伙伴预览**。包含新建/选择世界、两种有限种子世界、洞穴通道、伙伴任务和真实事件记录；保留既有像素显示、风动和光影。DeepSeek 需通过本机私有配置显式启用，只有发送文字指令才调用；没有凭据时保持离线。接入与用量限制见 [DeepSeek 本机配置](docs/AI-DEEPSEEK-SETUP.md)。完整目标见 [PRD 0.4](docs/PRD-0.4-world-companions-refactor.md)，基础重构与未完成部分见 [r1 实施记录](docs/IMPLEMENTATION-0.4-r1.md)。

稳定版仍是标签 `v0.3.0-visual-baseline.1`，没有被本轮预览替换。0.4 使用独立应用 `~/Applications/Mosslume 0.4 Preview.app` 和资料目录 `~/Library/Application Support/Mosslume-0.4`，不自动迁移旧个人存档。

```sh
python3 scripts/check_refactor.py
python3 scripts/package_refactor.py --skip-build
```

隔离原生检查使用 `scripts/run_refactor_probe.py --case <新名字>`；`--reload` 验证同一档恢复，`--ladders --preset woodland` 验证洞穴往返，`--harvest` 验证原生采收与取消。`--ai` 是单独显式启用的三请求付费模型验证，常规测试不调用模型。每个首次案例必须使用新名字，证据默认写入忽略的 `build/refactor-evidence/`。检查会真实修改其专用 QA 存档，不应对个人资料目录使用探针。

本次保存最新 Java 适配源码、运行素材、原创美术源图、素材导出与打包脚本、第三方 shader 来源和许可证，以及实际昼夜截图和视频。`mosslume/` 是原 `outputs/mosslume/` 的可移植资料快照；当前 `mod/` 是实际运行的适配器源码。个人存档、日志和测试缓存不在仓库中。

## 0.3.0 已保存效果

- 清晰像素显示、连续树冠风动、紧凑人物比例和连续自然小径。
- 昼夜环境光、营火及窗户局部暖光、GPU 树木和物品投影、岸边投向海面的阴影。
- 已调暗的夜间环境，保留火焰和窗户自发光。
- 最新适配器 SHA-256：`720feaac7f568127f946797281d1e88659591444937e753e7848205a2afa1234`。

实际画面对照在 `mosslume/validation/visual-baseline-0.3.0/`。当前 6 秒无截图性能段平均约 55.6 fps；仍不代表稳定 60 fps 或完整玩法回归。地图仍是视觉测试岛；AI 尚未接入。

## 恢复 0.3.0 工作区

可运行 Mac 包与校验信息放在对应私有 Release。源码快照保留了此前脚本的相邻目录关系，恢复时使用以下布局：

```text
workspace/
  necesse-island/       # 本仓库克隆目录
  mosslume/            # 从本仓库 mosslume/ 复制出来
```

```sh
git clone https://github.com/YEMIN11238/forest-island-necesse.git necesse-island
cd necesse-island
git checkout v0.3.0-visual-baseline.1
cp -R mosslume ../mosslume
python3 scripts/restore_release_runtime.py --app '/path/to/Mosslume S1 Preview.app'
cd ../mosslume
python3 scripts/build_game.py --skip-export
```

当前构建仍需配置 JDK 路径与 macOS 构建工具。默认重新打包独立预览，保留日常应用。运行包保持原有 Steam 依赖及验证流程，存档另外保存在用户资料目录。GitHub 中的美术快照与实际绑定素材分别见 `mosslume/art/`、`mosslume/runtime-art/` 和 `mod/resources/`。

## 历史 0.1.0 底座备份

2026-10-21 的私有项目备份，版本 **0.1.0**。源码、美术和构建脚本保存在本仓库；可运行 Mac 应用位于 [Releases](https://github.com/YEMIN11238/forest-island-necesse/releases)。

## 已保存的内容

- `source-rebuild/`：6,504 份反编译并修复的 Java 源文件，包含客户端与服务器代码。
- `mod/src/`：固定岛屿地图、原生玩法接入和画面适配。
- `mod/resources/`：本项目已使用的树木、灌木、火焰、水面、房屋及物品素材。
- `rebuild/`：全量构建、接口核对、初始化顺序核对和资源来源记录。
- `scripts/`：素材与运行资源准备、构建和原生 Mac 应用打包。
- `evidence/`：本次可运行版本的必要验证摘要。

采集、制作、库存、种植、居民与存档继续使用原生系统。地图固定在我们的岛屿。树冠摇动、8 帧火焰、烟雾、上升荧光、水面变化和局部投影使用已有素材。

## 恢复并运行

从 Releases 下载 Mac 应用压缩包并解压。应用包含重建内核、素材和运行库；保持原有 Steam 可用后启动。正式存档位于 `~/Library/Application Support/ForestIslandNecesse/`，个人存档未上传。

从该应用恢复源码工程依赖：

```sh
python3 scripts/restore_release_runtime.py --app '/path/to/林间小岛·Necesse.app'
python3 scripts/build_project.py
```

重建需要 JDK 21 和 Xcode Command Line Tools。当前构建脚本保留原工作机的工具路径；新机器可先修改 `rebuild/build.py`、`scripts/build_mod.py` 中的 `javac` 路径。原生客户端使用包内 Intel JRE，Apple Silicon 仍需 Rosetta。

## 核验范围

已全量编译；7,540 个具名类的 public/protected 接口无差异；932 个类的静态字段初始化写入顺序已对齐。重建内核已实际启动并读回存档，制作、箱子、边界碰撞和地图跳转限制通过原生接口检查。

未逐项回归整个游戏的全部玩法。人物和界面目前沿用 Necesse，道路阶梯边缘仍待细化。

本仓库为私人项目快照，包含第三方游戏的恢复代码，不附加开源授权。运行日志、个人存档、测试缓存及大体积运行资源没有放入 Git 历史；对应运行资源随私有 Release 应用保存。
