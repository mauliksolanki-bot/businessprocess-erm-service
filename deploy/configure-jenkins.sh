#!/usr/bin/env bash
#
# configure-jenkins.sh
#
# Fully scripted Jenkins setup (no manual UI clicking, no jenkins-cli):
#   - waits for Jenkins to come up
#   - installs required plugins via the REST API
#   - creates a Pipeline job "ermservice-deploy" pointing at the repo/Jenkinsfile via the REST API
#
# Run as root (or via sudo) on the Jenkins host, after provision-server.sh.
#
#   # If the Jenkins setup wizard hasn't been completed yet (initialAdminPassword still exists):
#   sudo bash configure-jenkins.sh
#
#   # If you've already completed the setup wizard and created your own admin user:
#   sudo JENKINS_USER=<user> JENKINS_PASSWORD='<password>' bash configure-jenkins.sh
#
set -euo pipefail

REPO_URL="${REPO_URL:-https://github.com/mauliksolanki-bot/businessprocess-erm-service.git}"
REPO_BRANCH="${REPO_BRANCH:-production}"
JENKINS_PORT="${JENKINS_PORT:-8080}"
JENKINS_URL="http://localhost:${JENKINS_PORT}"
JOB_NAME="${JOB_NAME:-ermservice-deploy}"
INITIAL_PW_FILE="/var/lib/jenkins/secrets/initialAdminPassword"

# If you've already completed the Jenkins setup wizard (initialAdminPassword
# has been deleted), pass your real admin credentials instead:
#   sudo JENKINS_USER=admin JENKINS_PASSWORD='yourpassword' bash configure-jenkins.sh
JENKINS_USER="${JENKINS_USER:-admin}"
JENKINS_PASSWORD="${JENKINS_PASSWORD:-}"

log() { echo -e "\n\033[1;32m==> $*\033[0m"; }

log "Waiting for Jenkins to respond on ${JENKINS_URL}"
for i in $(seq 1 60); do
  if curl -sf "${JENKINS_URL}/login" -o /dev/null; then
    break
  fi
  sleep 5
done

if [[ -n "${JENKINS_PASSWORD}" ]]; then
  ADMIN_PW="${JENKINS_PASSWORD}"
  log "Using provided credentials for user '${JENKINS_USER}'"
elif [[ -f "${INITIAL_PW_FILE}" ]]; then
  ADMIN_PW="$(cat "${INITIAL_PW_FILE}")"
  JENKINS_USER="admin"
else
  echo "ERROR: ${INITIAL_PW_FILE} not found (setup wizard already completed?) and no JENKINS_PASSWORD provided." >&2
  echo "Re-run as: sudo JENKINS_USER=<your-admin-user> JENKINS_PASSWORD='<your-password>' bash configure-jenkins.sh" >&2
  exit 1
fi

AUTH="${JENKINS_USER}:${ADMIN_PW}"
COOKIE_JAR="$(mktemp)"

# Fetch a CSRF crumb + matching session cookie for authenticated POSTs.
CRUMB_JSON="$(curl -sf -u "${AUTH}" -c "${COOKIE_JAR}" "${JENKINS_URL}/crumbIssuer/api/json")"
CRUMB_FIELD="$(echo "${CRUMB_JSON}" | grep -o '"crumbRequestField":"[^"]*"' | cut -d'"' -f4)"
CRUMB_VALUE="$(echo "${CRUMB_JSON}" | grep -o '"crumb":"[^"]*"' | cut -d'"' -f4)"

if [[ -z "${CRUMB_FIELD}" || -z "${CRUMB_VALUE}" ]]; then
  echo "ERROR: could not obtain CSRF crumb from Jenkins - check credentials." >&2
  exit 1
fi
log "Obtained CSRF crumb for authenticated requests"

jenkins_post() {
  local path="$1"; shift
  curl -sf -u "${AUTH}" -b "${COOKIE_JAR}" -H "${CRUMB_FIELD}: ${CRUMB_VALUE}" "${JENKINS_URL}${path}" "$@"
}

log "Installing required Jenkins plugins (git, workflow-aggregator/pipeline) via REST API"
PLUGIN_XML='<jenkins><install plugin="git@latest" /><install plugin="workflow-aggregator@latest" /></jenkins>'
jenkins_post "/pluginManager/installNecessaryPlugins" \
  -X POST -H "Content-Type: text/xml" -d "${PLUGIN_XML}" -o /dev/null || \
  echo "WARNING: plugin install request failed/already satisfied - continuing"

log "Waiting for plugin installation to finish"
for i in $(seq 1 60); do
  IS_JOB_RUNNING="$(curl -sf -u "${AUTH}" -b "${COOKIE_JAR}" "${JENKINS_URL}/updateCenter/api/json?tree=jobs[type,status[success,type]]" \
    | grep -o '"success":false' || true)"
  if [[ -z "${IS_JOB_RUNNING}" ]]; then
    break
  fi
  sleep 5
done

log "Restarting Jenkins to activate plugins"
jenkins_post "/safeRestart" -X POST -o /dev/null || true

log "Waiting for Jenkins to come back up after restart"
sleep 10
for i in $(seq 1 60); do
  if curl -sf "${JENKINS_URL}/login" -o /dev/null; then
    break
  fi
  sleep 5
done

# Refresh crumb/cookie after restart (session/crumb may be invalidated)
COOKIE_JAR="$(mktemp)"
CRUMB_JSON="$(curl -sf -u "${AUTH}" -c "${COOKIE_JAR}" "${JENKINS_URL}/crumbIssuer/api/json")"
CRUMB_FIELD="$(echo "${CRUMB_JSON}" | grep -o '"crumbRequestField":"[^"]*"' | cut -d'"' -f4)"
CRUMB_VALUE="$(echo "${CRUMB_JSON}" | grep -o '"crumb":"[^"]*"' | cut -d'"' -f4)"

log "Creating Pipeline job '${JOB_NAME}' (SCM: ${REPO_URL} @ ${REPO_BRANCH}, script: Jenkinsfile)"
CONFIG_XML="/tmp/${JOB_NAME}-config.xml"
cat > "${CONFIG_XML}" <<EOF
<?xml version='1.1' encoding='UTF-8'?>
<flow-definition plugin="workflow-job">
  <description>Builds and deploys ermservice from ${REPO_URL} (${REPO_BRANCH})</description>
  <keepDependencies>false</keepDependencies>
  <properties/>
  <definition class="org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition" plugin="workflow-cps">
    <scm class="hudson.plugins.git.GitSCM" plugin="git">
      <configVersion>2</configVersion>
      <userRemoteConfigs>
        <hudson.plugins.git.UserRemoteConfig>
          <url>${REPO_URL}</url>
        </hudson.plugins.git.UserRemoteConfig>
      </userRemoteConfigs>
      <branches>
        <hudson.plugins.git.BranchSpec>
          <name>*/${REPO_BRANCH}</name>
        </hudson.plugins.git.BranchSpec>
      </branches>
      <doGenerateSubmoduleConfigurations>false</doGenerateSubmoduleConfigurations>
      <submoduleCfg class="list"/>
      <extensions/>
    </scm>
    <scriptPath>Jenkinsfile</scriptPath>
    <lightweight>true</lightweight>
  </definition>
  <triggers/>
  <disabled>false</disabled>
</flow-definition>
EOF

JOB_EXISTS_CODE="$(curl -s -o /dev/null -w '%{http_code}' -u "${AUTH}" -b "${COOKIE_JAR}" "${JENKINS_URL}/job/${JOB_NAME}/api/json")"
if [[ "${JOB_EXISTS_CODE}" == "200" ]]; then
  log "Job already exists, updating its config"
  curl -sf -u "${AUTH}" -b "${COOKIE_JAR}" -H "${CRUMB_FIELD}: ${CRUMB_VALUE}" \
    -H "Content-Type: application/xml" -X POST --data-binary "@${CONFIG_XML}" \
    "${JENKINS_URL}/job/${JOB_NAME}/config.xml"
else
  curl -sf -u "${AUTH}" -b "${COOKIE_JAR}" -H "${CRUMB_FIELD}: ${CRUMB_VALUE}" \
    -H "Content-Type: application/xml" -X POST --data-binary "@${CONFIG_XML}" \
    "${JENKINS_URL}/createItem?name=${JOB_NAME}"
fi

log "Triggering an initial build of '${JOB_NAME}'"
curl -sf -u "${AUTH}" -b "${COOKIE_JAR}" -H "${CRUMB_FIELD}: ${CRUMB_VALUE}" \
  -X POST "${JENKINS_URL}/job/${JOB_NAME}/build" -o /dev/null || \
  echo "WARNING: could not trigger initial build automatically - trigger it manually from the Jenkins UI."

rm -f "${COOKIE_JAR}"

echo
echo "=========================================================================="
echo "Jenkins is ready:"
echo "  URL:          ${JENKINS_URL}"
echo "  Logged in as: ${JENKINS_USER}"
echo "  Job created:  ${JOB_NAME} (branch ${REPO_BRANCH}, uses repo Jenkinsfile)"
echo "  Check build status/logs at: http://<public-ip>:${JENKINS_PORT}/job/${JOB_NAME}/"
echo "=========================================================================="
