# CampusFlow Windows Service Registration
$ErrorActionPreference = "Continue"
$java   = "C:\Program Files\Eclipse Adoptium\jre-17.0.14.7-hotspot\bin\java.exe"
$nssm   = "C:\campusflow\nssm.exe"
$jar    = "C:\campusflow\campusflow.jar"
$nginx  = "C:\nginx\nginx.exe"
$appDir = "C:\campusflow"
$logDir = "C:\campusflow\logs"

Write-Host "=== Registering CampusFlow Services ===" -ForegroundColor Cyan

# Remove existing services if present
foreach ($svc in @("CampusFlowBackend","CampusFlowNginx")) {
    $existing = Get-Service -Name $svc -ErrorAction SilentlyContinue
    if ($existing) {
        Write-Host "Removing existing service: $svc" -ForegroundColor Yellow
        & $nssm stop $svc
        Start-Sleep -Seconds 2
        & $nssm remove $svc confirm
    }
}

# Register backend service
Write-Host "[1/2] Registering backend service..." -ForegroundColor Yellow
& $nssm install CampusFlowBackend $java "-jar `"$jar`""
& $nssm set CampusFlowBackend AppDirectory $appDir
& $nssm set CampusFlowBackend AppStdout "$logDir\backend.log"
& $nssm set CampusFlowBackend AppStderr "$logDir\backend-error.log"
& $nssm set CampusFlowBackend AppRotateFiles 1
& $nssm set CampusFlowBackend AppRotateBytes 10485760
& $nssm set CampusFlowBackend Start SERVICE_AUTO_START
& $nssm set CampusFlowBackend DisplayName "CampusFlow Backend"
& $nssm set CampusFlowBackend Description "CampusFlow Spring Boot API Server"
Write-Host "[1/2] Backend service registered" -ForegroundColor Green

# Register nginx service
Write-Host "[2/2] Registering nginx service..." -ForegroundColor Yellow
& $nssm install CampusFlowNginx $nginx
& $nssm set CampusFlowNginx AppDirectory "C:\nginx"
& $nssm set CampusFlowNginx AppStdout "$logDir\nginx.log"
& $nssm set CampusFlowNginx AppStderr "$logDir\nginx-error.log"
& $nssm set CampusFlowNginx Start SERVICE_AUTO_START
& $nssm set CampusFlowNginx DisplayName "CampusFlow Nginx"
& $nssm set CampusFlowNginx Description "CampusFlow Frontend + Proxy"
Write-Host "[2/2] Nginx service registered" -ForegroundColor Green

# Start services
Write-Host "Starting services..." -ForegroundColor Yellow
& $nssm start CampusFlowBackend
Start-Sleep -Seconds 5
& $nssm start CampusFlowNginx

Start-Sleep -Seconds 3
Write-Host ""
Write-Host "=== Service Status ===" -ForegroundColor Cyan
Get-Service -Name "CampusFlowBackend","CampusFlowNginx" | Format-Table Name, Status, StartType -AutoSize
Write-Host ""
Write-Host "Frontend: http://10.8.0.29:3000" -ForegroundColor Green
Write-Host "Backend:  http://10.8.0.29:8080" -ForegroundColor Green
