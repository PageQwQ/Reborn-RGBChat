<div align="center"><center>

# RGB Chat — Reborn

A mod that lets you output colored text.

![Preview](/images/preview.png)

</center></div>

Type `#RRGGBB` tags anywhere text renders and it gets colored client-side — no server mod, no network involvement:

- `#FF0000red text` — solid color
- `#FF0000-0000FFgradient text` — per-character gradient with 2+ stops
- `##` — literal `#`; unrecognized `#`s show as-is
- Vanilla `§` codes keep their semantics and end active gradients

It also supports item names renamed on signs or anvils.

## Compatibility

| Loader | Versions |
|---|---|
| Fabric | 1.21.x, 26.1.2, 26.2 |
| NeoForge | 1.21.1, 1.21.4, 1.21.11, 26.1.2, 26.2 |

On dedicated servers the mod is a no-op.

## Project layout

The mod is a pure-Java core plus thin loader layers, split across branches:

| Branch | Contents |
|---|---|
| `main` | Common core (`common/`), publish tooling (`scripts/`), doc/CI-less base |
| `1.21.x` | Fabric 1.21.1–1.21.11 + NeoForge 1.21.1/1.21.4/1.21.11 projects |
| `26.x` | Fabric + NeoForge 26.1.2/26.2 projects |

The loader projects live on their version branch:

| Directory (on `1.21.x`) | Target | Notes |
|---|---|---|
| `mc-1-21-1/` | Fabric 1.21.1 | fabric-loom, JDK 21 |
| `mc-1-21-x/` | Fabric 1.21.2–1.21.11 | single project, switch with `-Pmc_version` |
| `mc-1-21-1-neoforge/` | NeoForge 1.21.1 | ModDevGradle, fixed version |
| `mc-1-21-4-neoforge/` | NeoForge 1.21.4 | ModDevGradle, fixed version |
| `mc-1-21-11-neoforge/` | NeoForge 1.21.11 | ModDevGradle, fixed version |

| Directory (on `26.x`) | Target | Notes |
|---|---|---|
| `mc-26-x/` | Fabric 26.1.2, 26.2 | same project, switch with `-Pmc_version` |
| `mc-26-x-neoforge/` | NeoForge 26.1.2, 26.2 | ModDevGradle, switch with `-Pneoforge_version` |

## Build

JDK 21 is required for the 1.21.x projects, JDK 25 for the 26.x projects
(`org.gradle.java.home` pins the JDK in each `gradle.properties`). Check out the
version branch first, then build:

```sh
./mc-1-21-1/gradlew -p mc-1-21-1 build

./mc-1-21-x/gradlew -p mc-1-21-x build -Pmc_version=1.21.11

./mc-26-x/gradlew -p mc-26-x build -Pmc_version=26.2          # or 26.1.2
./mc-26-x-neoforge/gradlew -p mc-26-x-neoforge build \
    -Pneoforge_version=26.2.0.53-beta                          # or 26.1.2.94
```

Testing in game: `./gradlew -p <dir> runClient [same -P switches]`.
