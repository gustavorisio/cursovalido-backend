#!/bin/bash
# Executar como root, a partir de uma copia revisada destes arquivos na VPS.
set -euo pipefail
test "$(id -u)" = 0
cd "$(dirname "$0")"
test -f /opt/cursovalido/app.jar
test -f /var/www/cursovalido/index.html
test -f /etc/cursovalido/backend.env
apt-get update
apt-get install -y rsync curl python3 sudo util-linux
for user in deploy-backend deploy-frontend; do
    id "$user" >/dev/null 2>&1 || useradd -m -s /bin/bash "$user"
    # Necessario para SSH por chave em imagens com conta inicialmente bloqueada.
    # O hash aleatorio nao corresponde a uma senha conhecida.
    usermod -p "$(openssl rand -hex 32)" "$user"
    install -d -o root -g root -m 755 "/home/$user/.ssh"
    touch "/home/$user/.ssh/authorized_keys"
    chown root:root "/home/$user/.ssh/authorized_keys"
    chmod 644 "/home/$user/.ssh/authorized_keys"
done
install -d -o root -g root -m 755 /etc/ssh/cursovalido-keys
for user in deploy-backend deploy-frontend; do
    touch "/etc/ssh/cursovalido-keys/$user"
    chown root:root "/etc/ssh/cursovalido-keys/$user"
    chmod 600 "/etc/ssh/cursovalido-keys/$user"
done
cat > /etc/ssh/sshd_config.d/60-cursovalido-deploy.conf <<'EOF'
Match User deploy-backend
    AuthorizedKeysFile /etc/ssh/cursovalido-keys/%u
    ForceCommand /usr/local/bin/cursovalido-receive-backend
    PasswordAuthentication no
    KbdInteractiveAuthentication no
    DisableForwarding yes
    PermitTTY no
Match User deploy-frontend
    AuthorizedKeysFile /etc/ssh/cursovalido-keys/%u
    ForceCommand /usr/local/bin/cursovalido-receive-frontend
    PasswordAuthentication no
    KbdInteractiveAuthentication no
    DisableForwarding yes
    PermitTTY no
Match all
EOF
/usr/sbin/sshd -t
systemctl reload ssh
id cursovalido >/dev/null 2>&1 || useradd --system --user-group --no-create-home --shell /usr/sbin/nologin cursovalido
install -o root -g root -m 755 receive-backend.sh /usr/local/bin/cursovalido-receive-backend
install -o root -g root -m 755 receive-frontend.sh /usr/local/bin/cursovalido-receive-frontend
install -o root -g root -m 755 publish-backend.sh /usr/local/sbin/cursovalido-publish-backend
printf '%s\n' 'deploy-backend ALL=(root) NOPASSWD: /usr/local/sbin/cursovalido-publish-backend ""' > /etc/sudoers.d/cursovalido-deploy
chmod 440 /etc/sudoers.d/cursovalido-deploy
visudo -cf /etc/sudoers.d/cursovalido-deploy
# Java nao pode substituir seu JAR, arquivos do systemd ou scripts privilegiados.
chown root:root /opt/cursovalido /opt/cursovalido/app.jar
chmod 755 /opt/cursovalido
chmod 644 /opt/cursovalido/app.jar
chown root:root /etc/cursovalido /etc/cursovalido/backend.env
chmod 700 /etc/cursovalido
chmod 600 /etc/cursovalido/backend.env
install -d -m 755 /etc/systemd/system/cursovalido.service.d
cat > /etc/systemd/system/cursovalido.service.d/security.conf <<'EOF'
[Service]
User=cursovalido
Group=cursovalido
NoNewPrivileges=true
PrivateTmp=true
ProtectHome=true
ProtectSystem=strict
ProtectKernelTunables=true
ProtectKernelModules=true
ProtectControlGroups=true
RestrictSUIDSGID=true
CapabilityBoundingSet=
UMask=0077
EOF
# O frontend tem escrita apenas na pasta estatica; nao tem sudo.
chown -R deploy-frontend:deploy-frontend /var/www/cursovalido
systemctl daemon-reload
systemctl restart cursovalido
echo 'Instalado. Configure as chaves com comando forcado conforme o guia.'
