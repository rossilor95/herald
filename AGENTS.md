# Herald

A performant, non-bloated desktop API client (Postman alternative). Single Rust binary, native rendering, dark-themed utilitarian UI.

## Design Spec

Full design spec: `.claude/docs/superpowers/specs/2026-03-22-postman-clone-design.md`

## Tech Stack

| Component | Choice |
|---|---|
| Language | Rust (edition 2024) |
| UI framework | Dioxus (native renderer) |
| HTTP client | reqwest (async, cookie jar support) |
| Storage | SQLite via sqlx (async, runtime-checked queries) |
| Syntax highlighting | syntect |
| Secret encryption | AES-256-GCM, key in OS keychain |
| JSONPath | jsonpath-rust or serde_json_path |
| YAML export | serde_yaml |

## Architecture

Three layers, all Rust — no IPC, no serialization between layers:

```
UI Layer (Dioxus)       — components, styling, state management
Core Layer (lib)        — HTTP engine, cookies, import/export, variable/template resolution
Storage Layer (SQLite)  — collections, history, cookies, environments, secrets
```

- UI calls Core via async Rust functions.
- Core owns all business logic, is UI-agnostic and testable without Dioxus.
- Storage is accessed only through Core.

## Core Modules

- `http_engine` — wraps reqwest, no cookie jar (cookies managed manually via headers)
- `template_engine` — `{{var}}` substitution + template functions (`uuid()`, `timestamp()`, etc.) + `{{prompt(label)}}` + `{{response("Name", "$.path")}}`
- `cookie_manager` — loads/persists cookies from SQLite, injects/parses Cookie headers
- `importer` — Postman v2.1 JSON, cURL, YAML
- `exporter` — YAML interchange format (secrets excluded)
- `storage` — all SQLite CRUD, encryption/decryption transparent to callers

## Data Model

- **Collection** — named group, nestable via `parent_id` for folders
- **Request** — method, URL, headers, body, query params, auth, tags; belongs to collection
- **Environment** — named set of variables
- **Variable** — key + value + enabled + `is_secret` (encrypted at rest)
- **HistoryEntry** — resolved request + response snapshot (max 1000, auto-pruned)
- **Cookie** — global, scoped by domain

## Key Decisions

- Requests store raw templates; resolution at execution time
- History stores resolved values (what was actually sent/received)
- `request_id` nullable on history (ad-hoc/deleted requests preserve history)
- Cookies are global per-domain, not per-collection
- sqlx runtime-checked queries (no `DATABASE_URL` at build time)
- Migrations embedded via `sqlx::migrate!()`
- Secret encryption key in OS keychain (macOS Keychain, Linux libsecret, Windows DPAPI)

## Storage

App name: `rattler`. Database: single SQLite file in OS app data dir.
- macOS: `~/Library/Application Support/rattler/data.db`
- Linux: `~/.local/share/rattler/data.db`
- Windows: `%APPDATA%\rattler\data.db`

## UI

- Dark theme, monospace data (JetBrains Mono), sans labels (DM Sans)
- Vertical split default (request left, response right), toggleable to horizontal
- Sidebar: collection tree, history, environment selector
- Method colors: GET=green, POST=blue, PUT=amber, DELETE=red, PATCH=purple
- Status codes: semantic colors (2xx green, 3xx amber, 4xx/5xx red)
- Response viewer: Pretty (syntax-highlighted) and Tree (collapsible JSON) modes
- Dioxus signals for reactive state

## Build & Run

```bash
cargo build          # debug build
cargo run            # run
cargo test           # tests
cargo build --release # release build
```

## Learning Goal

This is a **learn-by-building** project. The developer is learning Rust while building Herald. AI assistants should:

- Explain **why** a Rust pattern or idiom is used, not just what it does
- Call out ownership, borrowing, and lifetime decisions when they matter
- Flag when a simpler approach exists but the more idiomatic Rust way is worth learning
- Briefly note relevant Rust concepts (e.g., trait objects vs generics, `Result` vs `Option`, `async` mechanics) as they come up naturally in the code being written

Keep explanations concise and contextual — tied to the code at hand, not abstract lectures.

## Conventions

- All modules expose plain async functions — no traits unless there's a second implementation
- Core has zero knowledge of Dioxus
- Fonts bundled via `include_bytes!` (both SIL OFL 1.1 — include license text)
- Large responses: >500KB disable highlighting, >1MB truncate with "show full"
