# AgentScope Java model integration

Mosslume pins the official stable AgentScope Java **2.0.4** release, tag `v2.0.4`,
commit `0b6743b263ba8e39eb417e35048026f1e0196548`. The Maven Central binaries are
identified independently by SHA-256 in `agentscope-lock.json`; a moved tag or a
changed download cannot silently change a build. Core classes use Java 17 class
format (major 61), and the integration is compiled with `javac --release 17`.

`python3 scripts/fetch_agentscope_dependencies.py` verifies or downloads eight
jars into the ignored `mod/build/agentscope-libs` directory. `build_mod.py` uses
them only as compilation dependencies. `package_app.py` copies them to `Game/lib`.
The launcher must include `Necesse-source-built-1.3.3.jar:lib/*` in its Java
classpath. Libraries must **not** be merged into the mod jar: the native loader
reflects over every class in a mod, including unused SDK integrations.

Actual model calls use AgentScope's `OpenAIChatModel`, `DeepSeekFormatter`, `Msg`,
`ToolSchema`, `GenerateOptions`, and typed `ChatResponse`/`ToolUseBlock`. This is
an executed SDK path, rather than an API-compatible wrapper bearing its name.
The game supplies durable world memories and personality context at the existing
`ModelProvider.Request` boundary. Only one `request_action` schema is exposed to
recruited companions; visitors get no action tools. The model only proposes an
intent; authoritative game code still checks permissions, world revision, target
coordinates, and native task execution.

A game-owned `HttpTransport` performs the actual JDK HTTP call. It restricts the
endpoint to the official DeepSeek API (loopback is a test-only constructor seam),
limits responses to 64 KiB, preserves durable daily request/cost reservations,
and cancels in-flight HTTP work when a dialogue is closed. AgentScope is set to
one attempt and a 17-second timeout; the provider has an 18-second outer timeout.
No automatic retries, generic file/terminal tools, MCP servers, remote memory
services, telemetry exporters, or autonomous model loops are initialized here.
The real credential belongs to the transport and never enters SDK messages.

The minimal model path is deliberately narrower than all of AgentScope Core's
optional modules. It uses Jackson 2.21.1/annotations 2.21, Reactor 3.8.2 and
Reactive Streams 1.0.4. The game's existing SLF4J 1.7 API/NOP binding supplies
logging; the isolated executed model-path tests verify that compatibility.
Unused core facilities such as external vector stores, Kubernetes, Redis,
SQLite, OpenTelemetry, shell tools and MCP are not supported by this minimal
classpath and must not be registered without declaring additional dependencies.

License notices are under `mod/resources/third-party/agentscope-java` and
`mod/resources/third-party/agentscope-dependencies`. The official SDK binaries
are unmodified, and their dependency jars retain their original META-INF notices.

`DeepSeekProviderTest` uses a local HTTP fixture with a dummy credential. It
executes the actual SDK to check request formatting, typed action/chat responses,
visitor permissions, single-attempt errors, cancellation, usage settlement,
durable budgets and bounded responses. It does not load real credentials,
start the game, or call a paid model endpoint.
