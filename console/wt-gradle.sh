#!/usr/bin/env bash
# Runs ./gradlew in the current git worktree. Inside ../ultra-worktrees/<track> the tests use that worktree's own
# databases (created by console/worktree-new.sh); the main worktree keeps the defaults.
#
#   console/wt-gradle.sh :funktor:cluster:jvmTest
#   console/wt-gradle.sh --full-suite      # clean build --continue, one at a time across ALL worktrees
set -euo pipefail

root=$(git rev-parse --show-toplevel)
cd "$root"

if [[ "$root" == */ultra-worktrees/* ]]; then
  db="ultra_wt_$(basename "$root" | tr -c 'a-zA-Z0-9\n' '_')"
  export ULTRA_TEST_ARANGO_DB="$db" ULTRA_TEST_MONGO_DB="$db" ULTRA_TEST_DEMO_DB="${db}_demo"
  echo "wt-gradle: test databases $db / ${db}_demo" >&2

  # Browser tests: each module has a fixed Karma port inside a block of 33 (console/tools/karma-ports). The main
  # worktree uses block 0; a track worktree block 1 or 2, by its place among the worktrees. Two tracks may share a
  # block: they own different modules, and a port that is taken fails the run instead of joining another's server.
  if [[ -z "${ULTRA_KARMA_SLOT:-}" ]]; then
    place=$(ls -1 "$(dirname "$root")" | sort | grep -n -x "$(basename "$root")" | cut -d: -f1)
    export ULTRA_KARMA_SLOT=$(( (place - 1) % 2 + 1 ))
  fi
  echo "wt-gradle: Karma port block $ULTRA_KARMA_SLOT" >&2
fi

if [[ "${1:-}" == "--full-suite" ]]; then
  shift
  echo "wt-gradle: waiting for the full-suite slot (/tmp/ultra-full-suite.lock)" >&2
  exec flock /tmp/ultra-full-suite.lock ./gradlew clean build --continue "$@"
fi

exec ./gradlew "$@"
