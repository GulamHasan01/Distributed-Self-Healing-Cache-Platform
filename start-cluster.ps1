# PowerShell script to build, launch, and test the Distributed Cache Platform cluster

param (
    [switch]$Docker,
    [switch]$BuildOnly,
    [switch]$Test,
    [switch]$DryRun
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Distributed Self-Healing Cache Platform - Cluster Manager " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

if ($BuildOnly) {
    Write-Host "`n[1/4] Building cache-service..." -ForegroundColor Yellow
    Push-Location ./cache-service; .\mvnw.cmd clean package -DskipTests; Pop-Location

    Write-Host "`n[2/4] Building gateway-service..." -ForegroundColor Yellow
    Push-Location ./gateway-service; .\mvnw.cmd clean package -DskipTests; Pop-Location

    Write-Host "`n[3/4] Building notification-service..." -ForegroundColor Yellow
    Push-Location ./notification-service; .\mvnw.cmd clean package -DskipTests; Pop-Location

    Write-Host "`n[4/4] Building iam-service..." -ForegroundColor Yellow
    Push-Location "./iam-service - updated"; .\mvnw.cmd clean package -DskipTests; Pop-Location

    Write-Host "`nAll microservices built successfully!" -ForegroundColor Green
    exit 0
}

if ($Test) {
    Write-Host "`nExecuting E2E Cluster Integration Test Suite..." -ForegroundColor Yellow
    if ($DryRun) {
        python e2e_cluster_test.py --dry-run
    } else {
        python e2e_cluster_test.py
    }
    exit 0
}

if ($Docker) {
    Write-Host "`nLaunching full 8-container cluster with Docker Compose..." -ForegroundColor Green
    docker-compose up --build -d
    Write-Host "`nCluster started!" -ForegroundColor Green
    Write-Host "  - Gateway API:   http://localhost:8080"
    Write-Host "  - Cache Node 1:  http://localhost:8081"
    Write-Host "  - Cache Node 2:  http://localhost:8082"
    Write-Host "  - Cache Node 3:  http://localhost:8083"
    Write-Host "  - Notification:  http://localhost:8084"
    Write-Host "  - IAM Service:   http://localhost:8085"
    Write-Host "  - Web Dashboard: http://localhost:3000"
} else {
    Write-Host "`nUsage:" -ForegroundColor Yellow
    Write-Host "  .\start-cluster.ps1 -BuildOnly       # Compiles all microservices"
    Write-Host "  .\start-cluster.ps1 -Docker          # Launches entire cluster via docker-compose"
    Write-Host "  .\start-cluster.ps1 -Test            # Runs live E2E integration test suite"
    Write-Host "  .\start-cluster.ps1 -Test -DryRun    # Runs synthetic E2E integration test suite"
}
