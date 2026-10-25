# AI NPC 开源方案复核（2026-10-24）

当前游戏使用 Java 17 原生运行时。已有 DeepSeek 请求适配、受控游戏动作及 AI Town 记忆排序算法的局部移植；这不等于完整 AI Town 或生成式村民系统。此次复核未接入以下新依赖。

## 建议顺序

1. 先评估 AgentScope Java 的核心、OpenAI 兼容模型及事件接口，替换自建请求编排。保留游戏线程、动作权限、路径规划和任务执行器。模型事件通过队列返回游戏，不能阻塞帧循环。
2. 参考并按许可证移植 AI Town 的谈话目标、上次交谈摘要、记忆检索及反思机制。角色身份按存档与 NPC 独立；一次交流必须有明确结束和取消语义。
3. 先验证核心伙伴跨存档回忆。需要中文语义检索时再评估 Mem0 OSS，避免未经验证同时堆入三套框架。
4. 关系、承诺、信任与拒绝理由属于游戏玩法规则。模型提出意图，游戏验证条件；模型文字不能自行创建真实事件或改变物品数量。

## 核验来源与限制

| 项目 | 可复用内容 | 限制 |
| --- | --- | --- |
| [AgentScope Java](https://github.com/agentscope-ai/agentscope-java/tree/07c5f3078d682da30f29e96b6d76f68242cbc2e1) | Apache-2.0；JDK 17+；流式事件、工具调用、中断、上下文管理 | 通用代理框架，不提供现成游戏关系系统；2.x 记忆属于 Harness/应用层，旧教程接口有变化 |
| [AI Town](https://github.com/a16z-infra/ai-town/tree/8e05997f2409275669c8344b84a51692e83f3f33) | MIT；NPC 邀聊、谈话目标、总结、记忆检索、反思 | TypeScript/Convex/Pixi 架构，不能把整个游戏直接嵌入 Java；主要为双人谈话 |
| [Mem0](https://github.com/mem0ai/mem0/tree/2c81af3bbe27d9bccb09c772d583df9e9be72ee5) | Apache-2.0；长期事实提取与检索、角色隔离 | Python/TypeScript 实现；DeepSeek 负责语言模型，向量模型仍需另配；不开箱提供任务行为 |
| [Generative Agents](https://github.com/joonspk-research/generative_agents) | 感知、检索、计划、反思的研究实现 | 仿真架构及旧接口，需要较多实时游戏适配，作为参考更合适 |
| [LLMUnity](https://github.com/undreamai/LLMUnity) | Unity 本地模型、角色聊天、RAG、聊天保存 | 当前项目不是 Unity，为此换引擎得不偿失 |

[AgentScope 模型配置](https://github.com/agentscope-ai/agentscope-java/blob/07c5f3078d682da30f29e96b6d76f68242cbc2e1/docs/v2/en/docs/building-blocks/model.md)支持 DeepSeek 的兼容端点。

[AgentScope 记忆文档](https://github.com/agentscope-ai/agentscope-java/blob/07c5f3078d682da30f29e96b6d76f68242cbc2e1/docs/v2/en/docs/harness/memory.md)列出提取、合并、压缩三种独立模型调用。默认每次会话结束可能触发额外记忆提取，必须限频或按重要事件触发。当前关键词检索也不等于完善的中文语义检索。

[AI Town conversation.ts](https://github.com/a16z-infra/ai-town/blob/8e05997f2409275669c8344b84a51692e83f3f33/convex/agent/conversation.ts)与[memory.ts](https://github.com/a16z-infra/ai-town/blob/8e05997f2409275669c8344b84a51692e83f3f33/convex/agent/memory.ts)是后续拆分移植重点。

## 预期玩家体验与验收

- 玩家靠近角色，通过世界内气泡互相交谈。
- 核心伙伴在重开存档后能引用真实共同经历，不把模型杜撰内容当成游戏事实。
- 玩家说“跟我去探路”，伙伴先检查状态与权限，再进入原生跟随任务；取消或中断能生效。
- 友好村民、商人、旅人可共用接口，但只有已招募且有权限的伙伴可被安排营地任务。
- 普通角色按玩家交流/重要事件唤醒；怪物和动物继续使用原生战斗与生态行为。不会让全地图角色持续请求模型。
- 人格成长、信任变化、主动社交与承诺履约应单独实现和验收，不能仅凭接入框架宣称完成。
