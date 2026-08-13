#!/usr/bin/env bash
# RGB Chat Modrinth 发布脚本
#
# 用法:
#   scripts/publish.sh check                 # 校验 token / 项目 slug / jar 存在性
#   scripts/publish.sh build                 # 为所有未构建的版本跑 gradle build
#   scripts/publish.sh create [--dry-run]    # 创建 Modrinth 项目并一次上传全部版本
#   scripts/publish.sh upload [--dry-run] [key...]  # 向已有项目逐版本上传 (POST /v2/version)
#
# 配置: scripts/publish-config.json
# token: ~/.zshrc 的 MODRINTH_PAT (绝不入库)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONFIG="$ROOT/scripts/publish-config.json"
API="https://api.modrinth.com/v2"
UA="rgb-chat/1.0.0 (PageQwQ)"

[[ -f "$CONFIG" ]] || { echo "missing $CONFIG" >&2; exit 1; }

# --- token ---
if [[ -z "${MODRINTH_PAT:-}" ]]; then
  source "$HOME/.zshrc" 2>/dev/null || true
fi
: "${MODRINTH_PAT:?MODRINTH_PAT 未设置 (见 ~/.zshrc)}"

CMD="${1:-upload}"; shift || true
DRY=""; KEYS=()
for a in "$@"; do
  [[ "$a" == "--dry-run" ]] && DRY=1 && continue
  KEYS+=("$a")
done

py() { python3 "$@"; }

slug=$(py -c "import json;print(json.load(open('$CONFIG'))['slug'])")

resolve_id() {
  local out
  out=$(curl -s -H "User-Agent: $UA" "$API/project/$slug") || true
  project_id=$(py -c "
import json,sys
try: print(json.loads(sys.stdin.read())['id'])
except Exception: print('')" <<<"$out")
}

build_missing() {
  py - "$CONFIG" "$ROOT" "$(dirname "$ROOT/gradle/wrapper/gradle-wrapper.jar")" <<'PY'
import json, os, subprocess, sys
cfg = json.load(open(sys.argv[1])); root = sys.argv[2]
for v in cfg["versions"]:
    parts = [v["jar"]] + ([v["sources"]] if v.get("sources") else [])
    missing = [p for p in parts if not os.path.exists(os.path.join(root, p))]
    if missing:
        target = os.path.join(root, v["build"]["dir"])
        args = [os.path.join(target, "gradlew") if os.name != "nt" else os.path.join(target, "gradlew.bat"),
                "-p", target, "build"] + v["build"].get("args", []) + ["--console=plain"]
        subprocess.run(args, check=True)
PY
}

case "$CMD" in
  check)
    me=$(curl -s -H "Authorization: Bearer $MODRINTH_PAT" -H "User-Agent: $UA" "$API/user/me")
    who=$(py -c "import json,sys;print(json.load(sys.stdin).get('username','INVALID'))" <<<"$me")
    echo "token: $who"
    resolve_id
    echo "project: $slug -> ${project_id:-未创建 (需先 create)}"
    build_missing
    py - "$CONFIG" "$ROOT" <<'PY'
import json, os, sys
cfg = json.load(open(sys.argv[1])); root = sys.argv[2]
for v in cfg["versions"]:
    parts = [v["jar"]] + ([v["sources"]] if v.get("sources") else [])
    missing = [p for p in parts if not os.path.exists(os.path.join(root, p))]
    print(f"{v['key']}: {'OK' if not missing else '缺 jar: '+', '.join(missing)}")
PY
    ;;
  build)
    build_missing
    echo "build done"
    ;;
  create)
    resolve_id
    if [[ -n "$project_id" && -z "$DRY" ]]; then
      echo "项目已存在 ($slug): $project_id, 用 upload 命令" >&2; exit 1
    fi
    build_missing
    [[ "$DRY" ]] && { echo "[dry-run] 跳过 jar 校验"; }
    dataf="/tmp/rgbchat-create-$slug.json"
    py - "$CONFIG" "$ROOT" "$dataf" <<'PY'
import json, os, sys
cfg = json.load(open(sys.argv[1])); root = sys.argv[2]
out = {k: cfg[k] for k in ("title","slug","description","license_id","client_side","server_side","categories","source_url")}
out["body"] = open(os.path.join(root, cfg["body_file"])).read()
out["requested_status"] = cfg.get("requested_status", "approved")
vers = []
for i, v in enumerate(cfg["versions"]):
    parts = [f"file_{i}_0"]
    if v.get("sources"): parts.append(f"file_{i}_1")
    vers.append({
        "file_parts": parts,
        "version_number": v["version_number"],
        "version_title": v["version_title"],
        "changelog": v.get("changelog", ""),
        "game_versions": v["game_versions"],
        "loaders": v["loaders"],
        "release_channel": v["release_channel"],
        "dependencies": [],
        "featured": False,
        "primary_file": parts[0],
    })
out["initial_versions"] = vers
json.dump(out, open(sys.argv[3], "w", encoding="utf-8"), ensure_ascii=False, indent=2)
PY
    cmd=(curl -sS -X POST "$API/project"
         -H "Authorization: Bearer $MODRINTH_PAT"
         -H "User-Agent: $UA"
         -F "data=<$dataf")
    icon=$(py -c "import json;print(json.load(open('$CONFIG')).get('icon',''))")
    [[ -n "$icon" && -f "$ROOT/$icon" ]] && cmd+=(-F "icon=@$ROOT/$icon")
    while IFS= read -r kv; do
      field=${kv%%:*}; path=${kv#*:}
      cmd+=(-F "${field}=@$ROOT/$path")
    done < <(py - "$CONFIG" <<'PY'
import json, sys
cfg = json.load(open(sys.argv[1]))
for i, v in enumerate(cfg["versions"]):
    print(f"file_{i}_0:{v['jar']}")
    if v.get("sources"): print(f"file_{i}_1:{v['sources']}")
PY
)
    if [[ "$DRY" ]]; then
      echo "[dry-run]"; printf '%q ' "POST $API/project" "${cmd[@]}"; echo
    else
      resp=$("${cmd[@]}")
      echo "$resp"
      py -c "import json,sys;d=json.loads(sys.stdin.read());print('created:',d.get('slug'),d.get('id'))" <<<"$resp" || echo "响应解析失败，检查上方输出"
    fi
    ;;
  upload)
    resolve_id
    if [[ -z "$project_id" ]]; then
      echo "项目 $slug 未找到。若刚 create 过可能在人工审核中 (公开 API 404 正常), 用带鉴权的查询:"
      echo "  curl -H \"Authorization: Bearer \$MODRINTH_PAT\" $API/project/$slug"
      exit 1
    fi
    build_missing
    py - "$CONFIG" "$ROOT" "$project_id" "$DRY" "${KEYS[*]}" <<'PY'
import json, os, subprocess, sys
cfg = json.load(open(sys.argv[1])); root = sys.argv[2]
pid, dry, only = sys.argv[3], bool(sys.argv[4]), sys.argv[5].split()
for v in cfg["versions"]:
    if only and v["key"] not in only: continue
    data = {
        "project_id": pid, "file_parts": ["file_0", "file_1"] if v.get("sources") else ["file_0"],
        "version_number": v["version_number"], "version_title": v["version_title"],
        "changelog": v.get("changelog", ""), "game_versions": v["game_versions"],
        "loaders": v["loaders"], "release_channel": v["release_channel"],
        "dependencies": [], "featured": False, "primary_file": "file_0",
    }
    dataf = f"/tmp/rgbchat-version-{v['key']}.json"
    with open(dataf, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    cmd = ["curl", "-sS", "-X", "POST", "https://api.modrinth.com/v2/version",
           "-H", f"Authorization: Bearer {os.environ['MODRINTH_PAT']}",
           "-H", "User-Agent: rgb-chat/1.0.0 (PageQwQ)",
           "-F", f"data=<{dataf}",
           "-F", f"file_0=@{os.path.join(root, v['jar'])}"]
    if v.get("sources"):
        cmd += ["-F", f"file_1=@{os.path.join(root, v['sources'])}"]
    print(f"== {v['key']}: {v['version_number']} @ {v['loaders']} {v['game_versions']}" + (" [dry-run]" if dry else ""), flush=True)
    if dry:
        print("  " + " ".join(cmd), flush=True)
        continue
    resp = subprocess.run(cmd, capture_output=True, text=True)
    out = resp.stdout.strip()
    try:
        d = json.loads(out)
        print("  ->", d.get("id", out))
    except Exception:
        print("  ->", out, resp.stderr.strip())
PY
    ;;
  *)
    echo "用法: $0 check|build|create|upload [--dry-run] [key...]" >&2; exit 1 ;;
esac