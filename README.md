# 林间小岛 · Necesse 底座

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
