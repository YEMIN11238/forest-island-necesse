# AI Town social lifecycle port

Upstream: https://github.com/a16z-infra/ai-town

Pinned revision: `8e05997f2409275669c8344b84a51692e83f3f33`

License: MIT, Copyright (c) 2023 a16z-infra. The unchanged license is included in this directory as `LICENSE`.

Verified upstream sources on 2026-10-24:

- https://github.com/a16z-infra/ai-town/blob/8e05997f2409275669c8344b84a51692e83f3f33/convex/aiTown/agent.ts
- https://github.com/a16z-infra/ai-town/blob/8e05997f2409275669c8344b84a51692e83f3f33/convex/aiTown/conversation.ts
- https://github.com/a16z-infra/ai-town/blob/8e05997f2409275669c8344b84a51692e83f3f33/convex/agent/conversation.ts
- https://github.com/a16z-infra/ai-town/blob/8e05997f2409275669c8344b84a51692e83f3f33/convex/constants.ts

`NpcSocialPlanner.java` adapts the upstream invite/accept, walking-over/midpoint, nearby participation, alternating messages and leave sequence; one conversation reservation per participant; per-person/per-pair cooldowns; message and invitation/action timeouts. The TypeScript/Convex scheduling and movement calls were replaced with immutable Java snapshots and bounded action outputs, consumed on the game server thread. This is a specific algorithmic port, not the full AI Town server or its autonomous LLM loop.

Game-specific adaptations: native job/combat/injury/player-conversation protection; movement eligibility; deterministic personality-weighted invitation acceptance; observed rescue/work-event priorities; one night reminder per world day; global speech/start budgets; bounded ephemeral event deduplication; local fallback sentences. Native movement permissions and path validity remain the responsibility of the game adapter. Conversation sessions are intentionally ephemeral; genuine events and memories are persisted elsewhere.

The upstream prompt preparation in `convex/agent/conversation.ts` informed the separation of speaker/peer identity, topic and observed memory data. No upstream prompts were copied into local fallback lines. A model response is optional and cannot authorize gameplay actions.

`NpcBehaviorPolicy.java` is game-specific original integration code. It maps persisted personality and actual health/risk observations to a bounded allow/decline/rescue-request decision; all native server command checks still apply.
