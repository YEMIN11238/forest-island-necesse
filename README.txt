林间小岛 · Necesse 底座 0.1

入口
  /Users/zhangjunhao/Applications/林间小岛·Necesse.app（已安装，桌面同名快捷入口）
  scripts/launch.py
  应用内已包含运行库、重建内核和素材，无需 Python；可独立于源码目录运行。

这个项目包含什么
  source-rebuild/ 是从本机正版 Necesse 1.3.3 恢复并修复的完整 Java 源码工程。
  6,504 个 Java 源文件可在不引用原游戏类文件的条件下全量编译。
  rebuild/ 输出源码重建客户端与服务器；第三方运行库和正版资源留在 runtime/。
  原始主程序和服务器共 10,267 个类；重建产生 10,221 个类。匿名类和编译器合成类会变化。
  已对照 7,540 个具名类，public/protected 非合成接口无缺失、无描述符差异。
  以上说明恢复和编译范围，不代表每一个玩法分支都经过回归测试。

我们的小岛
  mod/src/localforest/world/ 使用原生 Level、ObjectEntity、Inventory、Recipe 和 Settlement。
  自定义地图为 96 × 80 格；海岸有原生不可破坏碰撞，并拦截前往其他地图的请求。
  场景延续原有岛形、树木布局、房屋和物品布局，使用之前生成的素材。
  图像适配只替换绘制，原生采集、制作、箱子、燃料、种植、居民和存档逻辑继续工作。
  两名初始居民为林叔和小禾，可通过原生居民与定居点界面交互。
  全部原生配方与玩法源码保留；地图限制意味着其他地区、地牢和首领内容尚未布置到岛上。
  UI 和人物目前沿用 Necesse，环境使用我们的素材。这是可继续改动的底座首版。

操作
  W A S D 移动；数字键选择工具；左键使用工具；右键与箱子、工作台、居民交互。
  E 打开背包与制作；C 打开定居点；Esc 暂停与退出存档。
  岛上的箱子备有木材、石头、种子、工具和可放置物。
  F4 可显示或隐藏界面。

工程与存档
  /Users/zhangjunhao/Library/Application Support/ForestIslandNecesse/ 是新版本独立存档。
  test-profile-* 是隔离验证数据；不用于正式游戏。
  logs/ 保存启动记录；evidence/ 与 test-profile-*/evidence/ 保存实际编译和运行证据。
  原来的星露谷小岛及其存档仍在原路径，本项目没有覆盖它们。
  Steam 的安装、账号和鉴权流程保持原样；请保持 Steam 可用。

重新构建
  python3 scripts/build_project.py
  依次重建全部游戏源码、安装源码内核、构建小岛适配和生成 Mac 应用入口。
  仅构建小岛层可运行 python3 scripts/build_mod.py。
  资源准备脚本为 scripts/prepare_runtime.py，完整提取目录为 resources-original/。

来源与范围
  原游戏、第三方库和提取资源来自本机用户购买的游戏安装；这里未作为开源项目发布。
  本地生成的源代码与资源用于当前项目的研究、修改和测试。
  UI 细化、更多建筑/居民工作安排和玩法平衡仍需在这个可运行底座上继续设计。
