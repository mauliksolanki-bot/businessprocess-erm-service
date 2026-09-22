#!/usr/bin/env bash
#
# provision-server.sh
#
# Prepares a fresh Ubuntu EC2 instance to build & run the ermservice Spring Boot
# app natively (systemd service) and installs Jenkins on the same host.
#
# Run as root (or via sudo) on the target Ubuntu instance:
#   sudo bash provision-server.sh
#
set -euo pipefail

# ---------------------------------------------------------------------------
# Configuration (override via environment before invoking the script if needed)
# ---------------------------------------------------------------------------
REPO_URL="${REPO_URL:-https://github.com/mauliksolanki-bot/businessprocess-erm-service.git}"
REPO_BRANCH="${REPO_BRANCH:-production}"
APP_USER="${APP_USER:-ermservice}"
APP_DIR="${APP_DIR:-/opt/ermservice}"
APP_SRC_DIR="${APP_DIR}/src"
APP_JAR="${APP_DIR}/app.jar"
ENV_DIR="/etc/ermservice"
ENV_FILE="${ENV_DIR}/ermservice.env"
SERVICE_NAME="ermservice"
APP_PORT="${APP_PORT:-8081}"
JENKINS_PORT="${JENKINS_PORT:-8080}"

log() { echo -e "\n\033[1;32m==> $*\033[0m"; }
warn() { echo -e "\033[1;33mWARN: $*\033[0m"; }

if [[ $EUID -ne 0 ]]; then
  echo "This script must be run as root (use sudo)." >&2
  exit 1
fi

# ---------------------------------------------------------------------------
# 1. OS packages: Java 21, Maven, git, curl, ufw
# ---------------------------------------------------------------------------
log "Updating apt and installing prerequisites (Java 21, Maven, git, curl, ufw)"
export DEBIAN_FRONTEND=noninteractive

# Remove any broken/partial Jenkins apt source left over from a previous failed
# run, so the initial 'apt-get update' below doesn't fail before we get to the
# (re)configured Jenkins repo setup later in this script.
rm -f /etc/apt/sources.list.d/jenkins.list /usr/share/keyrings/jenkins-keyring.asc /usr/share/keyrings/jenkins-keyring.gpg

apt-get update -y
apt-get install -y openjdk-21-jdk maven git curl unzip gnupg ufw ca-certificates

java -version
mvn -version

# ---------------------------------------------------------------------------
# 2. Service user + directories
# ---------------------------------------------------------------------------
log "Creating service user '${APP_USER}' and directories"
if ! id -u "${APP_USER}" >/dev/null 2>&1; then
  useradd --system --create-home --home-dir "${APP_DIR}" --shell /usr/sbin/nologin "${APP_USER}"
fi
mkdir -p "${APP_DIR}" "${ENV_DIR}"
chown -R "${APP_USER}:${APP_USER}" "${APP_DIR}"

# ---------------------------------------------------------------------------
# 3. Environment file with placeholders (created once, never overwritten)
# ---------------------------------------------------------------------------
if [[ ! -f "${ENV_FILE}" ]]; then
  log "Creating placeholder env file at ${ENV_FILE} (fill in real DB/JWT values, then: sudo systemctl restart ${SERVICE_NAME})"
  cat > "${ENV_FILE}" <<EOF
SPRING_PROFILES_ACTIVE=production
SERVER_PORT=${APP_PORT}
# Fill these in manually with your real MySQL/RDS + JWT secret values:
PROD_DB_URL=jdbc:mysql://REPLACE_ME:3306/REPLACE_ME
PROD_DB_USERNAME=REPLACE_ME
PROD_DB_PASSWORD=REPLACE_ME
PROD_JWT_SECRET=REPLACE_ME
EOF
  chown "${APP_USER}:${APP_USER}" "${ENV_FILE}"
  chmod 600 "${ENV_FILE}"
else
  log "Env file ${ENV_FILE} already exists; leaving it untouched"
fi

# ---------------------------------------------------------------------------
# 4. Clone + build the app, deploy the jar
# ---------------------------------------------------------------------------
log "Cloning ${REPO_URL} (branch ${REPO_BRANCH})"
if [[ -d "${APP_SRC_DIR}/.git" ]]; then
  su -s /bin/bash "${APP_USER}" -c "git -C '${APP_SRC_DIR}' fetch --all && git -C '${APP_SRC_DIR}' checkout '${REPO_BRANCH}' && git -C '${APP_SRC_DIR}' reset --hard 'origin/${REPO_BRANCH}'"
else
  rm -rf "${APP_SRC_DIR}"
  su -s /bin/bash "${APP_USER}" -c "git clone --branch '${REPO_BRANCH}' '${REPO_URL}' '${APP_SRC_DIR}'"
fi

log "Building the app with Maven (this may take a few minutes on first run)"
su -s /bin/bash "${APP_USER}" -c "cd '${APP_SRC_DIR}' && mvn -B -q clean package -DskipTests"

BUILT_JAR="$(find "${APP_SRC_DIR}/target" -maxdepth 1 -name '*.jar' ! -name '*sources*' ! -name '*javadoc*' | head -n1)"
if [[ -z "${BUILT_JAR}" ]]; then
  echo "ERROR: could not find built jar under ${APP_SRC_DIR}/target" >&2
  exit 1
fi
cp "${BUILT_JAR}" "${APP_JAR}"
chown "${APP_USER}:${APP_USER}" "${APP_JAR}"
log "Deployed jar: ${BUILT_JAR} -> ${APP_JAR}"

# ---------------------------------------------------------------------------
# 5. systemd service
# ---------------------------------------------------------------------------
log "Writing systemd unit /etc/systemd/system/${SERVICE_NAME}.service"
cat > "/etc/systemd/system/${SERVICE_NAME}.service" <<EOF
[Unit]
Description=ermservice Spring Boot application
After=network.target

[Service]
Type=simple
User=${APP_USER}
WorkingDirectory=${APP_DIR}
EnvironmentFile=${ENV_FILE}
ExecStart=/usr/bin/java -jar ${APP_JAR}
SuccessExitStatus=143
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable "${SERVICE_NAME}"
systemctl restart "${SERVICE_NAME}"

# ---------------------------------------------------------------------------
# 6. Jenkins install
# ---------------------------------------------------------------------------
log "Installing Jenkins"
if ! command -v jenkins >/dev/null 2>&1; then
  # Clean up any partial/broken state from a previous failed attempt
  rm -f /usr/share/keyrings/jenkins-keyring.asc /usr/share/keyrings/jenkins-keyring.gpg
  rm -f /etc/apt/sources.list.d/jenkins.list

  KEY_FILE="/tmp/jenkins-key.asc"
  KEY_URL="https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key"

  log "Downloading Jenkins signing key from ${KEY_URL}"
  HTTP_CODE="$(curl -fsSL -w '%{http_code}' -o "${KEY_FILE}" --retry 5 --retry-delay 3 "${KEY_URL}" || echo "000")"

  if [[ "${HTTP_CODE}" != "200" ]] || ! grep -q "BEGIN PGP PUBLIC KEY BLOCK" "${KEY_FILE}"; then
    echo "ERROR: failed to download a valid Jenkins signing key (HTTP ${HTTP_CODE})." >&2
    echo "First bytes of response:" >&2
    head -c 300 "${KEY_FILE}" >&2 || true
    echo >&2
    echo "This usually means outbound HTTPS from this EC2 instance to pkg.jenkins.io is blocked/intercepted" >&2
    echo "(check the security group / NACL / any corporate proxy). Skipping Jenkins install; app deployment still succeeded." >&2
    SKIP_JENKINS=true
  fi

  if [[ "${SKIP_JENKINS:-false}" != "true" ]]; then
    gpg --dearmor -o /usr/share/keyrings/jenkins-keyring.gpg < "${KEY_FILE}"
    KEY_ID="$(gpg --show-keys --with-colons "${KEY_FILE}" 2>/dev/null | awk -F: '/^fpr:/ {print $10; exit}')"
    log "Imported Jenkins signing key (fingerprint: ${KEY_ID:-unknown})"

    echo "deb [signed-by=/usr/share/keyrings/jenkins-keyring.gpg] https://pkg.jenkins.io/debian-stable binary/" \
      > /etc/apt/sources.list.d/jenkins.list

    if apt-get update -y && apt-get install -y jenkins; then
      : # success
    else
      echo "ERROR: Jenkins repo/install failed even with a valid key - see apt output above." >&2
      rm -f /etc/apt/sources.list.d/jenkins.list
      apt-get update -y || true
      SKIP_JENKINS=true
    fi
  fi
fi

if [[ "${SKIP_JENKINS:-false}" != "true" ]]; then
  systemctl enable jenkins
  systemctl restart jenkins
fi

# ---------------------------------------------------------------------------
# 7. Firewall
# ---------------------------------------------------------------------------
log "Configuring ufw firewall (allow SSH, ${JENKINS_PORT}, ${APP_PORT})"
ufw allow OpenSSH || true
ufw allow "${JENKINS_PORT}/tcp" || true
ufw allow "${APP_PORT}/tcp" || true
ufw --force enable

# ---------------------------------------------------------------------------
# 8. Scoped sudoers so Jenkins can deploy without full root
# ---------------------------------------------------------------------------
log "Granting jenkins user scoped sudo rights to deploy ${SERVICE_NAME}"
cat > /etc/sudoers.d/jenkins-ermservice <<EOF
jenkins ALL=(root) NOPASSWD: /bin/systemctl restart ${SERVICE_NAME}
jenkins ALL=(root) NOPASSWD: /bin/systemctl status ${SERVICE_NAME}
jenkins ALL=(root) NOPASSWD: /bin/cp /tmp/ermservice-deploy.jar ${APP_JAR}
jenkins ALL=(root) NOPASSWD: /bin/chown ${APP_USER}\:${APP_USER} ${APP_JAR}
EOF
chmod 440 /etc/sudoers.d/jenkins-ermservice
visudo -c -f /etc/sudoers.d/jenkins-ermservice

# ---------------------------------------------------------------------------
# 9. Verification
# ---------------------------------------------------------------------------
log "Verifying deployment"
sleep 8

STATUS_OK=true

echo "---- systemctl status ${SERVICE_NAME} ----"
if systemctl is-active --quiet "${SERVICE_NAME}"; then
  echo "systemctl: ${SERVICE_NAME} is ACTIVE"
else
  echo "systemctl: ${SERVICE_NAME} is NOT active"
  STATUS_OK=false
fi
systemctl status "${SERVICE_NAME}" --no-pager -l || true

echo "---- java process check ----"
if pgrep -f "java -jar ${APP_JAR}" >/dev/null; then
  echo "java process FOUND:"
  ps -ef | grep "[j]ava -jar ${APP_JAR}"
else
  echo "java process NOT FOUND"
  STATUS_OK=false
fi

echo "---- HTTP smoke test (http://localhost:${APP_PORT}/v3/api-docs) ----"
for i in {1..10}; do
  if curl -sf "http://localhost:${APP_PORT}/v3/api-docs" -o /dev/null; then
    echo "HTTP check: OK (app responding on ${APP_PORT})"
    HTTP_OK=true
    break
  fi
  sleep 5
done
if [[ "${HTTP_OK:-false}" != "true" ]]; then
  echo "HTTP check: FAILED to get a response on ${APP_PORT} (this is expected if PROD_DB_* placeholders in ${ENV_FILE} haven't been filled in yet)"
fi

echo "---- Jenkins status ----"
systemctl is-active --quiet jenkins && echo "jenkins: ACTIVE" || { echo "jenkins: NOT active"; STATUS_OK=false; }

echo
if [[ "${STATUS_OK}" == "true" ]]; then
  log "PROVISIONING COMPLETE - PASS"
else
  warn "PROVISIONING COMPLETE WITH ISSUES - see checks above (likely just needs real DB/JWT values in ${ENV_FILE})"
fi

echo
echo "Next steps:"
echo "  1. Edit ${ENV_FILE} with real PROD_DB_URL / PROD_DB_USERNAME / PROD_DB_PASSWORD / PROD_JWT_SECRET"
echo "  2. sudo systemctl restart ${SERVICE_NAME}"
echo "  3. Jenkins UI: http://<public-ip>:${JENKINS_PORT}  (initial admin password: sudo cat /var/lib/jenkins/secrets/initialAdminPassword)"
