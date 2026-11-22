---
name: artseek
description: Inspect the running Koma debug build's ART heap and Compose text on a rooted arm64 device. Use when debugging runtime state without adding logs, finding which object holds a string, or tracing a live object's fields. Not for CI or non-root devices.
---

# artseek: live ART heap inspection

Read-only inspection of the **running** `moe.tnxg.koma` process on a rooted `arm64-v8a`
device. No injection, no hooks, no writes to app memory. The binary is pinned in
`tools/fetch-artseek.sh` and runs on the device as root.

## When to use it

- A bug depends on runtime state you would otherwise add `Log` calls to inspect.
- You need every live instance of a model class (`*Course`, `*Snapshot`, `*ViewModel`).
- You need to know which object owns a string (Compose text, error text, token flags).
- You need the live Activity/ViewRootImpl state or View bounds.

## Do not use it

- In CI or `tools/check.sh`; there is no device and no root on runners.
- On a non-rooted or non-arm64 device; preflight fails by design.
- To find Compose text via `artseek_tree`; the View tree contains only
  `AndroidComposeView`. Use `artseek_strsearch` and follow `owners`.
- Against a release build for behavior verification; start the debug build with
  `mise run run` so the code matches the working tree.

## Start the target

```bash
mise run run    # installs the debug build and launches MainActivity
```

The wrapper refuses to query when `pidof moe.tnxg.koma` is empty.

## Preferred: MCP tools

The project registers `tools/artseek-mcp.mjs` in `.omp/mcp.json` and
`.vscode/mcp.json`. Prefer these tools over shell calls:

| Tool | Use |
| --- | --- |
| `artseek_ping` | binary, ART layout cache and process reachability |
| `artseek_roots` | live Activities and ViewRootImpls with bounds/focus |
| `artseek_tree` | Android View hierarchy (`rootAddr`, `maxDepth`, `focused`) |
| `artseek_strsearch` | `needle`, optional `limit`/`preview`; owners identify holders |
| `artseek_instances` | `type` dot name, JNI descriptor or `*Glob` |
| `artseek_dumpobj` | `addr`, optional `depth`/`schemaOnly` |
| `artseek_whorefs` | `addr`, optional `limit` |
| `artseek_query` | one QuerySpec object; grammar in artseek `protocol/protocol.md` |
| `artseek_selfcheck` | validate the ART layout without recalibrating |
| `artseek_calibrate` | after an ART/ROM update when queries report calibration failure |

Addresses are raw hex strings (`0xa3c9dd0`) and change between runs; re-run
`artseek_roots`/`artseek_strsearch` instead of reusing old ones.

## CLI fallback

```bash
tools/artseek.sh ping
tools/artseek.sh roots
tools/artseek.sh strsearch '课表' --preview --limit 5
tools/artseek.sh instances 'moe.tnxg.koma.domain.model.Course' --limit 5
tools/artseek.sh dumpobj 0xa3c9dd0 --depth 2
tools/artseek.sh whorefs 0xa3abe40 --limit 10
tools/artseek.sh query '{"root":{"kind":"scan","type":"*Course"},"steps":[{"op":"ref"}],"collection":true}'
```

`ARTSEEK_SERIAL` selects one adb device; `ARTSEEK_PKG` overrides the package (never
target another app without being asked).

## Reading results

- `instances` returns `total` plus `items[].addr`; `total: 0` means no live instance,
  not a tool failure. Objects may not exist yet on the current screen.
- `strsearch` hits include `owners[]` with `ownerClass`, `fieldName`, and an optional
  `preview`. That preview is where Compose text, models and flags actually live.
- `tree` reports `effectiveVisible`, `visibleAreaRatio`, `inViewport`; a stopped app's
  root is `INVISIBLE` even though the heap still holds the last composition.
- `meta.cacheHit`/`recalibrated` in raw responses describe the ART layout cache.

## Failure modes

| Message | Fix |
| --- | --- |
| `no adb device` / `multiple devices` | connect the phone or set `ARTSEEK_SERIAL` |
| `device ... reports x86_64` | artseek ships arm64-v8a only |
| `not rooted (su -c id failed)` | use a KernelSU/Magisk device |
| `... is not running` | `mise run run` |
| `failed to infer Class.ifields_` | SDK/binary mismatch; re-run `tools/fetch-artseek.sh` |
| `CALIBRATION_FAILED` | `tools/artseek.sh calibrate` |
