#!/usr/bin/env python3
# Generate GitHub release notes from scripts/publish-config.json
import json, os

root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
cfg = json.load(open(os.path.join(root, "scripts/publish-config.json"), encoding="utf-8"))

version = cfg["versions"][0]["version_number"].split("+")[0]
loaders = {}
for v in cfg["versions"]:
    loader = "Fabric" if "fabric" in v["loaders"] else "NeoForge"
    loaders.setdefault(loader, []).extend(v["game_versions"])

print(f"# RGB Chat {version}\n")
print("Bug-fix release: Fabric 1.21.1–1.21.11 no longer crashes on startup.\n")
print("""### Fixed
- **Fabric 1.21.1–1.21.11: crash on startup** — the game failed to launch with an `InvalidInjectionException` in `StringDecomposerMixin`. The mixin config now declares its refmap, so the `#RRGGBB`/gradient injection target resolves correctly in production builds
- All Fabric 1.21.x jars were rebuilt; the 1.0.0 jars crashed on launch and should not be used

### Changed
- Nothing else — behaviour is identical to 1.0.0
""")
print("#### Supported versions")
print(f"- **Fabric**: {', '.join(sorted(set(loaders.get('Fabric', []))))}")
print(f"- **NeoForge**: {', '.join(sorted(set(loaders.get('NeoForge', []))))}")