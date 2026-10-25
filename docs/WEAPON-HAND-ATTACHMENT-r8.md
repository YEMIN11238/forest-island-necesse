# r8 人物攻击握持接入

本轮保持 AI 对话、服务器命中/伤害、原生攻击时间与动作进度不变。问题在于新身体是 64px 画布、脚点 32,52、2 倍渲染，旧武器渲染仍把手当作原生身体的臂长轨道。单改一个平移量无法同时修复各人物、四方向与六个身体动作帧。

## 实现

- `AttackHandSockets` 读取已复核的每角色、每方向、每攻击帧的可见手掌坐标。当前覆盖主角、阿朔、鹿安、农夫、木匠、商人，共 144 个握持点。以身体本帧与同一个插值/相机脚点计算屏幕位置，因此边移动边攻击时身体和工具不会采用两套坐标。
- `CharacterAnimation.weapon` 继续调用实际进行中的原生物品 `getAttackDrawOptions`。保留物品自己的握柄坐标、攻击纹理、色彩、旋转、弓蓄力拉伸和多层绘制；不换成背包图标。
- 原生 `ItemAttackDrawOptions.forEachItemSprite(itemRawCoords)` 能把物品自己的 `itemRotatePoint` 用作旋转枢轴。清除属于旧身体的臂长、推手和身体偏移，再让每个方向的原生枢轴基准落到当前手掌。
- 四个原生 raw 枢轴基准来自 `ItemAttackDrawOptions.pos` 的实际四分支：UP=(6,22)、RIGHT=(-4,5)、DOWN=(-10,18)、LEFT=(4,5)。这些是底模绘制接口的基准；人物差异来自逐帧素材元数据。
- 只绘制一份真实武器。北向在身体后，其他方向在身体前；随后复用原始身体本帧掌心小片覆盖手柄，让手掌在手柄前。没有另画原生身体或手臂。
- 弓使用该角色前伸持物姿势（攻击第 3 帧）；实际蓄力、弓身拉伸和放箭继续采用原生处理，避免给弓播放举斧动作。专门的双手拉弦动画尚未制作。
- 农夫、木匠、商人的新整套人物图保持身份一致。旧批次 work/carry/sleep/downed 图片已停用，相关状态使用同人物方向 idle 与现有睡倒旋转，不再换成另一个人。全部五套 NPC idle/walk/attack 为必需资源，缺失直接在启动时报告。

## 数据和复核

- 标点原档：`mosslume/art/hand-grips-r8/hand-sockets.json`
- 原档记录每张实际 strip 的 SHA-256；素材替换后必须重新复核握持点。
- 运行时导出：`mod/resources/mosslume/characters/attack-hand-sockets.properties`
- 机械导出脚本：`mosslume/scripts/export_hand_sockets_r8.py`
- 六张绿色标点合表：`mosslume/art/hand-grips-r8/qa/`
- 全局检查应纳入：`localforest.visual.AttackHandSocketsTest`

定向测试采用真正的原生斧类、木剑类、木弓类和原生 `ItemAttackDrawOptions.pos` 产生的几何四边形，没有用另写的旋转函数代替原生渲染。验证 144 个手点均落在当前素材的可见像素，元数据与素材 hash 对应；960 组覆盖六人物、四朝向、八攻击进度、弓半/满蓄力和移动脚点。原生几何握柄与指定手点误差小于 0.02px，物品旋转不变，每次仅一个武器且没有原生手臂/身体。`CharacterAnimationTest` 亦通过。证据在 `mosslume/art/hand-grips-r8/targeted-checks.json`。

这些是离线原生几何与资源测试，不代表已经在本次游戏窗口中验证。最终还需主流程用隔离 QA 存档录制真实攻击和确认正常游玩存档重载。

## 限制

少数原生后期特殊武器（例如自行替代整个绘制接口的光束/爪/传送门复合包装）返回的不是 `ItemAttackDrawOptions`。本轮保留其原生行为，并记录 `weaponCounters().bespokeNativeWrappers`；尚未对这些接口做逐层手位适配。普通斧、剑、弓采用新绑定链。每个攻击图仍为六个离散身体帧；本轮没有虚构双手弓箭动作，也没有重制主角攻击源图。

## 可选实际窗口矩阵夹具

`WeaponMatrixProbe` 由独立属性 `mosslume.visualProbe.weaponMatrix=true` 开启，强制要求 `sceneOnly=true` 且 profile 名以 `Mosslume-QA-` 开头。普通游戏与旧 `attackPoses` 不会触发。完整矩阵使用 18 秒 capture，12 步、每步 1.25 秒。

- 服务端只在可丢弃世界给玩家放入真实木斧、木剑、木弓与箭；客户端调用真实 `tryAttack`，逐一覆盖三物品和四方向。
- 新建五个 `shouldSave=false` 原生人形 NPC，分别采用五套人物外观，原生 guard 站在无碰撞的干燥地面。它们通过原生 `attackItem` 网络能力播放实际工具攻击。此部分只证明客户端原生能力、身体与武器绘制链，不能据此声称 AI 自发战斗或 NPC 命中/伤害已测。
- 不改地形、不修改正式存档、不调用模型。矩阵会改变其 QA 世界的人口和玩家装备，所以不要把其性能和居民生活样本当作纯自然场景的对照。
- 输出 `weapon-matrix.json`，列出 12 个玩家接受步骤、实际观察到的武器/方向、五 NPC 的 60 个武器/方向组合与真正的绘制计数，缺项明确列入 `missing`，不会把未观察到的组合当成通过。
- `weapon-player-*.png` 和 `weapon-npc-*.png` 为从真实已呈现 framebuffer 裁剪的角色近景，没有另画人物。读取的是上一张已呈现画面，动作状态元数据可能领先截图一帧。

主流程可使用 `scripts/run_refactor_probe.py --weapon-matrix --scene-only --preset continuous` 的独立 case；最终原生执行与图片验收结果由主流程追加，本分工未自行启动游戏。

## 原版头像漏换补齐与身体审计

只读检查确认，普通 HumanShop 职业、商人和旅人的场景身体继承 `HumanMob.addDrawables`，由现有适配器接管；未知职业也会使用当前 carpenter 家族，不会因未知职业名而自动退回原版身体。Elder 的愚人节特殊身体另有分支，但本次 10 月日期不触发。`StealthBasketHumanMob` 属于 cave critter 的拟态角色，非村民，本轮不把它强行替成村民。

确凿漏换的是原生小地图/世界地图头像、指令和对话头像、社交思考泡泡头像，以及居民列表的在线与离线头像。新增 `ResidentPortraits` 直接读取当前同角色 `idle-down` 图片的上部头片，保留原像素和透明度，再装入原生兼容 head atlas；不调用旧 HumanLook、旧皮肤、旧眼睛或旧衣服，也不重新生图。`ResidentPortraitPatches` 接回五个实际原生方法入口。

`ResidentPortraitsTest` 在真实内核上核对五个方法签名，并使用原生 `Settler.getHumanFaceDrawOptions` 验证五族 × 四尺寸共 20 组几何；每组仅一张自有头片纹理，无原版眼睛/身体。头片 QA 在 `qa/current-resident-heads.png`，定向检查在 `portrait-checks.json`。

矩阵 QA 另有 `WeaponMatrixHudPatch`，仅显式可丢弃 QA 档左下角绘制五角色的 command/chatter/map 三行原生返回头像，可在真实 `full-frame.png` 检查。矩阵 JSON 的 `nativePortraitHudFrames` 是该实际 HUD 的呈现次数；`npcRenderAtCloseups` 则在每张角色近景处记录附近每个 mob ID 的实际身体纹理、状态、帧和坐标，防止凭截图猜测把自有新素材误认为原版。该部分实际窗口检查由主流程执行，本分工未启动游戏。

头像 QA 面板仅在 CAPTURE 开始、首张 `full-frame.png` 尚未读取前绘制约 2–3 帧，WARMUP、无 readback 的 PERFORMANCE 以及后续攻击连续录屏均不绘制，避免把调试面板混入性能验收。`nativePortraitHudTiming` 记录各真实端点和整体 HUD 的 CPU 提交耗时及 shader identity 变化；计时没有 GPU 同步调用。生产头像贴图只初始化一次，原生 `HumanDrawOptions` 每次请求独立创建，避免复用可变的 UI 大小、位置、护甲状态。
