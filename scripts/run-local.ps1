param(
    [string]$AdminPassword = "change-me",
    [int]$Port = 8080
)

$env:SPRING_PROFILES_ACTIVE = "local"
$env:SERVER_PORT = "$Port"
$env:PCCE_ADMIN_PASSWORD = "{noop}$AdminPassword"
$env:PCCE_ASSESSMENT_ENABLED = "false"

Write-Host "Starting PCCE Observability API on http://localhost:$Port" -ForegroundColor Cyan
Write-Host "Admin user: admin" -ForegroundColor Cyan
Write-Host "Swagger UI: http://localhost:$Port/swagger-ui/index.html" -ForegroundColor Cyan

mvn spring-boot:run
