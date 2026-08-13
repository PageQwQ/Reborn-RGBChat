# RGB Chat — Reborn

RGB colored chat and text rendering for modern Minecraft, rebuilt from the original 1.12.2 RGB Chat mod.

Type `#RRGGBB` tags anywhere text renders and it gets colored client-side — no server mod, no network involvement:

- `#FF0000red text` — solid color
- `#FF0000-0000FFgradient text` — per-character gradient with 2+ stops
- `##` — literal `#`; unrecognized `#`s show as-is
- Vanilla `§` codes keep their semantics and end active gradients

Supported render contexts: chat, signs, anvils, item names, and the message input preview.

## Compatibility

| Loader | Versions |
|---|---|
| Fabric | 1.21.1, 1.21.2 – 1.21.11, 26.1.2, 26.2 |
| NeoForge | 26.1.2, 26.2 |

On dedicated servers the mod is a no-op.

## Project layout

The mod is a pure-Java core plus thin loader layers:

| Directory | Target | Notes |
|---|---|---|
| `common/` | — | Loader-free parsing/format core (`RgbParser`, `RgbFormat`, gradient engine), source-linked into every project |
| `mc-1-21-1/` | Fabric 1.21.1 | fabric-loom, JDK 21 |
| `mc-1-21-x/` | Fabric 1.21.2–1.21.11 | single project, switch with `-Pmc_version` |
| `mc-26-x/` | Fabric 26.1.2, 26.2 | same project, switch with `-Pmc_version` |
| `mc-26-x-neoforge/` | NeoForge 26.1.2, 26.2 | ModDevGradle, switch with `-Pneoforge_version` |
| `scripts/` | — | Modrinth publish tooling |

## Building

JDK 21 is required for the 1.21.x projects, JDK 25 for the 26.x projects
(`org.gradle.java.home` pins the JDK in each `gradle.properties`).

```sh
./mc-1-21-1/gradlew -p mc-1-21-1 build

./mc-1-21-x/gradlew -p mc-1-21-x build -Pmc_version=1.21.11

./mc-26-x/gradlew -p mc-26-x build -Pmc_version=26.2          # or 26.1.2
./mc-26-x-neoforge/gradlew -p mc-26-x-neoforge build \
    -Pneoforge_version=26.2.0.53-beta                          # or 26.1.2.94
```

Testing in game: `./gradlew -p <dir> runClient [same -P switches]`.

## Configuration

`config/rgbchat.json` is auto-created on first launch:

- `enabled` — master switch
- `gradient` — when false, gradient tags degrade to their first color stop
- `chat` / `signs` / `anvil` / `itemNames` / `inputPreview` — per-context switches

## Publishing

Uploads to Modrinth are handled by `scripts/publish.sh` (all versions are
defined in `scripts/publish-config.json`):

```sh
scripts/publish.sh check                          # token + jars sanity check
scripts/publish.sh create                         # create project + upload all versions
scripts/publish.sh upload [key...]                # add versions to an existing project
scripts/publish.sh <cmd> --dry-run                # print requests without sending
```

The API token comes from the `MODRINTH_PAT` environment variable and is never
stored in the repository.

## License

MIT