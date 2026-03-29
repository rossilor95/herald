# Postman Clone — Design Spec

## Overview

A performant, non-bloated desktop API client. Single Rust binary, native rendering, dark-themed utilitarian UI. Cross-platform: macOS, Linux, Windows.

## Tech Stack

| Component | Choice |
|---|---|
| Language | Rust |
| UI framework | Dioxus (native renderer) |
| HTTP client | reqwest (async, cookie jar support) |
| Storage | SQLite via sqlx (async SQLite driver) |
| Syntax highlighting | syntect (TextMate grammars, read-only highlighting) |
| Secret encryption | AES-256-GCM, key stored in OS keychain |
| JSONPath | jsonpath-rust or serde_json_path |
| YAML serialization | serde_yaml (for export format) |

## Architecture

Three layers, all Rust:

```
UI Layer (Dioxus)       — components, styling, state management
Core Layer (lib)        — HTTP engine, cookies, import/export, variable/template resolution
Storage Layer (SQLite)  — collections, history, cookies, environments, secrets
```

- UI calls Core via async Rust functions (no IPC, no serialization).
- Core owns all business logic and is UI-agnostic (testable without Dioxus).
- Storage is accessed only through Core.

## Storage Architecture

### Single-store SQLite

All data lives in a single SQLite database in the OS app data directory:
App name: **`rattler`** (working title). Database paths:
- macOS: `~/Library/Application Support/rattler/data.db`
- Linux: `~/.local/share/rattler/data.db`
- Windows: `%APPDATA%\rattler\data.db`

### Secret encryption

Secret variable values are encrypted at rest in SQLite using AES-256-GCM. The encryption key is generated on first launch and stored in the OS keychain:
- macOS: Security framework (Keychain)
- Linux: libsecret (GNOME Keyring / KDE Wallet)
- Windows: DPAPI (Credential Manager)

Regular variables are stored as plain text. Secret variables have an `is_secret` flag — their `value` column holds the encrypted ciphertext.

### YAML export/import

Collections can be exported to a folder of YAML files for git versioning and sharing. The YAML format is an interchange format, not the primary storage.

**Export behavior:**
- Entire collection exported as a **single YAML file** (e.g. `my-collection.yaml`)
- Folder hierarchy represented as nested YAML objects
- Environments included as a top-level `environments` key
- Secret variable **names** are listed but **values are omitted** (never exported)
- Tags, ordering, auth config, and all request metadata are preserved

**Export file structure:**
```yaml
name: my-collection
environments:
  - name: dev
    variables:
      - name: base_url
        value: "http://localhost:8080"
        enabled: true
      - name: api_key
        secret: true
        enabled: true
folders:
  - name: users
    seq: 1
    requests:
      - name: get-users
        method: GET
        url: "{{base_url}}/users"
        headers:
          - name: Accept
            value: application/json
            enabled: true
        tags: [crud, smoke]
      - name: create-user
        method: POST
        url: "{{base_url}}/users"
        body:
          mode: json
          content: '{"name": "John"}'
  - name: auth
    requests:
      - name: login
        method: POST
        url: "{{base_url}}/auth/login"
        auth:
          type: basic
          username: "{{user}}"
          password: "{{pass}}"
requests: []  # top-level requests (not in any folder)
```

**Import behavior:**
- Reads a single YAML file into SQLite, creating collections/requests/environments
- Prompts user for secret variable values that are missing
- Also supports Postman JSON (v2.1) and cURL (see `importer` module)

## Data Model

### Entities

- **Collection** — named group of requests, nestable via `parent_id` for folders.
- **Request** — method, URL, headers, body (raw text/JSON/XML/SOAP XML/form-data/binary), query params, auth config, tags, belongs to a collection. Auth config stores type + fields: None, Bearer (token), Basic (username, password). Auth fields support `{{variable}}` and template function resolution.
- **RequestTag** — join table between requests and tags. Columns: `request_id`, `tag` (string). Indexed on `tag` for fast filtering. A request can have multiple tags (e.g. `smoke`, `auth`, `crud`).
- **Environment** — named set of key-value pairs (e.g. "dev", "staging").
- **Variable** — key + value + enabled flag + `is_secret` flag. Belongs to an environment. Secret values are AES-256-GCM encrypted in the `value` column.
- **HistoryEntry** — snapshot of a resolved request + response (status, headers, body, timing). Timestamped. Retention: max 1000 entries globally, oldest pruned automatically on insert.
- **Cookie** — domain, name, value, path, expiry. Global, scoped by domain.

### Relationships

```
Collection 1──N Request
Collection 1──N Collection (folders)
Environment 1──N Variable
Request 0..1──N HistoryEntry  (nullable request_id; null = ad-hoc request)
```

### Key decisions

- Requests store raw templates with both variables and template functions: `{{base_url}}/api/users/{{uuid()}}`. Resolution happens at execution time.
- HistoryEntry stores resolved values — what was actually sent/received. The `request_id` is nullable: ad-hoc requests (fired without saving) still produce history entries with `request_id = NULL`.
- Deleting a request sets `request_id = NULL` on its history entries (no cascade delete — history is preserved).
- Cookies are global, scoped by domain, not per-collection.

## Core Layer Modules

### `http_engine`

Wraps `reqwest`. Owns a shared `reqwest::Client` (constructed without a cookie jar — cookies are managed manually by `cookie_manager` via headers). Takes a resolved request with cookie headers already injected, executes it, returns the full response (status, headers including `Set-Cookie`, body, timing).

### `template_engine`

Replaces the simpler `variable_resolver`. Handles two kinds of interpolation:

**1. Variable substitution:** `{{variable_name}}` — looks up the value in the active environment. Secret variable values arrive already decrypted from the `storage` layer (which handles encryption/decryption transparently). Secret values are never displayed in plain text in the UI (masked with `********`).

**2. Template functions:** `{{function_name(args)}}` — evaluated at execution time. V1 built-in functions:

| Function | Example | Description |
|---|---|---|
| `uuid()` | `{{uuid()}}` | Random UUID v4 |
| `timestamp()` | `{{timestamp()}}` | Unix timestamp (seconds) |
| `timestampMs()` | `{{timestampMs()}}` | Unix timestamp (milliseconds) |
| `isoDate()` | `{{isoDate()}}` | ISO 8601 date string |
| `base64(val)` | `{{base64(myVar)}}` | Base64 encode a variable's value |
| `randomInt(min,max)` | `{{randomInt(1,100)}}` | Random integer in range |
| `randomStr(len)` | `{{randomStr(16)}}` | Random alphanumeric string |

**3. Prompt variables:** `{{prompt(label)}}` — opens a dialog at send-time asking the user to enter a value. Useful for one-time tokens, OTPs, CAPTCHAs. The prompted value is not stored.

**Composability:** Template function arguments are resolved as variable names only, not as nested template expressions. `{{base64(myVar)}}` resolves `myVar` from the environment and then base64-encodes it. `{{base64(uuid())}}` is **not** valid — nesting is not supported in V1.

**Resolution scope:** URL, headers, query params, text-based body types (raw text, JSON, XML), form-data keys and values, auth fields. Binary bodies are never resolved. Unresolved variables/functions are left as-is with a warning.

### `response_chain`

Enables referencing values from previous responses in new requests.

**Syntax:** `{{response("Request Name", "$.json.path")}}` — looks up the most recent history entry for the named request and extracts a value using JSONPath.

**How it works:**
1. At resolution time, queries `HistoryEntry` for the most recent entry matching the request name. The lookup is scoped to the **same collection** as the request being resolved. If multiple requests in the collection share the same name, the most recently executed one (by `created_at` desc) wins.
2. Parses the stored response body as JSON.
3. Applies the JSONPath expression to extract the value.
4. Returns the extracted string value for interpolation.

**Scope:** Works anywhere template functions work. JSONPath only in V1 (XPath deferred to V2). If the referenced request has no history or the name is not found, resolution fails with a warning (the literal template string is left in place). Renaming a request does not retroactively update chained references — the user must update them manually.

### `importer`

Parses external formats into the internal model. Stateless functions.

**Supported formats:**
- **Postman collection JSON (v2.1)** — maps collections, folders, requests, environments, auth configs
- **cURL commands** — supported flags: `-X` (method), `-H` (headers), `-d`/`--data` (body), `-u` (basic auth), `-b` (cookies), `-A` (user agent), `--url`. Unsupported flags are ignored; a warning is added to the import summary.
- **YAML folder** — reads the app's own export format back into SQLite

Output is always saved Request(s) added to a target collection. Import summary shows successes, failures, and warnings for skipped/unsupported elements.

### `exporter`

Exports collections to the YAML interchange format. Secret variable values are never included — only names are listed with a placeholder marker. See "YAML export/import" section above.

### `storage`

All SQLite access. CRUD for collections, requests, environments, history, cookies. Uses `sqlx` with its async SQLite connection pool and runtime-checked queries (`sqlx::query()`, not compile-time `query!` macros — avoids requiring `DATABASE_URL` at build time). Migration SQL files are embedded in the binary via `sqlx::migrate!()` and run on startup.

Handles encryption/decryption of secret variable values transparently — Core modules always see plain text; `storage` encrypts on write and decrypts on read. No other module touches encryption.

**History pruning:** after each insert, executes `DELETE FROM history_entries WHERE id NOT IN (SELECT id FROM history_entries ORDER BY created_at DESC LIMIT 1000)` in the same transaction. Maximum 1000 entries retained.

### `cookie_manager`

Manages cookie persistence between SQLite and HTTP requests. Does not use `reqwest::cookie::Jar` (its API is write-only with no read-back). Instead:

1. Before each request: loads matching cookies from SQLite and injects them as `Cookie` headers via `http_engine`.
2. After each response: parses `Set-Cookie` headers from the raw `reqwest::Response`, persists new/updated cookies to SQLite.
3. Handles expiry cleanup (delete expired cookies on read).

The `reqwest::Client` is constructed without a cookie jar — cookie management is fully manual via headers and SQLite.

All modules expose plain async functions. No traits or abstractions unless there's a concrete second implementation. Core has zero knowledge of Dioxus.

## UI Design

### Aesthetic Direction: "Developer Instrument Panel"

Utilitarian precision. Dense, information-rich, clear visual hierarchy.

- **Typography:** Monospace for all data: JetBrains Mono (bundled), fallback chain: Fira Code, Cascadia Code, system monospace. Geometric sans for labels/chrome: DM Sans (bundled), fallback: system sans-serif.
- **Color:** Dark theme. Near-black background (#0a0a0b), muted chrome. Semantic accents: green for 2xx, amber for 3xx, red for 4xx/5xx, cyan for timing. Method colors: GET=green, POST=blue, PUT=amber, DELETE=red, PATCH=purple, HEAD/OPTIONS=muted gray.

### Layout

Default: vertical split (request left, response right). Configurable to horizontal split (request top, response bottom) via a toggle button or keyboard shortcut.

The split container is a generic component that takes an orientation prop.

```
┌──────────┬─────────────────────────────────────────┐
│ Sidebar  │ Tab Bar: [GET /users] [POST /users] [+] │
│          ├───────────────────┬─────────────────────┤
│ ▸ Auth   │ Request           │ Response            │
│ ▸ Users  │                   │                     │
│   GET /  │ GET ▾ {{base}}/u… │ 200 OK    45ms      │
│   POST / │                   │                     │
│   GET :id│ Params | Headers  │ Body | Headers      │
│          │ ───────────────── │ ────────────────────│
│ History  │ Accept: app/json  │ {                   │
│  12:03 ← │ Auth: Bearer {{t}}│   "users": [        │
│  12:01 ← │                   │     { "id": 1 }     │
│  11:58 ← │ Body              │   ]                 │
│          │ ───────────────── │ }                   │
│ ● prod   │          [Send ▶] │ 2.1 KB              │
└──────────┴───────────────────┴─────────────────────┘
```

The tab bar spans the request+response area, above the split container.

### Components

- **Sidebar** — tree view for collections/folders, history list (filterable by time and tags), environment selector dropdown at bottom. Tag filter bar at the top of the collection tree.
- **Tab bar** — open requests as tabs, close/reorder.
- **Request editor** — method dropdown, URL input with variable/template highlighting, sub-tabs for params/headers/body/auth. Reusable key-value editor widget. Tag editor (inline chips). Template function autocomplete in URL and value fields. Body mode selector: raw text, JSON, XML, SOAP (pre-fills envelope template), form-data, binary. Selecting SOAP auto-sets `Content-Type: text/xml` and adds a `SOAPAction` header placeholder.
- **Response viewer** — two view modes toggled by the user: **Pretty** (syntax-highlighted text) and **Tree** (collapsible JSON tree, only available for JSON responses — built as a custom Dioxus component, recursive rendering of serde_json::Value). Default is Pretty. Also: headers table, cookies table, status/time/size bar. A **Raw** toggle is always available to show the unformatted body. JSONPath filter bar above the body — type an expression, see only the matching subset.
- **Split container** — generic component, orientation prop (vertical/horizontal), draggable divider.
- **Prompt dialog** — modal that appears at send-time when the request contains `{{prompt(...)}}` template functions. Shows one input per prompt variable, with the label from the function argument. Send proceeds after the user fills in all values.
- **Secret variable editor** — in the environment editor, secret variables show a masked value (`********`) with a reveal toggle (eye icon). New variables can be marked as secret via a lock icon toggle.

### State Management

Dioxus signals for reactive state. One global app state struct:
- Open tabs and active tab (session-only, not persisted across restarts; unsaved changes are discarded on close with a confirmation dialog: "You have unsaved changes in N tabs. Close anyway?")
- Active environment
- Sidebar selection and collapse state
- Split orientation preference
- Tag filter state

Each component reads only the signals it needs.

### Micro-details

- Status codes rendered large and bold with semantic color.
- Pulsing indicator while request is in flight.
- JSON tree with indentation guide lines.
- Unresolved `{{variables}}` highlighted in amber.
- Template functions highlighted in a distinct color (cyan) to distinguish from plain variables.
- Secret values masked everywhere in the UI except the dedicated reveal toggle.

## V1 Features

1. HTTP request builder (method, URL, headers, body, query params)
2. Response viewer with syntax highlighting + collapsible JSON tree
3. Collections with nestable folders
4. Environment variables with `{{placeholder}}` resolution
5. Import from Postman (v2.1 JSON), cURL, and YAML
6. Export collections to YAML (git-friendly interchange format)
7. Request history with timestamps
8. Cookie management (per-domain, auto-persisted)
9. Configurable split layout (vertical/horizontal)
10. Request tagging and filtering
11. Template functions (uuid, timestamp, base64, random, etc.)
12. Prompt variables (ask for input at send-time)
13. Response chaining via JSONPath
14. Secret variables (encrypted at rest, masked in UI, excluded from export)
15. JSONPath response body filtering
16. SOAP/XML support (XML body mode, SOAP envelope template, XML syntax highlighting)

## Error Handling

- **Network errors** (timeout, DNS, TLS, connection refused): displayed inline in response panel with error type and message. No modals.
- **Large responses**: two independent thresholds. (1) Above 500KB: syntax highlighting is disabled, body renders as raw text. (2) Above 1MB: body is truncated with a "show full" button. "Show full" loads the complete body but always as raw text (no highlighting). Raw view toggle is available at any size.
- **Invalid input**: inline validation with subtle red underline, non-blocking.
- **Variable resolution**: unresolved variables highlighted in amber, request still sends with literal text.
- **Template function errors**: if a function fails (e.g. `{{response(...)}}` with no matching history), the literal template string is left in place and a warning is shown in a non-blocking notification bar below the URL field.
- **Prompt cancellation**: if the user cancels the prompt dialog, the request is not sent.
- **Import errors**: partial imports succeed. Valid requests imported, failures listed in a summary.
- **Keychain unavailable**: if the OS keychain is not accessible (e.g. headless Linux without libsecret), secret variables cannot be created. The app warns on startup and disables the secret variable feature. Pre-existing secret variables in the database are displayed as "[encrypted — keychain unavailable]" and cannot be decrypted, resolved, or revealed. They can be deleted but not read. Regular variables and all other features work normally.

## Distribution

- Fonts (JetBrains Mono, DM Sans) are bundled in the binary via `include_bytes!`. Both are SIL OFL 1.1 licensed — OFL license text must be included in the distributed binary (embedded as a string or shipped alongside).
- Single binary, no installer required. Distributed as a compressed archive per platform.
- XPath support for response chaining and body filtering is deferred to V2 (Rust XPath ecosystem is immature). V1 is JSONPath-only.
