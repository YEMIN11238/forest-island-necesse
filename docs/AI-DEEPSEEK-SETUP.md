# DeepSeek NPC 本机接入（0.4.0-r2）

2026-10-23，用户选择 DeepSeek 并提供开发 Key。本版将现有 `ModelProvider` 接到官方 `https://api.deepseek.com/chat/completions`，默认 `deepseek-flash`、关闭思考模式、最多输出 600 tokens。使用 Java 17 异步 HTTP；JSON 编码/解析复用 MIT minimal-json 0.9.5，原有记忆排序继续使用 AI Town 的 Java 移植。未引入完整 AI Town 服务或模型 SDK。

## 本机配置

配置位于 `~/Library/Application Support/Mosslume-0.4/ai/`，文件夹权限 0700，Key 文件 0600：

- `deepseek.key`：仅保存本机 Key，不打包、不写入存档，不提交 Git。
- `deepseek.properties`：启用开关、模型及用量限制。
- `usage-YYYY-MM-DD.jsonl`：按北京时间日期记录调用预约、token 用量和保守费用估算，不记录对话或 Key。

默认配置为：

```properties
enabled=true
model=deepseek-flash
dailyBudgetRmb=5
dailyRequests=200
maxOutputTokens=600
```

将 enabled 改为 false 后重启可回到离线任务。模型未配置时不会自动发现环境变量中的其他账户凭据。测试可显式设置 `mosslume.ai.directory` 指向隔离配置；生产请求地址固定为 DeepSeek 官方 HTTPS，禁止跟随重定向。

## 游戏里的使用方式

保存并退出已经运行的旧窗口，重新打开 `~/Applications/Mosslume 0.4 Preview.app`。展开左侧“伙伴与营地”，切换到目标伙伴，输入文字并发送。请求期间发送按钮禁用；取消、切换伙伴、关闭面板、暂停或离开会话会作废相关回复，尽可能取消仍在进行的网络请求。

可尝试“跟着我走”“守卫这里”“回住所”“恢复日常工作”“收一处成熟作物”“搬运一批”“取消当前指令”。普通询问只返回对话。模型最多提出一项任务，守卫只能使用该次指令提供的玩家坐标，任务仍需通过游戏的权限、状态和路径检查。回复表达行动意图，实际是否完成以任务状态和真实事件为准。

模型请求携带伙伴身份、当前状态、双方坐标和筛选后的真实事件。尚未接入闲置时自主思考、连续多步规划、完整对话长期记忆、建造、指定数量及指定仓库交付；这些不是填入 Key 后自动获得的功能。

## 用量口径

默认每自然日最多 200 次请求，且预约后的保守估算不超过 5 元。发送前按请求 UTF-8 字节数加协议余量、最大输出长度预约费用，成功返回 token 用量后结算；未知结果保留预约，不自动重试。用量文件加锁并持久化，重启不能清零限制。

价格以 2026-10-23 官方 Flash 高峰价（输入未命中缓存 2 元/百万 token、输出 8 元/百万 token）估算，忽略缓存及空闲时段折扣，因此不等于账户实际账单。价格变更需要同步配置代码；最终收费以 DeepSeek 控制台为准。取消已发送的请求也可能产生费用。

## 验证

`DeepSeekProviderTest` 使用本机 HTTP fixture，覆盖聊天/动作解析、非法动作/坐标、截断、调用与费用限制的重启保持、请求取消、错误脱敏及响应大小上限，不读取真实 Key、不产生付费调用。

原生检查需显式运行 `python3 scripts/run_refactor_probe.py --case <新名称> --ai`，使用独立 QA 存档并最多发出三次真实请求，分别验证自然语言跟随、取消及聊天不改变任务。默认回归不会调用付费模型。物理鼠标/键盘输入与长期自主行为不在此检查口径内。

原有帧率未达标的问题仍保留，本次模型接入没有把性能门槛标为通过。

接口与价格来源：https://api-docs.deepseek.com/api/create-chat-completion/ ，https://api-docs.deepseek.com/zh-cn/quick_start/pricing/ 。

实测结果：独立 QA 存档中的三次真实 Flash 请求通过。原生跟随字段匹配玩家，取消后跟随和守卫字段均清空；聊天保留任务及小禾身份。输入 2364、输出 238 tokens，按上述高峰价估算 0.006632 元。相关证据在 `mosslume/validation/deepseek-0.4-r2/`。之后只调整状态栏额度文案和探针字段可见性，没有追加付费验证。
