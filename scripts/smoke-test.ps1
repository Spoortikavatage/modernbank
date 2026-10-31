$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8082"

Write-Host "Starting ModernBank smoke test..."

# Test 1: Application health
$health = Invoke-RestMethod `
    -Uri "$baseUrl/actuator/health" `
    -Method GET

if ($health.status -ne "UP") {
    throw "Health check failed"
}

Write-Host "PASS: Application health"

# Test 2: Existing customer login
if ([string]::IsNullOrWhiteSpace($env:SMOKE_TEST_EMAIL) -or
    [string]::IsNullOrWhiteSpace($env:SMOKE_TEST_PASSWORD)) {
    throw "Set SMOKE_TEST_EMAIL and SMOKE_TEST_PASSWORD before running."
}

$body = @{
    email = $env:SMOKE_TEST_EMAIL
    password = $env:SMOKE_TEST_PASSWORD
} | ConvertTo-Json

$response = Invoke-RestMethod `
    -Uri "$baseUrl/api/v1/auth/login" `
    -Method POST `
    -ContentType "application/json" `
    -Body $body

if ([string]::IsNullOrWhiteSpace($response.accessToken)) {
    throw "Login failed: JWT token missing"
}

Write-Host "PASS: Customer login"

# Test 3: Protected endpoint
$customer = Invoke-RestMethod `
    -Uri "$baseUrl/api/v1/users/me" `
    -Method GET `
    -Headers @{
        Authorization = "Bearer $($response.accessToken)"
    }

if ($customer.email -ne $env:SMOKE_TEST_EMAIL){
    throw "Protected endpoint returned incorrect customer"
}

Write-Host "PASS: Protected endpoint"
Write-Host "All smoke tests passed!"