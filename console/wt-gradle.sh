#!/usr/bin/env bash
# Runs ./gradlew in the current git worktree. Inside ../ultra-wt/<track> the tests use that worktree's own
# databases (created by console/worktree-new.sh); the main worktree keeps the defaults.
#
#   console/wt-gradle.sh :funktor:cluster:jvmTest
#   console/wt-gradle.sh --full-suite      # clean build --continue, one at a time across ALL worktrees
set -euo pipefail

root=$(git rev-parse --show-toplevel)
cd "$root"

if [[ "$root" == */ultra-wt/* ]]; then
  db="ultra_wt_$(basename "$root" | tr -c 'a-zA-Z0-9\n' '_')"
  export ULTRA_TEST_ARANGO_DB="$db" ULTRA_TEST_MONGO_DB="$db" ULTRA_TEST_DEMO_DB="${db}_demo"
  echo "wt-gradle: test databases $db / ${db}_demo" >&2
fi

if [[ "${1:-}" == "--full-suite" ]]; then
  shift
  echo "wt-gradle: waiting for the full-suite slot (/tmp/ultra-full-suite.lock)" >&2
  exec flock /tmp/ultra-full-suite.lock ./gradlew clean build --continue "$@"
fi

exec ./gradlew "$@"
