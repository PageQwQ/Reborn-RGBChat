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

changelog = cfg["versions"][0].get("changelog", "")

print(f"# RGB Chat {version}\n")
print(changelog + "\n")
print("#### Downloads")
print(f"- **Fabric**: {', '.join(sorted(set(loaders.get('Fabric', []))))}")
print(f"- **NeoForge**: {', '.join(sorted(set(loaders.get('NeoForge', []))))}")
print("")
print("Find jars in the release assets (main + sources for every platform).")
