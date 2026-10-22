# AI Town memory ranking port

Source: https://github.com/a16z-infra/ai-town/blob/8e05997f2409275669c8344b84a51692e83f3f33/convex/agent/memory.ts

License: MIT, Copyright (c) 2023 a16z-infra. The bundled notice is `mod/resources/third-party/ai-town/LICENSE`.

`localforest.ai.MemoryRanking` ports the normalized relevance + importance + recency ranking from `rankAndTouchMemories` to Java. It preserves the 0.99 hourly recency decay, adds deterministic ties, rejects non-finite scores and fixes empty/uniform score ranges. The caller supplies a game clock and already authorized, observed events. It does not install Convex, call an embedding model or claim that AI Town's complete agent system is integrated.

The user deferred model selection on 2026-10-23. `ModelProvider` is an offline-first boundary; a future LangChain4j adapter can implement it without changing native tasks or saved memories. No model SDK or paid provider is enabled in this build. Natural-language action proposals must still pass the native task adapter's session, revision, bounds and permission checks.
