#!/bin/bash
set -euo pipefail
test "$(id -u)" = 0
# Le o upload com os privilegios do remetente, nunca como root: um link
# trocado durante o deploy nao pode copiar arquivos privados de root ao site/JAR.
snapshot=$(mktemp /opt/cursovalido/upload.XXXXXX)
trap 'rm -f -- "$snapshot"' EXIT
runuser -u deploy-backend -- cat /home/deploy-backend/app.jar > "$snapshot"
test -s "$snapshot"
install -d -m 700 /var/backups/cursovalido-backend
backup=/var/backups/cursovalido-backend/app-$(date +%s%N).jar
cp -p /opt/cursovalido/app.jar "$backup"
healthy() {
    for attempt in $(seq 1 60); do
        if systemctl is-active --quiet cursovalido &&
           test "$(curl -s --max-time 3 -o /dev/null -w '%{http_code}' \
             -H 'X-Forwarded-Proto: https' \
             http://127.0.0.1:8080/api/politicas/ativas || true)" = 200; then
            return 0
        fi
        sleep 2
    done
    return 1
}
install -o root -g root -m 644 "$snapshot" /opt/cursovalido/app.jar.new
mv -f /opt/cursovalido/app.jar.new /opt/cursovalido/app.jar
if systemctl restart cursovalido && healthy; then
    sha256sum /opt/cursovalido/app.jar > /opt/cursovalido/deployed-sha256
    echo 'Backend publicado e verificado.'
else
    install -m 644 "$backup" /opt/cursovalido/app.jar.rollback
    mv -f /opt/cursovalido/app.jar.rollback /opt/cursovalido/app.jar
    systemctl restart cursovalido
    healthy || echo 'Restauracao tambem falhou; consulte journalctl na VPS.' >&2
    exit 1
fi
