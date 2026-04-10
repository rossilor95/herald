# Herald — KMP Design Spec

## Overview

A performant, non-bloated desktop API client (Postman alternative). Cross-platform: macOS, Linux, Windows. Dark themed UI inspired by Postman's information architecture and JetBrains Fleet's aesthetic.

Supersedes the previous Rust/Dioxus design spec (`2026-03-22-postman-clone-design.md`).

## Tech Stack

| Component | Choice |
|---|---|
| Language | Kotlin 2.3.20 |
| Build system | Gradle 9.4.1 with Kotlin DSL, KMP plugin |
| UI framework | Compose Multiplatform (desktop) |
| HTTP client | Ktor Client |
| Storage | SQLDelight (JVM SQLite driver) |
| Syntax highlighting | Custom, via Compose `AnnotatedString` |
| Serialization | kotlinx-serialization |
| Async | kotlinx-coroutines |

## Project Structure

Two-module Gradle project (extract `:storage` as a third module later):

- **`:shared`** — KMP library module. Core business logic, data model, HTTP engine, variable resolution, storage (SQLDelight). Source sets: `commonMain`, `jvmMain`.
- **`:desktop`** — Compose Multiplatform application module. Depends on `:shared`. All UI components, state management, theming.

## Architecture

```
Desktop Module (Compose)   — components, theming, state management
Shared Module (KMP lib)    — HTTP engine, variable resolution, storage, domain model
```

- Desktop calls Shared via suspend functions and StateFlow.
- Shared owns all business logic, is UI-agnostic and testable without Compose.
- Storage is accessed only through Shared.

## Storage

App name: **herald**. Database: single SQLite file in OS app data dir.
- macOS: `~/Library/Application Support/herald/data.db`
- Linux: `~/.local/share/herald/data.db`
- Windows: `%APPDATA%\herald\data.db`

## Data Model

### Entities

**Collection**
- `id` (Long, auto-increment primary key)
- `name` (String)
- `created_at` (Long, epoch millis)
- `updated_at` (Long, epoch millis)

**Folder**
- `id` (Long, auto-increment primary key)
- `collection_id` (Long, FK to Collection)
- `name` (String)
- `seq` (Int, ordering within the collection)
- `created_at` (Long, epoch millis)
- `updated_at` (Long, epoch millis)

**Method**
- `name` (String, primary key: GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS)

**Request**
- `id` (Long, auto-increment primary key)
- `collection_id` (Long, FK to Collection)
- `folder_id` (Long, nullable FK to Folder. Null = lives at collection root)
- `name` (String)
- `method` (String, FK to Method)
- `url` (String, raw template with `{{variables}}`)
- `headers` (String, JSON-serialized list of name/value/enabled triples)
- `query_params` (String, JSON-serialized list of name/value/enabled triples)
- `body_type` (String, nullable: "raw" or "json". Null = no body)
- `body_content` (String, nullable)
- `seq` (Int, ordering within parent folder or collection root)
- `created_at` (Long, epoch millis)
- `updated_at` (Long, epoch millis)

**Environment**
- `id` (Long, auto-increment primary key)
- `name` (String)
- `created_at` (Long, epoch millis)
- `updated_at` (Long, epoch millis)

**Variable**
- `id` (Long, auto-increment primary key)
- `environment_id` (Long, FK to Environment)
- `key` (String)
- `value` (String)
- `enabled` (Boolean)

**HistoryEntry**
- `id` (Long, auto-increment primary key)
- `request_id` (Long, non-nullable FK to Request)
- `method` (String, FK to Method)
- `resolved_url` (String)
- `request_headers` (String, JSON-serialized, resolved)
- `request_body` (String, nullable, resolved)
- `response_status` (Int)
- `response_headers` (String, JSON-serialized)
- `response_body` (String, nullable)
- `duration_ms` (Long)
- `created_at` (Long, epoch millis)

### Relationships

```
Collection 1──N Folder
Collection 1──N Request
Folder 1──N Request
Environment 1──N Variable
Request 1──N HistoryEntry
```

### Key Decisions

- Single-level folder nesting only: Collection > Folder > Requests. No folders within folders.
- Requests can live at collection root (outside any folder) or inside a folder.
- Requests store raw templates; resolution at execution time.
- HistoryEntry stores resolved values (what was actually sent/received) plus response data.
- `request_id` on HistoryEntry is non-nullable. Deleting a request cascade-deletes its history.
- History capped at 100 entries, pruned on insert via cascade deletes.
- Method is a lookup table, pre-populated with seven standard methods.
- Headers and query params on Request are JSON-serialized (name/value/enabled triples).

## Shared Module Architecture

### Packages

**`dev.herald.core.http`** — HTTP engine.
Wraps Ktor Client with the CIO engine (pure Kotlin, no external dependencies). Takes a fully resolved request (URL, headers, body), executes it, returns response (status, headers, body, timing). No variable resolution here. Ktor engine configured in `jvmMain`.

**`dev.herald.core.variable`** — Variable resolution.
Takes a raw template string and an environment's variable map, returns the resolved string. Scans for `{{variableName}}` patterns, looks up values, substitutes. Returns a result indicating success or listing unresolved variables (which blocks sending).

**`dev.herald.core.model`** — Domain model.
Kotlin data classes: `Collection`, `Folder`, `Request`, `Environment`, `Variable`, `HistoryEntry`, `Method`. Pure data, no logic. Shared between storage and UI layers.

**`dev.herald.storage`** — SQLDelight database access.
`.sq` files defining all tables and queries. Database driver wired in `jvmMain` via expect/actual. CRUD operations for all entities. History pruning on insert. Migrations embedded and run on startup.

**`dev.herald.core.executor`** — Request execution orchestrator.
Takes a raw Request + active Environment, resolves variables, calls HTTP engine, persists history entry. Returns either success (with response) or failure (unresolved variables, network error).

### Request Execution Data Flow

```
User hits Send
  → executor receives raw Request + active Environment
  → variable resolver substitutes {{placeholders}} in URL, headers, query params, body
  → if unresolved variables remain → return error (block send)
  → HTTP engine sends resolved request via Ktor
  → executor persists HistoryEntry with resolved request + response
  → return result to UI
```

## Desktop Module Architecture

### State Management

Compose `MutableState` and `StateFlow` for reactive UI. A top-level `AppState` class holding:
- Open tabs (list of tab descriptors, each referencing a request ID or an unsaved draft)
- Active tab index
- Active environment ID (nullable, null = no environment selected)
- Sidebar collapse/expand state

Each tab references a request. No duplicate tabs allowed (clicking an already-open request switches to its tab). Tabs are session-only, not persisted across restarts. Closing a tab with unsaved changes shows a confirmation dialog.

### UI Components

- **`App`** — root composable. Lays out sidebar + main content area.
- **`Sidebar`** — collection tree + history list + environment selector dropdown.
  - **`CollectionTree`** — renders collections, folders, requests. Expand/collapse. Click to open in tab. Right-click context menu for rename/delete/new request/new folder.
  - **`HistoryList`** — flat chronological list, newest first. Click opens read-only view.
  - **`EnvironmentSelector`** — dropdown at bottom of sidebar.
- **`TabBar`** — horizontal tab strip. Close button per tab, unsaved indicator, click to switch.
- **`RequestEditor`** — method dropdown, URL input, sub-tabs for headers/query params/body. Reusable key-value editor widget (name, value, enabled checkbox, add/remove rows).
- **`ResponseViewer`** — status/time/size bar, Pretty and Raw toggle, body display. Pretty mode uses custom `AnnotatedString` highlighting.
- **`HistoryDetailView`** — read-only view of a history entry. Shows resolved request and response. Replay button re-sends the same resolved request.
- **`PromptDialog`** — generic confirmation/input dialog for unsaved changes, rename, delete.

### Layout

```
┌──────────┬───────────────────────────────────────┐
│ Sidebar  │ TabBar: [GET /users] [POST /login] [+] │
│          ├───────────────────────────────────────┤
│ ▸ MyAPI  │                                       │
│   ▸ Auth │   RequestEditor | ResponseViewer      │
│   GET /  │   (side by side, vertical split)      │
│   POST / │                                       │
│          │                                       │
│ History  │                                       │
│  12:03 ← │                                       │
│  12:01 ← │                                       │
│          │                                       │
│ ● dev    │                                       │
└──────────┴───────────────────────────────────────┘
```

### Theming

Dark theme inspired by Postman (information architecture) and JetBrains Fleet (aesthetic).
- Near-black background, minimal chrome, high contrast, refined typography.
- Monospace for data: JetBrains Mono (bundled). Sans-serif for labels: DM Sans (bundled). Both SIL OFL 1.1 licensed.
- Method colors: GET=green, POST=blue, PUT=amber, DELETE=red, PATCH=purple, HEAD/OPTIONS=muted gray.
- Status code colors: 2xx=green, 3xx=amber, 4xx/5xx=red.

## Error Handling

- **Network errors** (timeout, DNS, TLS, connection refused): displayed inline in response viewer with error type and message. No modals.
- **Unresolved variables**: block sending. Request editor highlights unresolved `{{variables}}` and lists which are missing.
- **Invalid input**: inline validation with red indicators plus descriptive text (not color alone). Non-blocking.
- **Large responses**: above 500KB, syntax highlighting disabled (raw text only). Above 1MB, body truncated with "show full" button.
- **Database errors**: non-blocking toast/notification. Failed operations leave UI in previous state.

## Accessibility

- **Keyboard navigation**: all interactive elements reachable and operable via keyboard. Tab order follows visual layout. Focus indicators visible and high-contrast.
- **Semantic structure**: proper content hierarchy. Screen reader announcements for state changes (request sent, error occurred, tab switched).
- **Color independence**: all information conveyed by color also conveyed via text labels or icons. Never color alone.
- **Contrast**: minimum WCAG AA contrast ratios (4.5:1 normal text, 3:1 large text) across the dark theme.
- **Focus management**: predictable focus movement on tab open/close. Dialogs trap focus. Closing dialog returns focus to trigger.
- **Resizability**: UI handles window resizing gracefully. Text respects system font size preferences where possible.
- **Compose semantics**: use `contentDescription`, `Role`, `stateDescription` modifiers to expose meaning to platform accessibility APIs.

## V1 Features

1. HTTP request builder (method, URL, headers, query params, body: raw text + JSON)
2. Response viewer (Pretty with custom syntax highlighting, Raw mode)
3. Collections with single-level folders
4. Environments with `{{variable}}` substitution (one active, unresolved blocks sending)
5. Tabs (no duplicates, unsaved changes dialog, session-only)
6. Request history (full snapshot, 100 entry cap, read-only + replay)
7. Basic sidebar tree with context menus (no drag-and-drop)
8. Dark theme (Postman + Fleet inspired)
9. Accessible (keyboard nav, color independence, WCAG AA contrast, Compose semantics)

## Deferred Features

- Auth config (Bearer, Basic)
- Unlimited/deeper folder nesting
- Drag-and-drop reordering in sidebar
- Import (Postman JSON, cURL, YAML)
- Export (YAML interchange)
- Cookie management
- Template functions (`uuid()`, `timestamp()`, etc.)
- Prompt variables (`{{prompt(label)}}`)
- Response chaining (`{{response("Name", "$.path")}}`)
- Secret variables (encrypted at rest)
- Tags and filtering
- Split layout toggle (vertical/horizontal)
- JSONPath response body filtering
- SOAP/XML support
- Collapsible JSON tree view
- History filtering/search
- Tab persistence across restarts
- Editable history entries
- Three-module split (extract `:storage` from `:shared`)

## Learning Goal

This is a **learn-by-building** project. The developer is experienced in Kotlin but learning Kotlin Multiplatform and Compose Multiplatform. AI assistants should:

- Explain KMP-specific patterns (expect/actual, source sets, platform-specific driver wiring) when they come up
- Call out Compose idioms (state hoisting, recomposition, side effects, remember vs derivedStateOf) as they appear naturally in the code
- Flag when a simpler approach exists but the more idiomatic Compose/KMP way is worth learning
- Briefly note relevant concepts (coroutine scoping in Compose, StateFlow vs mutableStateOf, modifier chains) tied to the code being written

Keep explanations concise and contextual.
