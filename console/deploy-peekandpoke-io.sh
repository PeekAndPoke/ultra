#!/bin/bash
set -euo pipefail

cd docs-site || exit
pnpm install
pnpm run build

cd ..

echo "Uploading JS files"
scp -r ./docs-site/dist/* finzo:/www/htdocs/w0057ac0/peekandpoke.io

# Type         | Host          | Value                                      | TTL
# CNAME Record | em3819        | u57039615.wl116.sendgrid.net.              | Automatic
# CNAME Record | s1._domainkey | s1.domainkey.u57039615.wl116.sendgrid.net. | Automatic
# CNAME Record | s2._domainkey | s2.domainkey.u57039615.wl116.sendgrid.net. | Automatic
# TXT Record   | @             | OSSRH-65427                                | Automatic
# TXT Record   | _dmarc        | v=DMARC1; p=none;                          | Automatic
