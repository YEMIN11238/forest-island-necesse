# AI Town mechanisms used by Mosslume NPC minds

Upstream: [a16z-infra/ai-town](https://github.com/a16z-infra/ai-town)

Pinned revision: `8e05997f2409275669c8344b84a51692e83f3f33`. Source retrieved and read on 2026-10-24. License: MIT, copyright (c) 2023 a16z-infra; full notice is stored here as `LICENSE` and bundled in the mod at `resources/third-party/ai-town/LICENSE`.

The following unmodified files are retained to make the adaptations inspectable:

- `upstream-memory.ts`: [`convex/agent/memory.ts`](https://github.com/a16z-infra/ai-town/blob/8e05997f2409275669c8344b84a51692e83f3f33/convex/agent/memory.ts).
- `upstream-conversation.ts`: [`convex/agent/conversation.ts`](https://github.com/a16z-infra/ai-town/blob/8e05997f2409275669c8344b84a51692e83f3f33/convex/agent/conversation.ts).
- `upstream-agent.ts`: [`convex/aiTown/agent.ts`](https://github.com/a16z-infra/ai-town/blob/8e05997f2409275669c8344b84a51692e83f3f33/convex/aiTown/agent.ts).

## Actual reuse

`mod/src/localforest/ai/MemoryRanking.java` is the existing Java port of `rankAndTouchMemories`' normalized relevance + importance + hourly `0.99` recency ranking, with empty/uniform score fixes and deterministic ties.

`mod/src/localforest/ai/NpcMind.java` adds these separable mechanisms:

1. The upstream `MEMORY_ACCESS_THROTTLE = 300000` gate is applied when retrieved memories are touched in the game-thread `context` method. The pure snapshot overload deliberately does not mutate access times.
2. As in `reflectOnMemories`, new observed experiences accumulate importance before a reflection is recorded; each reflection retains exact contributing memory IDs as in `relatedMemoryIds`. Mosslume uses a smaller 24-point threshold and deterministic, explicitly labeled aggregation of actual receipts. It does **not** call or claim to reproduce upstream's LLM reflection prompt/embedding pipeline.
3. Counterpart-specific past conversations and stable identity are represented by `Relationship` and `SOCIAL` memories. Per-counterpart cooldown prevents repeated tick/duplicate transcript relationship growth, adapting `agent.ts`'s `PLAYER_CONVERSATION_COOLDOWN` gate. Native encounter eligibility, distance, movement, and bubble delivery remain the game's responsibility.
4. As in `conversation.ts`, role/identity and selected remembered context are supplied together, while historical player statements are labeled untrusted data. Player-authored claims/preferences and actual server-observed events are separate memory kinds; model replies cannot create verified facts.

## Deliberate local adaptations

- Per-NPC world-owned native `SaveData`; immutable snapshots for client/model executor readers. No Convex dependency or independent clock.
- Structured extraction of explicit player address/preferences/proposed promises, bounded to prevent recent chat from erasing meaningful experience.
- Chinese bigram and Latin word overlap supplies lexical relevance instead of a remote embedding service. This is not vector semantic search, and extraction does not understand every phrasing.
- Personality changes derive from actual successful/blocked work, injury and explicitly attributed rescue receipts, with per-type game-time cooldowns. Mere conversation gives no player trust points. Recovery alone does not credit the player with rescue.
- Hard limits: 128 memories, 8 evidence summaries, 24 counterpart relationships, 256 recent receipt IDs, with scalar finite checks and old-save defaults.
- `observeSocial` accepts completed witnessed exchanges; it does not invent social events or execute a background language-model loop.

The rest of AI Town's Convex application, frontend, movement engine, embedding database, and continuously running agent operations have not been installed or represented as integrated. This directory documents exactly the source used; no API key is stored here.

## Offline validation

`mod/tests/localforest/ai/NpcMindTest.java` uses native `SaveData` -> script -> `LoadData` parsing to verify long-term Chinese recall after many conversations, NPC identity isolation, typed evidence, immutable snapshots, trait changes, duplicate and cooldown limits, real social memory, bounded state and legacy saves. It makes no paid API calls and does not start a game.
