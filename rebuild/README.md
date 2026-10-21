# Necesse 1.3.3 完整源码重建

这里已经能把恢复的完整客户端和服务端 Java 源码重新编译成可运行的游戏内核。所有编译输入均来自 `source-rebuild/` 和项目内的第三方依赖目录；没有用原始游戏 class 填补编译失败的部分。

当前最近一次干净构建为 `runtime02`，编译退出码 0，耗时约 11 秒。输入 6,504 个 Java 文件，输出 10,221 个 class，保留采集、制作、建造、物品、NPC、地图、网络、存档、控制器、界面及调试分支。岛屿场景和自有素材由独立的 `mod/` 适配层提供。

## 复现构建

在项目根目录执行：

```sh
python3 outputs/necesse-island/rebuild/build.py --tag local
python3 outputs/necesse-island/rebuild/verify.py
```

`build.py` 每次先清空本目录内的 `classes/`，再编译全部源码，使用 JDK 21 的 `javac --release 17`。目前编译器路径固定为本机已安装的 `/Users/zhangjunhao/Documents/Codex/tools/jdk-21.0.12.1+1/Contents/Home/bin/javac`。依赖来自 `../runtime/Necesse.app/Contents/Resources/lib/`，共 33 个第三方 jar。主游戏 jar 不在编译 classpath 内。

输出为：

- `Necesse-source-built-1.3.3.jar`，入口仍为原始 `StartSteamClient`。
- `Server-source-built-1.3.3.jar`，入口仍为原始 `StartDesktopServer`。

两个 jar 复用 `manifests/` 中逐字节保存的原始 Manifest，包含原本的库路径和规范折行。启动依赖配套资源与 native 库，应通过项目的 `../scripts/build_project.py` 打包并使用 `../scripts/launch.py` 启动。Steam 认证和原版授权逻辑保留。

## 恢复范围与证据

| 项目 | 结果 | 证据 |
| --- | --- | --- |
| 原始客户端 | 10,249 个 class，6,493 个恢复后的顶层源码文件 | `recovery.json`、`original-manifests.json` |
| 服务端额外部分 | 18 个独有 class，11 个顶层源码文件 | `reconstruction-coverage.json` 中的服务端独有类清单 |
| 客户端/服务端共享 | 10,173 个 class 原字节相同，未重复反编译 | 项目调查目录 `evidence/server-client-comparison.json` |
| 全部 Java 源码 | 6,504 个，均参与干净编译 | `source-inventory.json`、`build-runtime02.json` |
| 重新生成的 class | 10,221 个 | `verification.json` |
| 有明确名字的 class API | 7,540 个全部存在，public/protected 字段和方法描述符及访问位零差异 | `api-comparison.json` |
| 静态字段写入顺序 | 932 个原始类全部比较，同类非 `$` 前缀字段的 PUTSTATIC 顺序零差异 | `static-init-order.json` |
| 反编译失败占位 | 未发现已知 Vineflower/CFR 失败标记、未表示类型及方法占位 | `verification.json` |
| 编译依赖中的游戏 class | 与原游戏 class 名称清单的交集为零；编译 classpath 不含原版游戏 jar | `verification.json`、`build-runtime02.json` |
| Manifest | 两个输出均与各自原版逐字节相同，最长行 72 字节 | `verification.json` |

10,267 个原始不同 class 与 10,221 个重建 class 的数量差异来自编译器重新生成的匿名、合成与桥接结构。API 比较明确排除了带 `$数字` 的匿名/合成编号类和 synthetic/bridge 成员；不能把上述数据解释为所有 class 字节或所有运行行为完全相同。

原始输入 SHA-256：

```text
Necesse.jar 3390e31e593b7ea9f75dee59c7d54f29be3d5705a5eff647ba2053f9a2c170a7
Server.jar  83e08ad78ceb5f6f24b5ed9bee96ce63160e4f72a7bff8dafd8b99cb649952ef
```

`runtime02` 输出 SHA-256：

```text
Necesse-source-built-1.3.3.jar 0f0a2a5144d70810218c21f6a4020236776ddd9c9d6828b80e997882cf3d90e6
Server-source-built-1.3.3.jar  0a238bebc806ff78a6116599c8ffaca1f0ad1093f0f9fe2f12afef4ba9951832
```

重复构建会更新 zip 时间戳，jar 整体散列可能改变。每次构建后的散列记录在 `build-<tag>.json`。

## 源码修复方式

主恢复器为官方 Vineflower 1.12.0。CFR 0.152 用作 HUD 的补充恢复，HUD 的绘制主体采用无局部变量名提示的 Vineflower 结果。原始 CFR 树和 Steam 安装文件均保留。

修复集中在反编译恢复产生的 Java 源码问题：擦除后的泛型、匿名子类构造类型、编译器无法表达的匿名类型转换、lambda 中被反复复用的局部变量、内部类外部对象引用、及静态初始化顺序。原有方法和调试分支保留，未用空方法代替复杂逻辑。分批变更说明见 `semantic-repairs-*.json` 和 `runtime-repairs-*.json`。

首次实际启动在 `DLC.<clinit>` 暴露了初始化顺序问题：注册 DLC 发生在列表和映射创建前。修复恢复了原始字节码的执行次序。随后检查全部 932 个静态写入类，又把 ControllerInput、Localization、GameInterfaceStyle 和 5 个 PresetSet 的创建/注册顺序恢复为原始次序。该检查不执行目标游戏代码。

`UniqueIncursionModifierRegistry` 中一个原版的泛型调用看起来会把 Iterable 作为单个 varargs 元素。原字节码也确实如此，恢复时通过显式 Object 转换保留原行为；没有擅自修改玩法。证据位于工作目录的 `unique-modifier-bytecode.txt`。

## 已观察的运行结果

主线程已用 `runtime01` 完整源码内核实际启动客户端、本地服务端和固定小岛。`../test-profile-source-02/evidence/native-checks.json` 记录原生箱子、配方消耗与产出、非法配方拒绝、166 个岸线阻挡及跨地图跳转阻止检查通过；帧证据在该目录的 `frame-2.png`。这些检查通过原生服务端 API 进行，不代表每项都经过物理鼠标点击。

`runtime02` 进一步恢复静态初始化顺序，通过了上列编译和静态结构检查。该版本的最终启动、存档重载和素材画面核验由项目主线程继续记录；具体结论以对应 profile 与日志为准。编译成功和 API 一致不能代替全游戏流程回归。


## 最终运行验证

runtime02 源码内核已在 `logs/source-reload-01.log` 中完成启动并读回既有存档，原生制作消耗与产出、非法配方拒绝、箱子、海岸碰撞和地图限制通过。`evidence/release-validation.json` 汇总了最终安装应用的内核哈希和运行范围。原生 Mac 应用已通过实际 `open` 入口启动，独立数据位于用户 Library/Application Support/ForestIslandNecesse。该验证不代表全部玩法分支或物理鼠标输入都已回归。
