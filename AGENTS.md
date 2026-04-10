# Herald

A performant, non-bloated desktop API client (Postman alternative). Cross-platform (macOS, Linux, Windows). Dark themed UI inspired by Postman and JetBrains Fleet.

## Design Spec

Full design spec: `.claude/docs/superpowers/specs/2026-04-10-herald-kmp-design.md`

## Tech Stack

| Component | Choice |
|---|---|
| Language | Kotlin 2.1.x (upgrade to 2.3 when stable) |
| Build system | Gradle 8.10 with Kotlin DSL, KMP plugin |
| UI framework | Compose Multiplatform (desktop) |
| HTTP client | Ktor Client (CIO engine) |
| Storage | SQLDelight (JVM SQLite driver) |
| Syntax highlighting | Custom, via Compose `AnnotatedString` |
| Serialization | kotlinx-serialization |
| Async | kotlinx-coroutines |

## Architecture

Two-module Gradle project:

```
Desktop Module (Compose)   — components, theming, state management
Shared Module (KMP lib)    — HTTP engine, variable resolution, storage, domain model
```

- Desktop calls Shared via suspend functions and StateFlow.
- Shared owns all business logic, is UI-agnostic and testable without Compose.
- Storage is accessed only through Shared.

## Shared Module Packages

- `dev.herald.core.http` — wraps Ktor Client (CIO engine), executes resolved requests
- `dev.herald.core.variable` — `{{variable}}` substitution, returns success or lists unresolved vars
- `dev.herald.core.model` — Kotlin data classes (Collection, Folder, Request, Environment, Variable, HistoryEntry, Method)
- `dev.herald.storage` — SQLDelight schema, CRUD, history pruning, expect/actual driver wiring
- `dev.herald.core.executor` — orchestrates resolve → send → persist flow

## Data Model

- **Collection** — named group of requests
- **Folder** — single-level nesting within a collection (no folders-in-folders)
- **Method** — lookup table (GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS)
- **Request** — method, URL template, headers (JSON), query params (JSON), body (raw/JSON), belongs to collection, optionally to folder
- **Environment** — named set of variables
- **Variable** — key + value + enabled, belongs to environment
- **HistoryEntry** — full snapshot of resolved request + response, non-nullable FK to request, cascade-deletes on request removal, capped at 100

## Key Decisions

- Requests store raw templates; resolution at execution time
- Unresolved variables block sending (not sent as-is)
- History stores resolved values (what was actually sent/received)
- Deleting a request cascade-deletes its history
- History capped at 100 entries, pruned on insert
- One active environment at a time
- No duplicate tabs; closing unsaved tabs prompts confirmation
- Tabs are session-only (not persisted across restarts)

## Storage

App name: `herald`. Database: single SQLite file in OS app data dir.
- macOS: `~/Library/Application Support/herald/data.db`
- Linux: `~/.local/share/herald/data.db`
- Windows: `%APPDATA%\herald\data.db`

## UI

- Dark theme, Postman layout + Fleet aesthetic
- Monospace data (JetBrains Mono), sans labels (DM Sans), both bundled (SIL OFL 1.1)
- Method colors: GET=green, POST=blue, PUT=amber, DELETE=red, PATCH=purple, HEAD/OPTIONS=muted gray
- Status codes: 2xx=green, 3xx=amber, 4xx/5xx=red
- Response viewer: Pretty (custom `AnnotatedString` highlighting) and Raw modes
- Accessible: keyboard nav, WCAG AA contrast, color independence, Compose semantics

## Build & Run

```bash
./gradlew :desktop:run          # run
./gradlew :shared:test          # test shared module
./gradlew :desktop:packageDmg   # macOS package
```

## Learning Goal

This is a **learn-by-building** project. The developer is experienced in Kotlin but learning Kotlin Multiplatform and Compose Multiplatform. AI assistants should:

- Explain KMP-specific patterns (expect/actual, source sets, platform-specific driver wiring) when they come up
- Call out Compose idioms (state hoisting, recomposition, side effects, remember vs derivedStateOf) as they appear naturally in the code
- Flag when a simpler approach exists but the more idiomatic Compose/KMP way is worth learning
- Briefly note relevant concepts (coroutine scoping in Compose, StateFlow vs mutableStateOf, modifier chains) tied to the code being written

Keep explanations concise and contextual.

## Conventions

- All shared modules expose suspend functions (no traits/interfaces unless there's a second implementation)
- Shared has zero knowledge of Compose
- Fonts bundled via Compose resource system (both SIL OFL 1.1, include license text)
- Large responses: >500KB disable highlighting, >1MB truncate with "show full"
