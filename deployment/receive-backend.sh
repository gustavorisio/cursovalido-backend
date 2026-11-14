#!/bin/bash
set -euo pipefail
umask 077
exec 9>/home/deploy-backend/deploy.lock
flock -w 300 9
temp=$(mktemp /home/deploy-backend/incoming.XXXXXX)
trap 'rm -f -- "$temp"' EXIT
cat > "$temp"
test -s "$temp"
mv -f "$temp" /home/deploy-backend/app.jar
sudo -n /usr/local/sbin/cursovalido-publish-backend
