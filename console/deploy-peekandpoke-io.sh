#!/bin/bash
set -euo pipefail

cd docs-site || exit
pnpm install
pnpm run build

cd ..

echo "Deploying to peekandpoke.io"
rsync -av --delete ./docs-site/dist/ finzo:/www/htdocs/w0057ac0/peekandpoke.io/
