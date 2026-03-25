#!/bin/bash
set -euo pipefail

cd docs-site || exit
pnpm install
pnpm run build

cd ..

echo "Uploading JS files"
scp -r ./docs-site/dist/* finzo:/www/htdocs/w0057ac0/peekandpoke.io
