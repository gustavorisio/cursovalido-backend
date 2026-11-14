#!/bin/bash
set -euo pipefail
umask 022
exec 9>/home/deploy-frontend/deploy.lock
flock -w 300 9
stage=$(mktemp -d /home/deploy-frontend/build.XXXXXX)
trap 'rm -rf -- "$stage"' EXIT
python3 -c 'import sys,tarfile; tarfile.open(fileobj=sys.stdin.buffer,mode="r|gz").extractall(sys.argv[1],filter="data")' "$stage"
test -s "$stage/index.html"
test -d "$stage/assets"
# Nao publica links; todos os destinos precisam permanecer na pasta publica.
test -z "$(find "$stage" -type l -print -quit)"
test -z "$(find /var/www/cursovalido -type l -print -quit)"
backup=/home/deploy-frontend/index-$(date +%s%N).html
cp /var/www/cursovalido/index.html "$backup"
find "$stage" -type d -exec chmod 755 {} +
find "$stage" -type f -exec chmod 644 {} +
rsync -r --chmod=D755,F644 --exclude=/index.html "$stage/" /var/www/cursovalido/
install -m 644 "$stage/index.html" /var/www/cursovalido/index.html.new
mv -f /var/www/cursovalido/index.html.new /var/www/cursovalido/index.html
if curl -kfsS --max-time 15 --resolve cursovalido.dev:443:127.0.0.1 \
  https://cursovalido.dev/ -o "$stage/served.html" && cmp -s "$stage/index.html" "$stage/served.html"; then
    echo 'Frontend publicado e verificado.'
else
    install -m 644 "$backup" /var/www/cursovalido/index.html.rollback
    mv -f /var/www/cursovalido/index.html.rollback /var/www/cursovalido/index.html
    exit 1
fi
