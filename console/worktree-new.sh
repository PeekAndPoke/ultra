#!/usr/bin/env bash
# Creates a track worktree from master: ../ultra-worktrees/<track> on branch wt/<track>, plus its own test databases.
# Build in it with console/wt-gradle.sh, never with a bare ./gradlew (that would use the SHARED test databases).
set -euo pipefail

track=${1:?usage: console/worktree-new.sh <track>}
main=$(git rev-parse --show-toplevel)
dir="$(dirname "$main")/ultra-worktrees/$track"

git -C "$main" worktree add -b "wt/$track" "$dir" master

# Gitignored local config the test suites read: linked, so the secrets stay in one place.
for f in funktor-demo/server/src/main/resources/config/keys.env.conf; do
  if [[ -f "$main/$f" ]]; then
    ln -s "$main/$f" "$dir/$f"
    echo "linked $f"
  fi
done

db="ultra_wt_$(printf '%s' "$track" | tr -c 'a-zA-Z0-9' '_')"

# Karango creates collections and indexes, never a database. MongoDB creates its database on first write.
for name in "$db" "${db}_demo"; do
  code=$(curl -s -o /dev/null -w '%{http_code}' -u root: -H 'Content-Type: application/json' \
    -X POST http://localhost:8529/_api/database -d "{\"name\":\"$name\"}")
  case "$code" in
    201) echo "created Arango database $name" ;;
    409) echo "Arango database $name already exists" ;;
    *) echo "creating Arango database $name failed: HTTP $code" >&2; exit 1 ;;
  esac
done

echo "worktree $dir on branch wt/$track; tests use the databases $db and ${db}_demo"
