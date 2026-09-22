<#
.SYNOPSIS
    Provisions a remote Ubuntu EC2 instance for ermservice + Jenkins by
    copying the deploy scripts over SSH and executing them remotely.

.PARAMETER PublicIp
    Public IP address (or DNS name) of the EC2 instance.

.PARAMETER KeyPath
    Path to the SSH private key (.pem) used to connect (e.g. ubuntu default user).

.PARAMETER SshUser
    Remote SSH user. Defaults to 'ubuntu' (standard for Ubuntu AMIs).

.EXAMPLE
    .\run-remote.ps1 -PublicIp 3.10.20.30 -KeyPath C:\keys\mykey.pem
#>
param(
    [Parameter(Mandatory = $true)]
    [string]$PublicIp,

    [Parameter(Mandatory = $true)]
    [string]$KeyPath,

    [string]$SshUser = "ubuntu"
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RemoteDir = "/tmp/ermservice-deploy"

function Invoke-Remote {
    param([string]$Command)
    & ssh -o StrictHostKeyChecking=accept-new -i $KeyPath "$SshUser@$PublicIp" $Command
    if ($LASTEXITCODE -ne 0) {
        throw "Remote command failed (exit $LASTEXITCODE): $Command"
    }
}

Write-Host "==> Creating remote directory $RemoteDir" -ForegroundColor Green
Invoke-Remote "mkdir -p $RemoteDir"

Write-Host "==> Copying deploy scripts to $PublicIp" -ForegroundColor Green
& scp -o StrictHostKeyChecking=accept-new -i $KeyPath `
    "$ScriptDir\provision-server.sh" `
    "$ScriptDir\configure-jenkins.sh" `
    "${SshUser}@${PublicIp}:$RemoteDir/"
if ($LASTEXITCODE -ne 0) { throw "scp failed" }

Write-Host "==> Running provision-server.sh (installs Java/Maven/Jenkins, builds & starts the app)" -ForegroundColor Green
Invoke-Remote "chmod +x $RemoteDir/*.sh; sudo bash $RemoteDir/provision-server.sh"

Write-Host "==> Running configure-jenkins.sh (unlocks Jenkins, installs plugins, creates pipeline job)" -ForegroundColor Green
Invoke-Remote "sudo bash $RemoteDir/configure-jenkins.sh"

Write-Host "==> Final verification snapshot" -ForegroundColor Green
Invoke-Remote "sudo systemctl is-active ermservice; echo '---'; ps -ef | grep '[j]ava -jar /opt/ermservice/app.jar'; echo '---'; sudo systemctl is-active jenkins"

Write-Host "`nDone. App: http://$PublicIp:8081  Jenkins: http://$PublicIp:8080" -ForegroundColor Cyan
