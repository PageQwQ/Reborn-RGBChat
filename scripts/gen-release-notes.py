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
print("The first release of the rebuilt RGB Chat.\n")
print("""- `#RRGGBB` solid colors and `#RRGGBB-RRGGBB` gradients, rendered entirely client-side — no server mod required
- Works in chat, signs, anvils, item names, and the input preview before you send
- Auto-creates `config/rgbchat.json` with all switches on
- Vanilla `§` codes keep their original meaning and end an active gradient
""")
print("#### Supported versions")
print(f"- **Fabric**: {', '.join(sorted(set(loaders.get('Fabric', []))))}")
print(f"- **NeoForge**: {', '.join(sorted(set(loaders.get('NeoForge', []))))}")