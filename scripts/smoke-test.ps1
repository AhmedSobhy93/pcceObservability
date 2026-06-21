param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Username = "admin",
    [string]$Password = "change-me",
    [string]$From = (Get-Date).ToString("yyyy-MM-dd"),
    [string]$To = (Get-Date).ToString("yyyy-MM-dd")
)

$pair = "$Username`:$Password"
$token = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($pair))
$headers = @{ Authorization = "Basic $token" }

function Invoke-Check {
    param([string]$Name, [string]$Path)
    Write-Host "Checking $Name ..." -ForegroundColor Cyan
    Invoke-RestMethod -Headers $headers -Uri "$BaseUrl$Path" -Method Get | ConvertTo-Json -Depth 10
}

Invoke-Check "health" "/actuator/health"
Invoke-Check "current user" "/api/v1/auth/me"
Invoke-Check "components" "/api/v1/components/status"
Invoke-Check "operations last assessment" "/api/v1/operations/assessment/last"
Invoke-Check "operations assessment" "/api/v1/operations/assessment?from=$From&to=$To"
Invoke-Check "summary" "/api/v1/summary?from=$From&to=$To"

Write-Host "Smoke test completed" -ForegroundColor Green
