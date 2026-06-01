# CampusFlow Windows Server Setup
$ErrorActionPreference = "Stop"
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

Write-Host "=== CampusFlow Setup Starting ===" -ForegroundColor Cyan

# 1. Create directories
foreach ($d in @("C:\campusflow","C:\campusflow\frontend","C:\campusflow\logs","C:\nginx")) {
    if (-not (Test-Path $d)) { New-Item -ItemType Directory -Path $d -Force | Out-Null }
}
Write-Host "[1/5] Directories created" -ForegroundColor Green

# 2. Install Java 17 JRE
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host "[2/5] Downloading Java 17 JRE..." -ForegroundColor Yellow
    $jreUrl = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.14%2B7/OpenJDK17U-jre_x64_windows_hotspot_17.0.14_7.msi"
    Invoke-WebRequest -Uri $jreUrl -OutFile "C:\campusflow\jre17.msi" -UseBasicParsing
    Write-Host "[2/5] Installing Java 17 JRE..." -ForegroundColor Yellow
    Start-Process msiexec.exe -Wait -ArgumentList "/i C:\campusflow\jre17.msi /quiet /norestart ADDLOCAL=FeatureMain,FeatureEnvironment,FeatureJarFileRunWith,FeatureJavaHome"
    Remove-Item "C:\campusflow\jre17.msi" -Force
    Write-Host "[2/5] Java 17 installed" -ForegroundColor Green
} else {
    Write-Host "[2/5] Java already installed" -ForegroundColor Green
}

# 3. Install nginx
if (-not (Test-Path "C:\nginx\nginx.exe")) {
    Write-Host "[3/5] Downloading nginx..." -ForegroundColor Yellow
    Invoke-WebRequest -Uri "https://nginx.org/download/nginx-1.26.3.zip" -OutFile "C:\campusflow\nginx.zip" -UseBasicParsing
    Expand-Archive -Path "C:\campusflow\nginx.zip" -DestinationPath "C:\campusflow\nginx_tmp" -Force
    $src = (Get-ChildItem "C:\campusflow\nginx_tmp" | Select-Object -First 1).FullName
    Copy-Item "$src\*" "C:\nginx\" -Recurse -Force
    Remove-Item "C:\campusflow\nginx.zip","C:\campusflow\nginx_tmp" -Recurse -Force
    Write-Host "[3/5] nginx installed" -ForegroundColor Green
} else {
    Write-Host "[3/5] nginx already installed" -ForegroundColor Green
}

# 4. Write nginx config
@'
worker_processes  1;
events { worker_connections 1024; }
http {
    include       mime.types;
    default_type  application/octet-stream;
    sendfile      on;
    keepalive_timeout 65;
    gzip on;
    gzip_types text/plain application/json application/javascript text/css;
    server {
        listen 3000;
        root   C:/campusflow/frontend;
        index  index.html;
        charset utf-8;
        location /api/ {
            proxy_pass         http://127.0.0.1:8080;
            proxy_set_header   Host $host;
            proxy_set_header   X-Real-IP $remote_addr;
            proxy_read_timeout 120s;
        }
        location / { try_files $uri $uri/ /index.html; }
        location /assets/ {
            expires 1y;
            add_header Cache-Control "public, immutable";
        }
    }
}
'@ | Out-File -FilePath "C:\nginx\conf\nginx.conf" -Encoding utf8 -Force
Write-Host "[4/5] nginx config written" -ForegroundColor Green

# 5. Firewall rules
foreach ($r in @(@{N="CampusFlow-Front";P=3000},@{N="CampusFlow-Back";P=8080})) {
    if (-not (Get-NetFirewallRule -DisplayName $r.N -ErrorAction SilentlyContinue)) {
        New-NetFirewallRule -DisplayName $r.N -Direction Inbound -Protocol TCP -LocalPort $r.P -Action Allow | Out-Null
    }
}
Write-Host "[5/5] Firewall rules added" -ForegroundColor Green

# Download NSSM (service manager)
if (-not (Test-Path "C:\campusflow\nssm.exe")) {
    Write-Host "[+] Downloading NSSM..." -ForegroundColor Yellow
    Invoke-WebRequest -Uri "https://nssm.cc/release/nssm-2.24.zip" -OutFile "C:\campusflow\nssm.zip" -UseBasicParsing
    Expand-Archive -Path "C:\campusflow\nssm.zip" -DestinationPath "C:\campusflow\nssm_tmp" -Force
    Copy-Item "C:\campusflow\nssm_tmp\nssm-2.24\win64\nssm.exe" "C:\campusflow\nssm.exe" -Force
    Remove-Item "C:\campusflow\nssm.zip","C:\campusflow\nssm_tmp" -Recurse -Force
    Write-Host "[+] NSSM ready" -ForegroundColor Green
}

Write-Host ""
Write-Host "=== Setup Complete ===" -ForegroundColor Cyan
Write-Host "Java:  installed" -ForegroundColor White
Write-Host "nginx: C:\nginx\nginx.exe" -ForegroundColor White
Write-Host "App:   C:\campusflow\" -ForegroundColor White
